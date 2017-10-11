package io.flow.localization

import io.flow.currency.v0.models.Rate
import io.flow.localization.RatesCacheImpl._
import io.flow.published.event.v0.models.OrganizationRatesData
import io.flow.published.event.v0.models.json._
import io.flow.reference.data.Currencies
import io.flow.reference.v0.models.Currency
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

class RateCacheSpec extends WordSpec with MockitoSugar with Matchers with Eventually {

  def createRate(base: Currency, target: Currency, value: Double) = {
    Rate(
      id = "",
      base = base.iso42173,
      target = target.iso42173,
      value = value,
      effectiveAt = DateTime.now
    )
  }

  val firstRate = createRate(Currencies.Cad, Currencies.Eur, 0.5)

  val firstRates = OrganizationRatesData(rates = Seq(firstRate))
  val secondRates = OrganizationRatesData(rates = Seq(firstRate.copy(value = 0.1)))

  "RatesCache" should {

    "retrieve the rates" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq(RatesKey))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(firstRates).toString)))

      val ratesCache = new RatesCacheImpl(localizerClient, 1.minute.toMillis)
      ratesCache.start()

      ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(0.5))
    }

    "refresh the rates" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq(RatesKey))(any(), any()))
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

    "compute all rates" in {
      val rates = Seq(
        createRate(Currencies.Usd, Currencies.Cad, 0.5),
        createRate(Currencies.Usd, Currencies.Eur, 4),
        createRate(Currencies.Usd, Currencies.Jpy, 10)
      )
      val originalRates = OrganizationRatesData(rates = rates)

      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq(RatesKey))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(originalRates).toString)))

      val ratesCache = new RatesCacheImpl(localizerClient, 1.minute.toMillis)
      ratesCache.start()

      ratesCache.get(Currencies.Usd.iso42173, Currencies.Usd.iso42173) shouldBe Some(BigDecimal(1))
      ratesCache.get(Currencies.Usd.iso42173, Currencies.Cad.iso42173) shouldBe Some(BigDecimal(0.5))
      ratesCache.get(Currencies.Usd.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(4))
      ratesCache.get(Currencies.Usd.iso42173, Currencies.Jpy.iso42173) shouldBe Some(BigDecimal(10))

      ratesCache.get(Currencies.Cad.iso42173, Currencies.Usd.iso42173) shouldBe Some(BigDecimal(2))
      ratesCache.get(Currencies.Cad.iso42173, Currencies.Cad.iso42173) shouldBe Some(BigDecimal(1))
      ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(8))
      ratesCache.get(Currencies.Cad.iso42173, Currencies.Jpy.iso42173) shouldBe Some(BigDecimal(20))

      ratesCache.get(Currencies.Eur.iso42173, Currencies.Usd.iso42173) shouldBe Some(BigDecimal(0.25))
      ratesCache.get(Currencies.Eur.iso42173, Currencies.Cad.iso42173) shouldBe Some(BigDecimal(0.125))
      ratesCache.get(Currencies.Eur.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(1))
      ratesCache.get(Currencies.Eur.iso42173, Currencies.Jpy.iso42173) shouldBe Some(BigDecimal(2.5))

      ratesCache.get(Currencies.Jpy.iso42173, Currencies.Usd.iso42173) shouldBe Some(BigDecimal(0.1))
      ratesCache.get(Currencies.Jpy.iso42173, Currencies.Cad.iso42173) shouldBe Some(BigDecimal(0.05))
      ratesCache.get(Currencies.Jpy.iso42173, Currencies.Eur.iso42173) shouldBe Some(BigDecimal(0.4))
      ratesCache.get(Currencies.Jpy.iso42173, Currencies.Jpy.iso42173) shouldBe Some(BigDecimal(1))
    }

  }

  "return empty rates if key is missing" in {
    val localizerClient = mock[LocalizerClient]
    when(localizerClient.get[String](ArgumentMatchers.eq(RatesKey))(any(), any()))
      .thenReturn(Future.successful(None))

    val ratesCache = new RatesCacheImpl(localizerClient, 1.minute.toMillis)
    ratesCache.start()

    ratesCache.get(Currencies.Cad.iso42173, Currencies.Eur.iso42173) shouldBe None
  }

}
