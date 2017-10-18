package io.flow.localization.rates

import javax.inject.Inject

import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.finagle.redis
import io.flow.localization.countries.{AvailableCountriesProvider, AvailableCountriesProviderCacheImpl}
import io.flow.localization.pricing.FlowSkuPrice
import io.flow.reference.Countries
import io.flow.localization.utils.{DataClient, RedisDataClient}
import org.msgpack.jackson.dataformat.MessagePackFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Localizer {

  /**
    * Returns the localized pricing of the specified items for the specified country, using
    * the default currency for that country.
    *
    * @param country country in the ISO 3166-3 format
    * @param itemNumbers the item numbers to localize
    * @return the localized pricing of the specified items for the specified country
    */
  def getSkuPricesByCountry(country: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]]

  /**
    * Returns the localized pricing of the specified item for the specified country, using
    * the default currency for that country.
    *
    * @param country country in the ISO 3166-3 format
    * @param itemNumber the item number to localize
    * @return the localized pricing of the specified item for the specified country
    */
  def getSkuPriceByCountry(country: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]]

  /**
    * Returns the localized pricing of the specified item for the specified country,
    * then converting as necessary to the specified target currency.
    *
    * @param country country in the ISO 3166-3 format
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified country in the specified target currency
    */
  def getSkuPriceByCountryWithCurrency(country: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getSkuPriceByCountry(country, itemNumber).map(_.map(convert(_, targetCurrency)))
  }

  /**
    * Returns the localized pricing of the specified items for the specified country,
    * then converting as necessary to the specified target currency.
    *
    * @param country country in the ISO 3166-3 format
    * @param itemNumbers the id of the items
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified items for the specified country in the specified target currency
    */
  def getSkuPricesByCountryWithCurrency(country: String, itemNumbers: Iterable[String], targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]] = {
    getSkuPricesByCountry(country, itemNumbers).map(_.map(_.map(convert(_, targetCurrency))))
  }

  /**
    * Converts a given price to the specified target currency
    *
    * @param targetCurrency the ISO currency code
    */
  def convert(pricing: FlowSkuPrice, targetCurrency: String): FlowSkuPrice

  /**
    * Returns the localized pricing of the specified items for the specified experience
    *
    * @param experienceKey the id of the experience
    * @param itemNumbers the item numbers to localize
    * @return the localized pricing of the specified items for the specified experience
    */
  def getSkuPricesByExperience(experienceKey: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]]

  /**
    * Returns the localized pricing of the specified item for the specified experience
    *
    * @param experienceKey the id of the experience
    * @param itemNumber the item number to localize
    * @return the localized pricing of the specified item for the specified experience
    */
  def getSkuPriceByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]]

  /**
    * Returns the localized pricing of the specified item for the specified experience,
    * then converting as necessary to the specified target currency.
    *
    * @param experienceKey the id of the experience
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified experience in the specified currency
    */
  def getSkuPriceByExperienceWithCurrency(experienceKey: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getSkuPriceByExperience(experienceKey, itemNumber).map(_.map(convert(_, targetCurrency)))
  }

  /**
    * Returns the localized pricing of the specified items for the specified experience,
    * then converting as necessary to the specified target currency.
    *
    * @param experienceKey the id of the experience
    * @param itemNumbers the id of the items
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified items for the specified experience in the specified currency
    */
  def getSkuPricesByExperienceWithCurrency(experienceKey: String, itemNumbers: Iterable[String], targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]] = {
    getSkuPricesByExperience(experienceKey, itemNumbers).map(_.map(_.map(convert(_, targetCurrency))))
  }

  /**
    * Returns true if the specified country is enabled, false otherwise
    * @param country country in the ISO 3166-3 format
    * @return true if the specified country is enabled, false otherwise
    */
  def isEnabled(country: String): Boolean

}

object Localizer {

  private val DefaultRatesRefreshPeriod = FiniteDuration(1, MINUTES)

