package io.flow.localization.transit

import io.flow.localization.countries.AvailableCountriesProvider
import io.flow.published.event.v0.models.json._
import io.flow.published.event.v0.models.{OrganizationRatecardTransitWindowsData, TransitWindow}
import io.flow.localization.transit.utils.DayRange
import io.flow.localization.transit.TransitDataProviderSpec.{Key, transitWindowsData}
import io.flow.localization.utils.DataClient
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

class TransitDataProviderSpec extends FlatSpec with MockitoSugar with Matchers with Eventually {

  "TransitDataProvider" should "retrieve transit windows" in {
    val dataClient = mock[DataClient]
    when(dataClient.get[String](ArgumentMatchers.eq(Key))(any(), any()))
      .thenReturn(Future.successful(Some(Json.toJson(transitWindowsData).toString)))

    val transitWindowsCacheImpl = new TransitWindowsCacheImpl(dataClient, 1.minute.toMillis)
    transitWindowsCacheImpl.start()

    val transitWindowProvider = new TransitDataProviderImpl(transitWindowsCacheImpl, mock[AvailableCountriesProvider])
    transitWindowProvider.getTransitWindowByOriginAndDestination("USD", "CAN") shouldEqual Some(DayRange(3, 5))
    transitWindowProvider.getTransitWindowByOriginAndDestination("USD", "CHN") shouldEqual Some(DayRange(2, 6))
    transitWindowProvider.getTransitWindowByOriginAndDestination("CAN", "USD") shouldEqual Some(DayRange(1, 7))
    transitWindowProvider.getTransitWindowByOriginAndDestination("CAN", "CHN") shouldEqual Some(DayRange(4, 9))
    transitWindowProvider.getTransitWindowByOriginAndDestination("CAN", "ABC") shouldEqual None
  }

  it should "return when a country is enabled" in {
    val dataClient = mock[DataClient]
    val transitWindowsCacheImpl = new TransitWindowsCacheImpl(dataClient, 1.minute.toMillis)

    val availableCountriesProvider = mock[AvailableCountriesProvider]
    when(availableCountriesProvider.isEnabled("CAN")).thenReturn(true)
    when(availableCountriesProvider.isEnabled("CHN")).thenReturn(false)

    val transitWindowProvider = new TransitDataProviderImpl(transitWindowsCacheImpl, availableCountriesProvider)
    transitWindowProvider.isEnabled("CAN") shouldBe true
    transitWindowProvider.isEnabled("CHN") shouldBe false
  }

}

object TransitDataProviderSpec {

  private val Key: String = "transit_windows"
  private val transitWindowsData: OrganizationRatecardTransitWindowsData = OrganizationRatecardTransitWindowsData(
    Seq(
      TransitWindow("USD", "CAN", 3, 5),
      TransitWindow("USD", "CHN", 2, 6),
      TransitWindow("CAN", "USD", 1, 7),
      TransitWindow("CAN", "CHN", 4, 9)
    )
  )

}