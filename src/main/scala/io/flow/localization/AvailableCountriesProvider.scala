package io.flow.localization

import io.flow.localized.item.cache.v0.models.LocalizedItemCacheAvailableCountries
import io.flow.localized.item.cache.v0.models.json._
import play.api.libs.json.Json

import scala.concurrent.Future


trait AvailableCountriesProvider {

  def isEnabled(country: String): Boolean

}

private[localization] class AvailableCountriesProviderImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends LocalizerClientCache[LocalizedItemCacheAvailableCountries, String, Object] with AvailableCountriesProvider {

  import AvailableCountriesProviderImpl._

  override def retrieveData(): Future[Option[LocalizedItemCacheAvailableCountries]] = {
    localizerClient.get(AvailableCountriesRedisKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[LocalizedItemCacheAvailableCountries]
      }
    }
  }

  override def toKeyValues(optionalAvailableCountries: Option[LocalizedItemCacheAvailableCountries]): Iterable[(String, Object)] = {
    optionalAvailableCountries
      .map(_.availableCountries.map(_ -> AvailableValue))
      .getOrElse(sys.error(s"Available countries cannot be found - expected key named '$AvailableCountriesRedisKey"))
  }

  override def isEnabled(country: String): Boolean = super.get(country).isDefined
}

object AvailableCountriesProviderImpl {

  private val AvailableCountriesRedisKey = "available_countries"

  private val AvailableValue = new Object()

}
