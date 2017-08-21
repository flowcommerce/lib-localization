package io.flow.localization

import io.flow.catalog.v0.models.{LocalizedItemPrice, SubcatalogItemStatus}
import io.flow.common.v0.models.{CatalogItemReference, ExperienceSummary, Price, PriceWithBase}
import io.flow.item.v0.models.json._
import io.flow.item.v0.models.{LocalItem, LocalItemPricing}
import io.flow.reference.data.{Countries, Currencies}
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

  private def createItem(pricing: LocalItemPricing) = {
    LocalItem(
      id = "",
      experience = ExperienceSummary("", "", ""),
      item = CatalogItemReference("", ""),
      pricing = pricing,
      status = SubcatalogItemStatus.Included
    )
  }

  private val pricing50Cad = LocalItemPricing (
    price = LocalizedItemPrice(
      currency = "CAD",
      amount = 50,
      label = "CA$50.00",
      base = Price(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      ),
      includes = None
    ),
    attributes = Map(
      "msrp_price" -> PriceWithBase(
        amount = 100,
        currency = "CAD",
        label = "CA$100.00",
        base = Some(Price(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        ))
      )
    )
  )

  private val pricing25Eur = LocalItemPricing (
    price = LocalizedItemPrice(
      currency = "EUR",
      amount = 25,
      label = "EUR25.00",
      base = Price(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      )
    ),
    attributes = Map(
      "msrp_price" -> PriceWithBase(
        amount = 50,
        currency = "EUR",
        label = "EUR50.00",
        base = Some(Price(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        ))
      )
    )
  )

  private val pricing5Eur = LocalItemPricing (
    price = LocalizedItemPrice(
      currency = "EUR",
      amount = 5,
      label = "EUR5.00",
      base = Price(
        currency = "USD",
        amount = 40,
        label = "$40.00"
      )
    ),
    attributes = Map(
      "msrp_price" -> PriceWithBase(
        amount = 10,
        currency = "EUR",
        label = "EUR10.00",
        base = Some(Price(
          amount = 100,
          currency = "USD",
          label = "$75.00"
        ))
      )
    )
  )

  "Localizer" should {

    "retrieve a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], mock[AvailableCountriesProvider])

      val expected = FlowSkuPrice(pricing50Cad)

      eventually(Timeout(3.seconds)) {
        // Verify we can retrieve by iso31663 code
        whenReady(localizer.getSkuPriceByCountry(Countries.Can.iso31663, itemNumber = itemNumber)) { res =>
          res shouldBe Some(expected)
          res.get.msrpPrice.get shouldBe expected.msrpPrice.get
        }

        // Verify we can retrieve by iso31662 code lowercase
        whenReady(localizer.getSkuPriceByCountry(Countries.Can.iso31662.toLowerCase, itemNumber = itemNumber)) { res =>
          res shouldBe Some(expected)
          res.get.msrpPrice.get shouldBe expected.msrpPrice.get
        }
      }
    }

    "retrieve a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], mock[AvailableCountriesProvider])

      val expected = FlowSkuPrice(pricing50Cad)

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getSkuPriceByExperience(experienceKey, itemNumber = itemNumber)) { res =>
          res shouldBe Some(expected)
          res.get.msrpPrice.get shouldBe expected.msrpPrice.get
        }

        // Verify case insensitive
        whenReady(localizer.getSkuPriceByExperience(experienceKey.toUpperCase, itemNumber = itemNumber)) {res =>
          res shouldBe Some(expected)
          res.get.msrpPrice.get shouldBe expected.msrpPrice.get
        }
      }
    }

    "retrieve and convert a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      eventually(Timeout(3.seconds)) {
        // Verify we can retrieve by iso31663 code
        whenReady(localizer.getSkuPriceByCountryWithCurrency(
          Countries.Can.iso31663, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)
        ) {
          _ shouldBe Some(FlowSkuPrice(pricing25Eur))
        }

        // Verify we can retrieve by iso31662 code lowercase
        whenReady(localizer.getSkuPriceByCountryWithCurrency(
          Countries.Can.iso31662.toLowerCase, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)
        ) {
          _ shouldBe Some(FlowSkuPrice(pricing25Eur))
        }
      }
    }

    "retrieve and convert localized pricings by country" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber1 = "item1"
      val itemNumber2 = "item2"

      val key1 = s"country-$country:$itemNumber1"
      val key2 = s"country-$country:$itemNumber2"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.mGet(Seq(key1, key2))).thenReturn(Future.successful(Seq(Some(value), Some(value))))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getSkuPricesByCountryWithCurrency(country, itemNumbers = Seq(itemNumber1, itemNumber2),
          targetCurrency = Currencies.Eur.iso42173)) { res =>
          res should have size 2
          res(0) shouldBe Some(FlowSkuPrice(pricing25Eur))
          res(1) shouldBe Some(FlowSkuPrice(pricing25Eur))
        }
      }
    }

    "retrieve and convert a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      eventually(Timeout(3.seconds)) {
        whenReady(localizer.getSkuPriceByExperienceWithCurrency(experienceKey, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(pricing25Eur))
        }
      }
    }

    "update rates" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"country-$country:$itemNumber"
      val value: String = Json.toJson(createItem(pricing50Cad)).toString

      when(localizerClient.get(key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any()))
        .thenReturn(Some(BigDecimal(0.5)))
        .thenReturn(Some(BigDecimal(0.1)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      eventually(Timeout(1.seconds)) {
        whenReady(localizer.getSkuPriceByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(pricing25Eur))
        }
      }

      eventually(Timeout(2.seconds)) {
        whenReady(localizer.getSkuPriceByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
          _ shouldBe Some(FlowSkuPrice(pricing5Eur))
        }
      }

    }

    "return if a country is enabled" in {
      val localizerClient = mock[LocalizerClient]

      val availableCountriesProvider = mock[AvailableCountriesProvider]
      when(availableCountriesProvider.isEnabled("FRA")).thenReturn(true)
      when(availableCountriesProvider.isEnabled("CAN")).thenReturn(false)

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], availableCountriesProvider)

      localizer.isEnabled("FRA") shouldBe true
      localizer.isEnabled("CAN") shouldBe false
    }

  }

}
