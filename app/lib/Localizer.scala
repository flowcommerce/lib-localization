package io.flow.lib

import javax.inject.Inject

import com.redis.RedisClientPool
import io.flow.localized.items.cache.v0.models.LocalizedPricing
import io.flow.localized.items.cache.v0.models.json._
import play.api.libs.json.Json

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
  ): Future[Option[LocalizedPricing]]

  /**
    * Returns the localized pricing of the specified item for the specified country,
    * then converting as necessary to the specified target currency.
    * 
    * @param country country in the ISO 3166-3 format
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified country
    */
  def getByCountry(country: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]] = {
    getByCountry(country, itemNumber).map( _.map(convert(_, targetCurrency)) )
  }

  /**
    * Converts a given price to the specified target currency
    *
    * @param targetCurrency the ISO currency code
    */
  def convert(pricing: LocalizedPricing, targetCurrency: String): LocalizedPricing

  /**
    * Returns localized pricing of the specified item for the specified experience
    * @param experienceKey the id of the experience
    * @param itemNumber the id of the item
    * @return the localized pricing of the specified item for the specified experience
    */
  def getByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]]


  /**
    * Returns localized pricing of the specified item for the specified experience
    * @param experienceKey the id of the experience
    * @param itemNumber the id of the item
    * @param targetCurrency the ISO currency code
    * @return the localized pricing of the specified item for the specified experience
    */
  def getByExperience(experienceKey: String, itemNumber: String, targetCurrency: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]] = {
    getByExperience(experienceKey, itemNumber).map(_.map(convert(_, targetCurrency)) )
  }
}

object Localizer {

  /**
    * Creates a new [[Localizer]] backed by [[RedisLocalizerClient]]
    * @param redisClientPool the client pool to use to connect to redis
    * @return a new [[Localizer]] backed by [[RedisLocalizerClient]]
    */
  def apply(redisClientPool: RedisClientPool): Localizer = {
    new LocalizerImpl(
      new RedisLocalizerClient(redisClientPool)
    )
  }

}

class LocalizerImpl @Inject() (localizerClient: LocalizerClient) extends Localizer {

  override def getByCountry(country: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]] = {
    get(CountryKey(country = country, itemNumber = itemNumber))
  }

  override def getByExperience(experienceKey: String, itemNumber: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]] = {
    get(ExperienceKey(experience = experienceKey, itemNumber = itemNumber))
  }

  private def get(keyProvider: KeyProvider)(
    implicit executionContext: ExecutionContext
  ): Future[Option[LocalizedPricing]] = {
    localizerClient.get(keyProvider.getKey).map { optionalPrice =>
      optionalPrice.map(
        Json.parse(_).as[LocalizedPricing]
      )
    }
  }

  override def convert(pricing: LocalizedPricing, targetCurrency: String): LocalizedPricing = ???

}

private[this] trait KeyProvider {
  def getKey: String
}

private[this] case class CountryKey(country: String, itemNumber: String) extends KeyProvider {
  def getKey: String = s"country-$country:$itemNumber"
}

private[this] case class ExperienceKey(experience: String, itemNumber: String) extends KeyProvider {
  def getKey: String = s"experience-$experience:$itemNumber"
}
