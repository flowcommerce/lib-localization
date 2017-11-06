package io.flow.localization.transit

import io.flow.published.event.v0.models.json._
import io.flow.published.event.v0.models.{OrganizationRatecardTransitWindowsData, TransitWindow}
import io.flow.localization.transit.utils.{DayRange, TransitWindowKey}
import io.flow.localization.transit.TransitWindowsCacheSpec._
import io.flow.localization.utils.DataClient
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

class TransitWindowsCacheSpec extends FlatSpec with MockitoSugar with Matchers with Eventually {

  "TransitWindowsCache" should "retrieve transit windows" in {
    val dataClient = mock[DataClient]
    when(dataClient.get[String](ArgumentMatchers.eq(Key))(any(), any()))
      .thenReturn(Future.successful(Some(Json.toJson(initialTransitWindowsData).toString)))

    val transitWindowCache = new TransitWindowsCacheImpl(dataClient, 1.minute.toMillis)
    transitWindowCache.start()
    transitWindowCache.get(TransitWindowKey("USD", "CAN")) shouldBe Some(DayRange(3, 5))
    transitWindowCache.get(TransitWindowKey("USD", "CHN")) shouldBe Some(DayRange(2, 6))
    transitWindowCache.get(TransitWindowKey("CAN", "USD")) shouldBe Some(DayRange(1, 7))
    transitWindowCache.get(TransitWindowKey("CAN", "CHN")) shouldBe Some(DayRange(4, 9))
    transitWindowCache.get(TransitWindowKey("CAN", "ABC")) shouldBe None
  }

  it should "refresh transit windows" in {
    val dataClient = mock[DataClient]
    when(dataClient.get[String](ArgumentMatchers.eq(Key))(any(), any()))
      .thenReturn(Future.successful(Some(Json.toJson(initialTransitWindowsData).toString)))
      .thenReturn(Future.successful(Some(Json.toJson(updatedTransitWindowData).toString)))

    val transitWindowCache = new TransitWindowsCacheImpl(dataClient, 100.millis.toMillis)
    transitWindowCache.start()
    transitWindowCache.get(TransitWindowKey("USD", "CAN")) shouldBe Some(DayRange(3, 5))
    transitWindowCache.get(TransitWindowKey("USD", "CHN")) shouldBe Some(DayRange(2, 6))
    transitWindowCache.get(TransitWindowKey("CAN", "USD")) shouldBe Some(DayRange(1, 7))
    transitWindowCache.get(TransitWindowKey("CAN", "CHN")) shouldBe Some(DayRange(4, 9))

    eventually(Timeout(1.second)) {
      transitWindowCache.get(TransitWindowKey("USD", "CAN")) shouldBe Some(DayRange(2, 6))
      transitWindowCache.get(TransitWindowKey("CAN", "USD")) shouldBe Some(DayRange(3, 4))
      transitWindowCache.get(TransitWindowKey("CAN", "CHN")) shouldBe Some(DayRange(4, 11))
      transitWindowCache.get(TransitWindowKey("USD", "CHN")) shouldBe None
    }
  }

  it should "retrieve no transit windows when the cache is empty" in {
    val dataClient = mock[DataClient]
    when(dataClient.get[String](ArgumentMatchers.eq(Key))(any(), any()))
      .thenReturn(Future.successful(Some(Json.toJson(emptyTransitWindowData).toString())))

    val transitWindowCache = new TransitWindowsCacheImpl(dataClient, 1.minute.toMillis)
    transitWindowCache.start()
    transitWindowCache.get(TransitWindowKey("USD", "CAN")) shouldBe None
  }

}

object TransitWindowsCacheSpec {

  private val Key: String = "transit_windows"
  private val emptyTransitWindowData: OrganizationRatecardTransitWindowsData = OrganizationRatecardTransitWindowsData(Seq.empty[TransitWindow])
  private val initialTransitWindowsData: OrganizationRatecardTransitWindowsData = OrganizationRatecardTransitWindowsData(
    Seq(
      TransitWindow("USD", "CAN", 3, 5),
      TransitWindow("USD", "CHN", 2, 6),
      TransitWindow("CAN", "USD", 1, 7),
      TransitWindow("CAN", "CHN", 4, 9)
    )
  )
  private val updatedTransitWindowData: OrganizationRatecardTransitWindowsData = OrganizationRatecardTransitWindowsData(
    Seq(
      TransitWindow("USD", "CAN", 2, 6),
      TransitWindow("CAN", "USD", 3, 4),
      TransitWindow("CAN", "CHN", 4, 11)
    )
  )

}