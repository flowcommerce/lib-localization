package io.flow.lib

import com.gilt.gfc.cache.{CacheConfiguration, SyncCacheImpl}
import com.gilt.gfc.guava.cache.CacheInitializationStrategy
import io.flow.localized.items.cache.v0.models.LocalizedItemCacheRates
import io.flow.localized.items.cache.v0.models.json._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

private[this] trait RateProvider {

  def get(base: String, target: String): Option[BigDecimal]

}

private[lib] class RatesCacheImpl(localizerClient: LocalizerClient, override val refreshPeriodMs: Long)
  extends SyncCacheImpl[(String, String), BigDecimal] with CacheConfiguration with RateProvider {

  import RatesCacheImpl._

  implicit val ec = ExecutionContext.fromExecutor(executor)

  override def cacheInitStrategy: CacheInitializationStrategy = CacheInitializationStrategy.SYNC

  override def get(base: String, target: String): Option[BigDecimal] = super.get((base.toLowerCase, target.toLowerCase))

  override def getSourceObjects: Future[Iterable[((String, String), BigDecimal)]] = {
    getRates().map { optionalRates =>
      optionalRates
        .map(_.rates.map(rate => (rate.base.toLowerCase, rate.target.toLowerCase) -> rate.value))
        .getOrElse(sys.error("Rates cannot be found"))
    }
  }

  private def getRates()(implicit ec: ExecutionContext): Future[Option[LocalizedItemCacheRates]] = {
    localizerClient.get(RatesKey).map { optionalJson =>
      optionalJson.map { js =>
        Json.parse(js).as[LocalizedItemCacheRates]
      }
    }
  }

}

object RatesCacheImpl {
  private val RatesKey = "rates"
}