  /**
    * Creates a new [[Localizer]] backed by [[RedisDataClient]]
    *
    * @param redisClient the redis client to use to connect to redis
    * @return a new [[Localizer]] backed by [[RedisDataClient]]
    */
  def apply(redisClient: redis.Client, ratesRefreshPeriod: FiniteDuration = DefaultRatesRefreshPeriod): Localizer = {
    val dataClient = new RedisDataClient(redisClient)

    val rateProvider = new RatesCacheImpl(dataClient, ratesRefreshPeriod.toMillis)
    rateProvider.start()

    val availableCountriesProvider = new AvailableCountriesProviderCacheImpl(dataClient, ratesRefreshPeriod.toMillis)
    availableCountriesProvider.start()

    new LocalizerImpl(
      dataClient = dataClient,
      rateProvider = rateProvider,
      availableCountriesProvider = availableCountriesProvider
    )
  }

}

class LocalizerImpl @Inject() (dataClient: DataClient, rateProvider: RateProvider,
                               availableCountriesProvider: AvailableCountriesProvider) extends Localizer {

  import LocalizerImpl._

  private val mapper = new ObjectMapper(new MessagePackFactory())

  override def getSkuPriceByCountry(country: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    val key = CountryKey(country, itemNumber)
    getPricing(key)
  }

  override def getSkuPriceByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    val key = ExperienceKey(experienceKey, itemNumber)
    getPricing(key)
  }


  override def getSkuPricesByCountry(country: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]] = {
    val keys = itemNumbers.map(CountryKey(country, _))
    getPricings(keys)
  }

  override def getSkuPricesByExperience(experienceKey: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]] = {
    val keys = itemNumbers.map(ExperienceKey(experienceKey, _))
    getPricings(keys)
  }

  private def getPricings(keyProviders: Iterable[KeyProvider])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]] = {
    dataClient.mGet[Array[Byte]](keyProviders.map(_.getKey).toSeq).map { optionalPrices =>
      optionalPrices.map(toFlowSkuPrice).toList
    }
  }

  private def getPricing(keyProvider: KeyProvider)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    dataClient.get[Array[Byte]](keyProvider.getKey).map(toFlowSkuPrice)
  }

  private def toFlowSkuPrice(optionalPrice: Option[Array[Byte]]): Option[FlowSkuPrice] = {
    optionalPrice.flatMap { bytes =>
      FlowSkuPrice(mapper.readValue(bytes, classOf[java.util.Map[String, Any]]))
    }
  }

  override def convert(pricing: FlowSkuPrice, targetCurrency: String): FlowSkuPrice = {
    val localCurrency = pricing.currency
    if (localCurrency == targetCurrency) {
      pricing
    } else {
      rateProvider
        .get(localCurrency, targetCurrency)
        .map(rate => convertWithRate(pricing, targetCurrency, rate))
        // TODO: should we fall back to the original pricing instead of an error?
        .getOrElse(sys.error(s"Cannot find conversion rate for $localCurrency -> $targetCurrency"))
    }
  }

  override def isEnabled(country: String): Boolean = availableCountriesProvider.isEnabled(country)

}

object LocalizerImpl {

  private def convertWithRate(pricing: FlowSkuPrice, targetCurrency: String, rate: BigDecimal): FlowSkuPrice = {
    pricing.copy(
      currency = targetCurrency,
      salePrice = convertPrice(pricing.salePrice, rate),
      msrpPrice = pricing.msrpPrice.map(convertPrice(_, rate)),
      basePrice = pricing.basePrice.map(convertPrice(_, rate)),
      shippingSurcharge = pricing.shippingSurcharge.map(convertPrice(_, rate))
    )
  }

  private def convertPrice(amount: BigDecimal, rate: BigDecimal): BigDecimal = amount * rate

}

private[this] sealed trait KeyProvider {
  def getKey: String
}

private[this] case class CountryKey(country: String, itemNumber: String) extends KeyProvider {
  def getKey: String = {
    val code = Countries.find(country).map(_.iso31663).getOrElse(country)
    s"c-$code:$itemNumber"
  }
}

private[this] case class ExperienceKey(experience: String, itemNumber: String) extends KeyProvider {
  def getKey: String = s"experience-${experience.toLowerCase}:$itemNumber"
}
