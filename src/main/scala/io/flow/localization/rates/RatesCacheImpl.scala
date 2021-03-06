package io.flow.localization.rates

import io.flow.localization.rates.RatesCacheImpl.RateKey
import io.flow.published.event.v0.models.json._
import io.flow.published.event.v0.models.{OrganizationRatesData => Rates}
import io.flow.reference.{Currencies, data}
import play.api.libs.json.Json
import io.flow.localization.utils.DataClientConversions._
import io.flow.localization.utils.{Cache, DataClient}

import scala.concurrent.Future

private[this] trait RateProvider {

  def get(base: String, target: String): Option[BigDecimal]

}

private[rates] class RatesCacheImpl(dataClient: DataClient, override val refreshPeriodMs: Long)
  extends Cache[Rates, RateKey, BigDecimal] with RateProvider {

  import RatesCacheImpl._

  override def get(base: String, target: String): Option[BigDecimal] = {
    super.get(buildKey(base, target))
  }

  override def retrieveData(): Future[Option[Rates]] = {
    dataClient.get[String](RatesKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[Rates]
      }
    }
  }

  override def toKeyValues(optionalRates: Option[Rates]): Iterable[((String, String), BigDecimal)] = {
    optionalRates
      .map(computeAllRates)
      .getOrElse{
        warn(s"Rates cannot be found - expected key named '$RatesKey'. Returning no rates.")
        Iterable.empty
      }
  }

}

private[rates] object RatesCacheImpl {

  // (Base, Target)
  type RateKey = (String, String)

  val RatesKey: String = "rates"

  val ReferenceCurrency = data.Currencies.Usd
  val ReferenceCurrencyIso: String = format(ReferenceCurrency.iso42173)
  val One = BigDecimal(1)

  def computeAllRates(originalRates: Rates): Map[RateKey, BigDecimal] = {
    val originalRatesMap: Map[RateKey, BigDecimal] =
      (originalRates.rates.map(rate => buildKey(rate.base, rate.target) -> rate.value) :+
        // append Base -> Base rate if missing
        (buildKey(ReferenceCurrencyIso, ReferenceCurrencyIso) -> One)).toMap

    val allFormattedCurrencies: Set[String] =
      originalRatesMap
        .keySet
        .flatMap { case (base, target) => Set(base, target) }
        .map(format)

    val allRates: Set[(RateKey, BigDecimal)] = for {
      x <- allFormattedCurrencies
      y <- allFormattedCurrencies
      refToX <- originalRatesMap.get(ReferenceCurrencyIso -> x)
      // avoid division by 0
      if refToX.signum != 0
      refToY <- originalRatesMap.get(ReferenceCurrencyIso -> y)
    } yield {
      buildKey(x, y) -> refToY / refToX
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
