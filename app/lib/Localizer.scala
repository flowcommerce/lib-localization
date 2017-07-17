package io.flow.lib


import javax.inject.Inject

import com.redis.RedisClientPool
import io.flow.common.v0.models.Price
import io.flow.common.v0.models.json._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}


trait Localizer {

  def get(country: String, sku: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]]

}

object Localizer {

  def apply(redisClientPool: RedisClientPool): Localizer = {
    new LocalizerImpl(
      new RedisLocalizerClient(redisClientPool)
    )
  }

}

private[this] case class Key(
  country: String,
  sku: String
) {
  def key: String = {
    s"$country:$sku"
  }
}

class LocalizerImpl @Inject() (localizerClient: LocalizerClient) extends Localizer {

  override def get(country: String, sku: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[Price]] = {
    val key = Key(country = country, sku = sku).key

    localizerClient.get(key).map { optionalPrice =>
      optionalPrice.map(
        Json.parse(_).as[Price]
      )
    }
  }

}

