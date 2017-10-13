package io.flow.localization

import com.fasterxml.jackson.databind.ObjectMapper
import io.flow.localization.FlowSkuPrice._
import io.flow.reference.data.{Countries, Currencies}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LocalizerSpec extends WordSpec with MockitoSugar with Matchers with ScalaFutures {

  private val pricing50Cad = Map(
    CurrencyKey -> "CAD",
    SalePriceKey -> 50.0,
    MsrpPriceKey -> 100.0
  ).asJava

  private val pricing25Eur = Map(
    CurrencyKey -> "EUR",
    SalePriceKey -> 25.0,
    MsrpPriceKey -> 50.0
  ).asJava

  private val pricing5Eur = Map(
    CurrencyKey -> "EUR",
    SalePriceKey -> 5.0,
    MsrpPriceKey -> 10.0
  ).asJava

  // { "i": "Includes VAT and duty", "c": "PHP", "b": 9100, "m": 22000, "t": 1507581191, "a": 16400 }
  private val serializedPricing = Array(134, 161, 105, 181, 73, 110, 99, 108, 117, 100, 101, 115, 32, 86, 65, 84, 32,
    97, 110, 100, 32, 100, 117, 116, 121, 161, 99, 163, 80, 72, 80, 161, 98, 205, 35, 140, 161, 109, 205, 85, 240, 161,
    116, 206, 89, 219, 221, 7, 161, 97, 205, 64, 16).map(_.toByte)

  private val deserializedPricing = Map[String, Any](
    "i" -> "Includes VAT and duty",
    "c" -> "PHP",
    "b" -> 9100,
    "m" -> 22000,
    "t" -> 1507581191,
    "a" -> 16400
  ).asJava

  "Localizer" should {

    "retrieve a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"c-$country:$itemNumber"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], mock[AvailableCountriesProvider])

      val expected = FlowSkuPrice(pricing50Cad).get

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

    "retrieve a localized pricing by country - using serialized data" in {
      val localizerClient = mock[LocalizerClient]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"c-$country:$itemNumber"
      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(serializedPricing)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], mock[AvailableCountriesProvider])

      val expected = FlowSkuPrice(deserializedPricing).get

      whenReady(localizer.getSkuPriceByCountry(Countries.Can.iso31663, itemNumber = itemNumber)) { res =>
        res shouldBe Some(expected)
        res.get.msrpPrice.get shouldBe expected.msrpPrice.get
      }
    }

    "retrieve a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(value)))

      val localizer = new LocalizerImpl(localizerClient, mock[RateProvider], mock[AvailableCountriesProvider])

      val expected = FlowSkuPrice(pricing50Cad).get

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

    "retrieve and convert a localized pricing by country" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"c-$country:$itemNumber"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      // Verify we can retrieve by iso31663 code
      whenReady(localizer.getSkuPriceByCountryWithCurrency(
        Countries.Can.iso31663, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)
      ) {
        _ shouldBe FlowSkuPrice(pricing25Eur)
      }

      // Verify we can retrieve by iso31662 code lowercase
      whenReady(localizer.getSkuPriceByCountryWithCurrency(
        Countries.Can.iso31662.toLowerCase, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)
      ) {
        _ shouldBe FlowSkuPrice(pricing25Eur)
      }
    }

    "retrieve and convert localized pricings by country" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber1 = "item1"
      val itemNumber2 = "item2"

      val key1 = s"c-$country:$itemNumber1"
      val key2 = s"c-$country:$itemNumber2"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.mGet[Array[Byte]](Seq(key1, key2))).thenReturn(Future.successful(Seq(Some(value), Some(value))))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      whenReady(localizer.getSkuPricesByCountryWithCurrency(country, itemNumbers = Seq(itemNumber1, itemNumber2),
        targetCurrency = Currencies.Eur.iso42173)) { res =>
        res should have size 2
        res(0) shouldBe FlowSkuPrice(pricing25Eur)
        res(1) shouldBe FlowSkuPrice(pricing25Eur)
      }
    }

    "retrieve and convert a localized pricing by experience" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val experienceKey = "canada-2"
      val itemNumber = "item123"

      val key = s"experience-$experienceKey:$itemNumber"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any())).thenReturn(Some(BigDecimal(0.5)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      whenReady(localizer.getSkuPriceByExperienceWithCurrency(experienceKey, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
        _ shouldBe FlowSkuPrice(pricing25Eur)
      }
    }

    "update rates" in {
      val localizerClient = mock[LocalizerClient]
      val rateProvider = mock[RateProvider]

      val country = Countries.Can.iso31663
      val itemNumber = "item123"

      val key = s"c-$country:$itemNumber"
      val value: Array[Byte] = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(pricing50Cad)

      when(localizerClient.get[Array[Byte]](key)).thenReturn(Future.successful(Some(value)))
      when(rateProvider.get(any(), any()))
        .thenReturn(Some(BigDecimal(0.5)))
        .thenReturn(Some(BigDecimal(0.1)))

      val localizer = new LocalizerImpl(localizerClient, rateProvider, mock[AvailableCountriesProvider])

      whenReady(localizer.getSkuPriceByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
        _ shouldBe FlowSkuPrice(pricing25Eur)
      }

      whenReady(localizer.getSkuPriceByCountryWithCurrency(country, itemNumber = itemNumber, targetCurrency = Currencies.Eur.iso42173)) {
        _ shouldBe FlowSkuPrice(pricing5Eur)
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
