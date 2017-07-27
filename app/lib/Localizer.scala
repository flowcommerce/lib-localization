package io.flow.lib

import javax.inject.Inject

import com.redis.RedisClientPool
import io.flow.localized.items.cache.v0.models.json._
import io.flow.localized.items.cache.v0.models.{LocalizedItemCachePrice, LocalizedItemCachePrices, LocalizedItemCachePricing}
import io.flow.reference.Countries
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Localizer {

  /**
    * Returns the localized pricing of the specified item for the specified country, using
    * the default currency for that country.
    * 
    * @param country country in the ISO 3166-3 format
    * @param itemNumber the id of the item
    * @return the localized pricing of the specified item for the specified country
    */
  def getByCountry(country: String, itemNumber: String)(
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
  def getByCountryWithCurrency(country: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getByCountry(country, itemNumber).map(_.map(convert(_, targetCurrency)))
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
    * @param itemNumber the id of the item
    * @return the localized pricing of the specified item for the specified experience
    */
  def getByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]]


  /**
    * Returns localized pricing of the specified item for the specified experience
    * @param experienceKey the id of the experience
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified experience
    */
  def getByExperienceWithCurrency(experienceKey: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getByExperience(experienceKey, itemNumber).map(_.map(convert(_, targetCurrency)))
  }
}

object Localizer {

  private val DefaultRatesRefreshPeriodMs = 10.minutes.toMillis

  /**
    * Creates a new [[Localizer]] backed by [[RedisLocalizerClient]]
    * @param redisClientPool the client pool to use to connect to redis
    * @return a new [[Localizer]] backed by [[RedisLocalizerClient]]
    */
  def apply(redisClientPool: RedisClientPool, ratesRefreshPeriodMs: Long = DefaultRatesRefreshPeriodMs): Localizer = {
    val localizerClient = new RedisLocalizerClient(redisClientPool)

    val rateProvider = new RatesCacheImpl(localizerClient, ratesRefreshPeriodMs)
    rateProvider.start()

    new LocalizerImpl(
      localizerClient = localizerClient,
      rateProvider = rateProvider
    )
  }

}

class LocalizerImpl @Inject() (localizerClient: LocalizerClient, rateProvider: RateProvider) extends Localizer {

  import LocalizerImpl._

  override def getByCountry(country: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getPricing(CountryKey(country = country, itemNumber = itemNumber))
  }

  override def getByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    getPricing(ExperienceKey(experience = experienceKey, itemNumber = itemNumber))
  }

  private def getPricing(keyProvider: KeyProvider)(
    implicit executionContext: ExecutionContext
  ): Future[Option[FlowSkuPrice]] = {
    localizerClient.get(keyProvider.getKey).map { optionalPrice =>
      optionalPrice.map { js =>
        FlowSkuPrice(
          Json.parse(js).as[LocalizedItemCachePricing]
        )
      }
    }
  }

  override def convert(pricing: FlowSkuPrice, targetCurrency: String): FlowSkuPrice = {
    val localCurrency = pricing.salePrice.local.currency
    if (localCurrency == targetCurrency) {
      pricing
    } else {
      rateProvider
        .get(localCurrency, targetCurrency)
        .map(rate => convertWithRate(pricing, targetCurrency, rate))
        // TODO: should we fall back to the original pricing instead of an error?
        .getOrElse(sys.error(s"Cannot find conversino rate for $localCurrency -> $targetCurrency"))
    }
  }

}

object LocalizerImpl {

  private def convertWithRate(pricing: FlowSkuPrice, targetCurrency: String, rate: BigDecimal): FlowSkuPrice = {
    FlowSkuPrice(
      salePrice = convertPrices(pricing.salePrice, targetCurrency, rate),
      msrpPrice = pricing.msrpPrice.map(convertPrices(_, targetCurrency, rate)),
      basePrice = pricing.basePrice.map(convertPrices(_, targetCurrency, rate)),
      shippingSurcharge = pricing.shippingSurcharge.map(convertPrices(_, targetCurrency, rate)),
      includes = pricing.includes
    )
  }

  private def convertPrices(prices: LocalizedItemCachePrices, targetCurrency: String, rate: BigDecimal): LocalizedItemCachePrices = {
    LocalizedItemCachePrices(
      local = convertPrice(prices.local, targetCurrency, rate),
      base = prices.base
    )
  }

  private def convertPrice(price: LocalizedItemCachePrice, targetCurrency: String, rate: BigDecimal): LocalizedItemCachePrice = {
    val newAmount = price.amount * rate

    // TODO: we need the target locale here, not the default one!
    val format = java.text.NumberFormat.getCurrencyInstance()
    format.setCurrency(java.util.Currency.getInstance(targetCurrency))

    LocalizedItemCachePrice(
      currency = targetCurrency,
      amount = newAmount,
      label = format.format(newAmount.toDouble)
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
