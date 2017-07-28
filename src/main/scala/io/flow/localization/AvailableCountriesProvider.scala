package io.flow.localization

import io.flow.bulk.event.v0.models.OrganizationCountries
import io.flow.reference.Countries
import io.flow.reference.v0.models.Country
import play.api.libs.json.Json

import scala.concurrent.Future


trait AvailableCountriesProvider {

  def isEnabled(country: String): Boolean

}

private[localization] class AvailableCountriesProviderImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends LocalizerClientCache[Seq[Country], String, Object] with AvailableCountriesProvider {

  import AvailableCountriesProviderImpl._

  override def retrieveData(): Future[Option[Seq[Country]]] = {
    localizerClient.get(OrganizationCountriesRedisKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[OrganizationCountries].available.flatMap(Countries.find)
      }
    }
  }

  override def toKeyValues(optionalAvailableCountries: Option[Seq[Country]]): Iterable[(String, Object)] = {
    // Jean: Not sure how to update this...
    optionalAvailableCountries
      .map(_.availableCountries.map(_ -> AvailableValue))
      .getOrElse(sys.error(s"Available countries cannot be found - expected key named '$OrganizationCountriesRedisKey"))
  }

  override def isEnabled(country: String): Boolean = super.get(country).isDefined
}

object AvailableCountriesProviderImpl {

  private val OrganizationCountriesRedisKey = "organization_countries"

  private val AvailableValue = new Object()

}
