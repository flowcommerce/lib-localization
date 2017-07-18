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
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LocalizerSpec extends WordSpec with MockitoSugar with Matchers with Eventually {

  private val price = Price(
    amount = 50,
    currency = "CAD",
    label = "CA$50.00"
  )

  "Localizer" should {

    "retrieve a price by country" in {
      val localizerClient = mock[LocalizerClient]

      val country = "CAN"
      val itemId = "item123"

      val countryKey = s"country-$country:$itemId"
      val value: String = Json.toJson(price).toString

      when(localizerClient.get(countryKey)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient)

      eventually(Timeout(3.seconds)) {
        Await.result(
          localizer.getByCountry(country = country, itemId = itemId),
          3.seconds
        ) shouldBe Some(price)
      }
    }

    "retrieve a price by experience" in {
      val localizerClient = mock[LocalizerClient]

      val experienceId = "ExperienceId"
      val itemId = "item123"

      val experienceKey = s"experience-$experienceId:$itemId"
      val value: String = Json.toJson(price).toString

      when(localizerClient.get(experienceKey)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient)

      eventually(Timeout(3.seconds)) {
        Await.result(
          localizer.getByExperience(experienceId, itemId = itemId),
          3.seconds
        ) shouldBe Some(price)
      }
    }

  }

}
