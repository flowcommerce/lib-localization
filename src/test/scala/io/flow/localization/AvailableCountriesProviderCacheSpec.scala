package io.flow.localization

import io.flow.published.event.v0.models.OrganizationCountriesData
import io.flow.published.event.v0.models.json._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

class AvailableCountriesProviderCacheSpec extends WordSpec with MockitoSugar with Matchers with Eventually {

  val firstCountries = OrganizationCountriesData(available = Seq("FRA"))
  val secondCountries = OrganizationCountriesData(available = Seq("USA", "CAN"))

  "AvailableCountriesProviderCache" should {

    "retrieve the enabled countries case insensitively" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq("organization_countries"))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(firstCountries).toString)))

      val countriesCache = new AvailableCountriesProviderCacheImpl(localizerClient, 1.minute.toMillis)
      countriesCache.start()

      countriesCache.isEnabled("FRA") shouldBe true
      countriesCache.isEnabled("fr") shouldBe true
      countriesCache.isEnabled("USA") shouldBe false
      countriesCache.isEnabled("usa") shouldBe false
    }

    "refresh the enabled countries and retrieve case insensitively" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq("organization_countries"))(any(), any()))
        .thenReturn(Future.successful(Some(Json.toJson(firstCountries).toString)))
        .thenReturn(Future.successful(Some(Json.toJson(secondCountries).toString)))

      val countriesCache = new AvailableCountriesProviderCacheImpl(localizerClient, 100.millis.toMillis)
      countriesCache.start()

      countriesCache.isEnabled("FRA") shouldBe true
      countriesCache.isEnabled("fr") shouldBe true
      countriesCache.isEnabled("USA") shouldBe false
      countriesCache.isEnabled("usa") shouldBe false

      eventually(Timeout(200.millis)) {
        countriesCache.isEnabled("FRA") shouldBe false
        countriesCache.isEnabled("fr") shouldBe false
        countriesCache.isEnabled("USA") shouldBe true
        countriesCache.isEnabled("usa") shouldBe true
        countriesCache.isEnabled("CAN") shouldBe true
      }
    }

    "return no enabled countries if key is missing" in {
      val localizerClient = mock[LocalizerClient]
      when(localizerClient.get[String](ArgumentMatchers.eq("organization_countries"))(any(), any()))
        .thenReturn(Future.successful(None))

      val countriesCache = new AvailableCountriesProviderCacheImpl(localizerClient, 1.minute.toMillis)
      countriesCache.start()

      countriesCache.isEnabled("FRA") shouldBe false
      countriesCache.isEnabled("fr") shouldBe false
      countriesCache.isEnabled("USA") shouldBe false
      countriesCache.isEnabled("usa") shouldBe false
    }

  }

}
