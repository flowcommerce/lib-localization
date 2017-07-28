package io.flow.localization

import io.flow.currency.v0.models.Rate
import io.flow.item.v0.models.LocalizedItemCacheRates
import io.flow.item.v0.models.json._
import io.flow.reference.data.Currencies
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

class RateCacheSpec extends WordSpec with MockitoSugar with Matchers with Eventually {

  val firstRate = Rate(
    id = "",
    base = Currencies.Cad.iso42173,
    target = Currencies.Eur.iso42173,
    value = 0.5,
    effectiveAt = DateTime.now
  )

  val firstRates = LocalizedItemCacheRates(rates = Seq(firstRate))
  val secondRates = LocalizedItemCacheRates(rates = Seq(firstRate.copy(value = 0.1)))

  "RatesCache" should {

    "retrieve the rates" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get(ArgumentMatchers.eq("rates"))(any()))
        .thenReturn(Future.successful(Some(Json.toJson(firstRates).toString)))

      val ratesCache = new RatesCacheImpl(localizerClient, 1.minute.toMillis)
      ratesCache.start()

      ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(0.5))
    }

    "refresh the rates" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get(ArgumentMatchers.eq("rates"))(any()))
        // first call with a rate of 0.5
        .thenReturn(Future.successful(Some(Json.toJson(firstRates).toString)))
        // second call with a rate of 0.1
        .thenReturn(Future.successful(Some(Json.toJson(secondRates).toString)))

      val ratesCache = new RatesCacheImpl(localizerClient, 100.millis.toMillis)
      ratesCache.start()

      ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(0.5))
      eventually(Timeout(200.millis)) {
        ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(0.1))
      }
    }

  }

}
