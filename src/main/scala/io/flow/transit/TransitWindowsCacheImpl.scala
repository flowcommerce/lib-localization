package io.flow.transit

import io.flow.published.event.v0.models.json._
import io.flow.published.event.v0.models.{OrganizationRatecardTransitWindowsData => TransitWindows}
import io.flow.transit.utils.{DayRange, TransitWindowKey}
import io.flow.utils.{Cache, DataClient}
import play.api.libs.json.Json
import io.flow.utils.DataClientConversions._

import scala.concurrent.Future

private[transit] class TransitWindowsCacheImpl(dataClient: DataClient, override val refreshPeriodMs: Long)
  extends Cache[TransitWindows, TransitWindowKey, DayRange] {

  private val Key: String = "transit_window"

  override def retrieveData(): Future[Option[TransitWindows]] = {
    dataClient.get[String](Key).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[TransitWindows]
      }
    }
  }

  override def toKeyValues(retrievedData: Option[TransitWindows]): Iterable[(TransitWindowKey, DayRange)] = {
    retrievedData.map { data =>
      data.transitWindows.map { transitWindow =>
        TransitWindowKey(transitWindow.originCountry, transitWindow.destinationCountry) -> DayRange(transitWindow.from, transitWindow.to)
      }
    }.getOrElse {
      warn(s"Transit windows cannot be found - expected key named '$Key'. Returning no transit windows.")
      Iterable.empty
    }
  }

}