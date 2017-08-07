package io.flow.localization

import io.flow.localization.RatesCacheImpl.RateKey
import io.flow.published.event.v0.models.json._
import io.flow.published.event.v0.models.{OrganizationRatesData => Rates}
import io.flow.reference.{Currencies, data}
import play.api.libs.json.Json

import scala.concurrent.Future

private[this] trait RateProvider {

  def get(base: String, target: String): Option[BigDecimal]

}

private[localization] class RatesCacheImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends LocalizerClientCache[Rates, RateKey, BigDecimal] with RateProvider {

  import RatesCacheImpl._

  override def get(base: String, target: String): Option[BigDecimal] = {
    super.get(buildKey(base, target))
  }

  override def retrieveData(): Future[Option[Rates]] = {
    localizerClient.get(RatesKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[Rates]
      }
    }
  }

  override def toKeyValues(optionalRates: Option[Rates]): Iterable[((String, String), BigDecimal)] = {
    optionalRates
      .map(computeAllRates)
      .getOrElse(sys.error(s"Rates cannot be found - expected key named '$RatesKey"))
  }

}

private[localization] object RatesCacheImpl {

  // (Base, Target)
  type RateKey = (String, String)

  val RatesKey: String = "rates"
  val UsdIso: String = data.Currencies.Usd.iso42173.toLowerCase
  val One = BigDecimal(1)

  def computeAllRates(originalRates: Rates): Map[RateKey, BigDecimal] = {
    val originalRatesMap: Map[RateKey, BigDecimal] =
      (originalRates.rates.map(rate => buildKey(rate.base, rate.target) -> rate.value) :+
        // append USD -> USD rate if missing
        (buildKey(UsdIso, UsdIso) -> One)).toMap

    val allFormattedCurrencies: Set[String] =
      originalRatesMap
        .keySet
        .flatMap { case (base, target) => Set(base, target) }
        .map(format)

    val allRates: Set[(RateKey, BigDecimal)] = for {
      x <- allFormattedCurrencies
      y <- allFormattedCurrencies
      usdToX <- originalRatesMap.get(UsdIso -> x)
      // avoid division by 0
      if usdToX.signum != 0
      usdToY <- originalRatesMap.get(UsdIso -> y)
    } yield {
      buildKey(x, y) -> usdToY / usdToX
    }

    // keep original rates
    allRates.toMap ++ originalRatesMap
  }

  /**
    * Formats the currency code in a deterministic way, preferring the lowercase
    * ISO 42173 code when available
    */
  private def format(currencyCode: String): String = {
    Currencies.find(currencyCode).map(_.iso42173).getOrElse(currencyCode).toLowerCase
  }

  private def buildKey(base: String, target: String): RateKey = (format(base), format(target))

}
