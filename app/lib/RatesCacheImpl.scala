package io.flow.lib

import com.gilt.gfc.cache.{CacheConfiguration, SyncCacheImpl}
import com.gilt.gfc.guava.cache.CacheInitializationStrategy
import io.flow.localized.items.cache.v0.models.LocalizedItemCacheRates
import io.flow.localized.items.cache.v0.models.json._
import io.flow.reference.Currencies
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

private[this] trait RateProvider {

  def get(base: String, target: String): Option[BigDecimal]

}

private[lib] class RatesCacheImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends SyncCacheImpl[(String, String), BigDecimal] with CacheConfiguration with RateProvider {

  import RatesCacheImpl._

  private[this] implicit val ec = ExecutionContext.fromExecutor(executor)

  override def cacheInitStrategy: CacheInitializationStrategy = CacheInitializationStrategy.SYNC

  override def get(base: String, target: String): Option[BigDecimal] = {
    super.get(buildKey(base, target))
  }

  override def getSourceObjects: Future[Iterable[((String, String), BigDecimal)]] = {
    getRates().map { optionalRates =>
      optionalRates
        .map(_.rates.map(rate => buildKey(rate.base, rate.target) -> rate.value))
        .getOrElse(sys.error(s"Rates cannot be found - expected key named '$RatesKey"))
    }
  }

  private def getRates()(implicit ec: ExecutionContext): Future[Option[LocalizedItemCacheRates]] = {
    localizerClient.get(RatesKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[LocalizedItemCacheRates]
      }
    }
  }

  /**
    * Formats the currency code in a deterministic way, preferring the lowercase
    * ISO 42173 code when available
    */
  private def format(currencyCode: String): String = {
    Currencies.find(currencyCode).map(_.iso42173).getOrElse(currencyCode).toLowerCase
  }

  private def buildKey(base: String, target: String) = (format(base), format(target))

}

object RatesCacheImpl {
  private val RatesKey = "rates"
}
