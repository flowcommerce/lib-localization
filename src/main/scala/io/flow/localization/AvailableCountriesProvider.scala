package io.flow.localization

import io.flow.published.event.v0.models.{OrganizationCountriesData => CountriesData}
import io.flow.published.event.v0.models.json._
import io.flow.reference.Countries
import io.flow.reference.v0.models.Country
import play.api.libs.json.Json

import scala.concurrent.Future

trait AvailableCountriesProvider {

  def isEnabled(country: String): Boolean

}

private[localization] class AvailableCountriesProviderCacheImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends LocalizerClientCache[Seq[Country], String, Object] with AvailableCountriesProvider {

  import AvailableCountriesProviderCacheImpl._

  override def retrieveData(): Future[Option[Seq[Country]]] = {
    localizerClient.get(OrganizationCountriesKey, gzipped = false).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[CountriesData].available.flatMap(Countries.find)
      }
    }
  }

  override def toKeyValues(optionalAvailableCountries: Option[Seq[Country]]): Iterable[(String, Object)] = {
    optionalAvailableCountries
      .map(_.flatMap(c => buildCountryKeys(c).map(_ -> Present)))
      .getOrElse {
        warn(s"Available countries cannot be found - expected key named '$OrganizationCountriesKey'. " +
          s"Returning no enabled countries.")
        Iterable.empty
      }
  }

  private def buildCountryKeys(country: Country): Seq[String] = {
    Seq(
      country.iso31663.toLowerCase,
      country.iso31662.toLowerCase,
      country.name
    )
  }

  override def isEnabled(country: String): Boolean = super.get(country.toLowerCase).isDefined
}

object AvailableCountriesProviderCacheImpl {

  private val OrganizationCountriesKey = "organization_countries"

  private val Present = new Object()

}
