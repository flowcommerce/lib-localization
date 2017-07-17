package io.flow.lib

import io.flow.common.v0.models.Price
import io.flow.common.v0.models.json._
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class LocalizerSpec extends WordSpec with MockitoSugar with Matchers with Eventually{

  "Localizer" should {
    "retrieve a price" in {
      val localizerClient = mock[LocalizerClient]

      val country = "CAN"
      val sku = "sku123"
      val price = Price(
        amount = 50,
        currency = "CAD",
        label = "CA$50.00"
      )

      val key = s"$country:$sku"
      val value: String = Json.toJson(price).toString

      when(
        localizerClient.get(key)
      ).thenReturn(
        Future.successful(Some(value))
      )

      val localizer = new LocalizerImpl(localizerClient)

      eventually(Timeout(3.seconds)) {
        Await.result(
          localizer.get(country = country, sku = sku),
          3.seconds
        ) shouldBe Some(price)
      }
    }
  }

}
