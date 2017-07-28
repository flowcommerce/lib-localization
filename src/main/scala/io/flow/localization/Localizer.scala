package io.flow.localization

import javax.inject.Inject

import io.flow.catalog.v0.models.LocalizedItemPrice
import io.flow.common.v0.models.PriceWithBase
import io.flow.item.v0.models.LocalItem
import io.flow.item.v0.models.json._
import io.flow.reference.Countries
import play.api.libs.json.Json
import redis.RedisClientPool

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Localizer {

  /**
    * Returns the localized pricing of the specified item for the specified country, using
    * the default currency for that country.
    *
    * @param country country in the ISO 3166-3 format
    * @param itemNumbers the item numbers to localize
    * @return the localized pricing of the specified item for the specified country
    */
  def getSkuPricesByCountry(country: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]]

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
    * @return the localized pricing of the specified item for the specified country
    */
  def getSkuPriceByCountryWithCurrency(country: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getSkuPriceByCountry(country, itemNumber).map(_.map(convert(_, targetCurrency)))
  }

  /**
    * Converts a given price to the specified target currency
    *
    * @param targetCurrency the ISO currency code
    */
  def convert(pricing: FlowSkuPrice, targetCurrency: String): FlowSkuPrice

  /**
    * Returns localized pricing of the specified item for the specified experience
    * @param experienceKey the id of the experience
    * @param itemNumbers the item numbers to localize
    * @return the localized pricing of the specified item for the specified experience
    */
  def getSkuPricesByExperience(experienceKey: String, itemNumbers: Iterable[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[FlowSkuPrice]]]

  def getSkuPriceByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]]

  /**
    * Returns localized pricing of the specified item for the specified experience
    * @param experienceKey the id of the experience
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified experience
    */
  def getSkuPriceByExperienceWithCurrency(experienceKey: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getSkuPriceByExperience(experienceKey, itemNumber).map(_.map(convert(_, targetCurrency)))
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
    * Creates a new [[Localizer]] backed by [[RedisLocalizerClient]]
    * @param redisClientPool the client pool to use to connect to redis
    * @return a new [[Localizer]] backed by [[RedisLocalizerClient]]
    */
  def apply(redisClientPool: RedisClientPool, ratesRefreshPeriod: FiniteDuration = DefaultRatesRefreshPeriod): Localizer = {
    val localizerClient = new RedisLocalizerClient(redisClientPool)

    val rateProvider = new RatesCacheImpl(localizerClient, ratesRefreshPeriod.toMillis)
    rateProvider.start()

    val availableCountriesProvider = new AvailableCountriesProviderImpl(localizerClient, ratesRefreshPeriod.toMillis)
    availableCountriesProvider.start()

    new LocalizerImpl(
      localizerClient = localizerClient,
      rateProvider = rateProvider,
      availableCountriesProvider = availableCountriesProvider
    )
  }

}

class LocalizerImpl @Inject() (localizerClient: LocalizerClient, rateProvider: RateProvider,
                               availableCountriesProvider: AvailableCountriesProvider) extends Localizer {

  import LocalizerImpl._

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
    localizerClient.mget(keyProviders.map(_.getKey).toSeq).map { optionalPrices =>
      optionalPrices.map(toFlowSkuPrice).toList
    }
  }

  private def getPricing(keyProvider: KeyProvider)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    localizerClient.get(keyProvider.getKey).map(toFlowSkuPrice)
  }

  private def toFlowSkuPrice(optionalPrice: Option[String]) = {
    optionalPrice.map { js =>
      FlowSkuPrice(
        Json.parse(js).as[LocalItem].pricing
      )
    }
  }

  override def convert(pricing: FlowSkuPrice, targetCurrency: String): FlowSkuPrice = {
    val localCurrency = pricing.salePrice.currency
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
    FlowSkuPrice(
      salePrice = convertLocalizedItemPrice(pricing.salePrice, targetCurrency, rate),
      msrpPrice = pricing.msrpPrice.map(convertPrice(_, targetCurrency, rate)),
      basePrice = pricing.basePrice.map(convertPrice(_, targetCurrency, rate)),
      shippingSurcharge = pricing.shippingSurcharge.map(convertPrice(_, targetCurrency, rate))
    )
  }

  private def convertLocalizedItemPrice(price: LocalizedItemPrice, targetCurrency: String, rate: BigDecimal): LocalizedItemPrice = {
    val newAmount = (price.amount * rate).toDouble

    // TODO: we need the target locale here, not the default one!
    val format = java.text.NumberFormat.getCurrencyInstance()
    format.setCurrency(java.util.Currency.getInstance(targetCurrency))

    LocalizedItemPrice(
      currency = targetCurrency,
      amount = newAmount,
      label = format.format(newAmount),
      base = price.base,
      includes = price.includes
    )
  }

  private def convertPrice(price: PriceWithBase, targetCurrency: String, rate: BigDecimal): PriceWithBase = {
    val newAmount = (price.amount * rate).toDouble

    // TODO: we need the target locale here, not the default one!
    val format = java.text.NumberFormat.getCurrencyInstance()
    format.setCurrency(java.util.Currency.getInstance(targetCurrency))

    PriceWithBase(
      currency = targetCurrency,
      amount = newAmount,
      label = format.format(newAmount),
      base = price.base
    )
  }

}

private[this] sealed trait KeyProvider {
  def getKey: String
}

private[this] case class CountryKey(country: String, itemNumber: String) extends KeyProvider {
  def getKey: String = {
    val code = Countries.find(country).map(_.iso31662).getOrElse(country).toLowerCase
    s"country-$code:$itemNumber"
  }
}

private[this] case class ExperienceKey(experience: String, itemNumber: String) extends KeyProvider {
  def getKey: String = s"experience-${experience.toLowerCase}:$itemNumber"
}
