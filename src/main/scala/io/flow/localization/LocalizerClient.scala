package io.flow.localization

import javax.inject.Inject

import com.redis.{RedisClient, RedisClientPool}

import scala.concurrent.{ExecutionContext, Future}

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get(key: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[String]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mget(keys: Seq[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[String]]]

}

class RedisLocalizerClient @Inject() (redisClientPool: RedisClientPool) extends LocalizerClient {

  override def get(key: String)(
    implicit executionContext: ExecutionContext
  ): Future[Option[String]] = {
    futureWithClient(_.get(key))
  }

  override def mget(keys: Seq[String])(
    implicit executionContext: ExecutionContext
  ): Future[List[Option[String]]] = {
    futureWithClient { client =>
      keys.headOption.flatMap { k =>
        // returns an Option[List[Option[String]]]
        client.mget(k, keys.tail:_*)
      }.getOrElse(List.empty)
    }
  }

  private def futureWithClient[T](block: RedisClient => T)(
    implicit executionContext: ExecutionContext
  ): Future[T] = Future {
    redisClientPool.withClient { client =>
      block(client)
    }
  }

}
