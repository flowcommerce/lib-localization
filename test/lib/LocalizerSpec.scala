package io.flow.lib

import io.flow.common.v0.models.{Price, PriceWithBase}
import io.flow.localized.items.cache.v0.models.{LocalizedCachePrice, LocalizedPricing}
import io.flow.localized.items.cache.v0.models.json._
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

  private val pricing = LocalizedPricing(
    prices = Seq(
      LocalizedCachePrice(
        key = "sale",
        price = PriceWithBase(
          amount = 50,
          currency = "CAD",
          label = "CA$50.00",
          base = Some(Price(
            amount = 50,
            currency = "CAD",
            label = "CA$50.00"
          ))
        )
      )
    )
  )

  "Localizer" should {

    "retrieve a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]

      val country = "CAN"
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient)

      eventually(Timeout(3.seconds)) {
        Await.result(
          localizer.getByCountry(country = country, itemNumber = itemNumber),
          3.seconds
        ) shouldBe Some(pricing)
      }
    }

    "retrieve a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient)

      eventually(Timeout(3.seconds)) {
        Await.result(
          localizer.getByExperience(key, itemNumber = itemNumber),
          3.seconds
        ) shouldBe Some(pricing)
      }
    }

  }

}
