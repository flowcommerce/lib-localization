package io.flow.localization

import javax.inject.Inject

import redis.RedisClientPool

import scala.concurrent.Future

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get(key: String): Future[Option[String]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mget(keys: Seq[String]): Future[Seq[Option[String]]]

}

class RedisLocalizerClient @Inject() (redisClientPool: RedisClientPool) extends LocalizerClient {

  override def get(key: String): Future[Option[String]] = {
    redisClientPool.get[String](key)
  }

  override def mget(keys: Seq[String]): Future[Seq[Option[String]]] = {
    redisClientPool.mget[String](keys:_*)
  }

}
