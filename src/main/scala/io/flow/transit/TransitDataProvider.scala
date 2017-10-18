package io.flow.transit

import javax.inject.Inject

import com.twitter.finagle.redis
import io.flow.countries.{AvailableCountriesProvider, AvailableCountriesProviderCacheImpl}
import io.flow.transit.utils.{DayRange, TransitWindowKey}
import io.flow.utils.RedisDataClient

import scala.concurrent.duration.{FiniteDuration, MINUTES}

trait TransitDataProvider {

  /**
    * Returns the [[DayRange]] associated with shipping from the origin country to the destination country
    *
    * @param origin country in the ISO 3166-3 format that the package is being shipped from
    * @param destination country in the ISO 3166-3 format that the package is being delivered to
    * @return the minimum and maximum days it could take for a package to be shipped from the origin country to the destination country
    */
  def getTransitWindowByOriginAndDestination(origin: String, destination: String): Option[DayRange]

  /**
    * Returns true if the specified country is enabled, false otherwise
    * @param country country in the ISO 3166-3 format
    * @return true if the specified country is enabled, false otherwise
    */
  def isEnabled(country: String): Boolean
}

object TransitDataProvider {

  private val defaultTransitWindowsRefreshPeriod = FiniteDuration(1, MINUTES)

  def apply(redisClient: redis.Client, transitWindowsRefreshPeriod: FiniteDuration = defaultTransitWindowsRefreshPeriod): TransitDataProvider = {
    val dataClient = new RedisDataClient(redisClient)

    val transitWindowsCache = new TransitWindowsCacheImpl(dataClient, transitWindowsRefreshPeriod.toMillis)
    transitWindowsCache.start()

    val availableCountriesProvider = new AvailableCountriesProviderCacheImpl(dataClient, transitWindowsRefreshPeriod.toMillis)
    availableCountriesProvider.start()


    new TransitDataProviderImpl(
      transitWindowsCache,
      availableCountriesProvider
    )
  }

}

class TransitDataProviderImpl @Inject()(transitWindowsCache: TransitWindowsCacheImpl,
                                        availableCountriesProvider: AvailableCountriesProvider) extends TransitDataProvider {

  override def getTransitWindowByOriginAndDestination(origin: String, destination: String): Option[DayRange] = {
      transitWindowsCache.get(TransitWindowKey(origin, destination))
  }

  override def isEnabled(country: String): Boolean = availableCountriesProvider.isEnabled(country)

}