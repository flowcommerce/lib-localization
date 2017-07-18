package io.flow.lib

import javax.inject.Inject

import com.redis.RedisClientPool
import io.flow.common.v0.models.Price
import io.flow.common.v0.models.json._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

trait Localizer {

  /**
    * Returns the price of the specified item for the specified country
    * @param country country in the ISO 3166-3 format
    * @param itemId the id of the item
    * @return the price of the specified item for the specified country
    */
  def getByCountry(country: String, itemId: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]]

  /**
    * Returns the price of the specified item for the specified experience
    * @param experienceId the id of the experience
    * @param itemId the id of the item
    * @return the price of the specified item for the specified experience
    */
  def getByExperience(experienceId: String, itemId: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]]

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

  override def getByCountry(country: String, itemId: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]] = {
    get(CountryKey(country = country, itemId = itemId))
  }

  override def getByExperience(experienceId: String, itemId: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]] = {
    get(ExperienceKey(experience = experienceId, itemId = itemId))
  }

  private def get(keyProvider: KeyProvider)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]] = {
    localizerClient.get(keyProvider.getKey).map { optionalPrice =>
      optionalPrice.map(
        Json.parse(_).as[Price]
      )
    }
  }

}

private[this] trait KeyProvider {
  def getKey: String
}

private[this] case class CountryKey(country: String, itemId: String) extends KeyProvider {
  def getKey: String = s"country-$country:$itemId"
}

private[this] case class ExperienceKey(experience: String, itemId: String) extends KeyProvider {
  def getKey: String = s"experience-$experience:$itemId"
}
