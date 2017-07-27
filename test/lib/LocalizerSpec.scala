package io.flow.lib

import io.flow.localized.items.cache.v0.models._
import io.flow.localized.items.cache.v0.models.json._
import io.flow.reference.data.{Countries, Currencies}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class LocalizerSpec extends WordSpec with MockitoSugar with Matchers with Eventually with ScalaFutures {

  private val pricing = LocalizedItemCachePricing (
    price = LocalizedItemCachePrices(
      local = LocalizedItemCachePrice(
        currency = "CAD",
        amount = 50,
        label = "CA$50.00"
      ),
      base = LocalizedItemCachePrice(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      )
    ),
    includes = None,
    attributes = Map(
      "msrp" -> LocalizedItemCachePrices(
        local = LocalizedItemCachePrice(
          amount = 100,
          currency = "CAD",
          label = "CA$100.00"
        ),
        base = LocalizedItemCachePrice(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        )
      )
    )
  )

  private val convertedPricing = LocalizedItemCachePricing (
    price = LocalizedItemCachePrices(
      local = LocalizedItemCachePrice(
        currency = "EUR",
        amount = 25,
        label = "EUR25.00"
      ),
      base = LocalizedItemCachePrice(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      )
    ),
    includes = None,
    attributes = Map(
      "msrp" -> LocalizedItemCachePrices(
        local = LocalizedItemCachePrice(
          amount = 50,
          currency = "EUR",
          label = "EUR50.00"
        ),
        base = LocalizedItemCachePrice(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        )
      )
    )
  )

  private val convertedPricingAfterResfresh = LocalizedItemCachePricing (
    price = LocalizedItemCachePrices(
      local = LocalizedItemCachePrice(
        currency = "EUR",
        amount = 5,
        label = "EUR5.00"
      ),
      base = LocalizedItemCachePrice(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      )
    ),
    includes = None,
    attributes = Map(
      "msrp" -> LocalizedItemCachePrices(
        local = LocalizedItemCachePrice(
          amount = 10,
          currency = "EUR",
          label = "EUR10.00"
        ),
        base = LocalizedItemCachePrice(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        )
      )
    )
  )

  "Localizer" should {

    "retrieve a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]

      val country = Countries.Can.iso31662.toLowerCase
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(ArgumentMatchers.eq(key))(any())).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider])

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getByCountry(country, itemNumber = itemNumber)) {
          _ shouldBe Some(FlowSkuPrice(pricing))
        }

        // Verify can retrieve by three characters country code
        whenReady(localizer.getByCountry("CAN", itemNumber = itemNumber)) {
          _ shouldBe Some(FlowSkuPrice(pricing))
        }
      }
    }

    "retrieve a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(ArgumentMatchers.eq(key))(any())).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider])

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getByExperience(experienceKey, itemNumber = itemNumber)) {
          _ shouldBe Some(FlowSkuPrice(pricing))
        }

        // Verify case insensitive
        whenReady(localizer.getByExperience(experienceKey.toUpperCase, itemNumber = itemNumber)) {
          _ shouldBe Some(FlowSkuPrice(pricing))
        }
      }
    }

    "retrieve and convert a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31662.toLowerCase
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      val rates = LocalizedItemCacheRates(
        rates = Seq(
          LocalizedItemCacheRate(
            id = "",
            base = Currencies.Cad.iso42173,
            target = Currencies.Eur.iso42173,
            value = 0.5,
            effectiveAt = DateTime.now
          )
        )
      )

      when(localizerClient.get(ArgumentMatchers.eq(key))(any())).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider)

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(convertedPricing))
        }
      }
    }

    "retrieve and convert a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(ArgumentMatchers.eq(key))(any())).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider)

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getByExperienceWithCurrency(experienceKey, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(convertedPricing))
        }
      }
    }

    "rates should refresh" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31662.toLowerCase
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(pricing).toString

      when(localizerClient.get(ArgumentMatchers.eq(key))(any())).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any()))
        .thenReturn(Some(BigDecimal(0.5)))
        .thenReturn(Some(BigDecimal(0.1)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider)

      eventually(Timeout(1.seconds)) {
        whenReady(localizer.getByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(convertedPricing))
        }
      }

      eventually(Timeout(2.seconds)) {
        whenReady(localizer.getByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(convertedPricingAfterResfresh))
        }
      }

    }

  }

}
