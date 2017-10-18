package io.flow.utils

import com.gilt.gfc.cache.{CacheConfiguration, SyncCacheImpl}
import com.gilt.gfc.guava.cache.CacheInitializationStrategy

import scala.concurrent.{ExecutionContext, Future}

trait Cache[T, K, V] extends SyncCacheImpl[K, V] with CacheConfiguration {

  protected[this] implicit val ec = ExecutionContext.fromExecutor(executor)

  override def cacheInitStrategy: CacheInitializationStrategy = CacheInitializationStrategy.SYNC

  override def getSourceObjects: Future[Iterable[(K, V)]] = retrieveData().map(toKeyValues)

  def retrieveData(): Future[Option[T]]

  def toKeyValues(retrievedData: Option[T]): Iterable[(K, V)]

}