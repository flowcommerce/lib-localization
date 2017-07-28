package io.flow.localization

import io.flow.currency.v0.models.Rate
import io.flow.localization.RatesCacheImpl.RateKey
import io.flow.item.v0.models.Rate
import io.flow.item.v0.models.json._
import io.flow.reference.Currencies
import play.api.libs.json.Json

import scala.concurrent.Future

private[this] trait RateProvider {

  def get(base: String, target: String): Option[BigDecimal]

}

// Jean: Not sure if we should use Seq[Rate] here or create a type for it
private[localization] class RatesCacheImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends LocalizerClientCache[Seq[Rate], RateKey, BigDecimal] with RateProvider {

  import RatesCacheImpl._

  override def get(base: String, target: String): Option[BigDecimal] = {
    super.get(buildKey(base, target))
  }

  override def retrieveData(): Future[Option[Seq[Rate]]] = {
    localizerClient.get(RatesKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[Seq[Rate]]
      }
    }
  }

  override def toKeyValues(optionalRates: Option[Seq[Rate]]): Iterable[((String, String), BigDecimal)] = {
    optionalRates
      .map(_.rates.map(rate => buildKey(rate.base, rate.target) -> rate.value))
      .getOrElse(sys.error(s"Rates cannot be found - expected key named '$RatesKey"))
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

object RatesCacheImpl {

  type RateKey = (String, String)

  private val RatesKey = "rates"
}
