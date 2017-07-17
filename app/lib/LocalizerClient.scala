package io.flow.lib


import javax.inject.Inject

import com.redis.{RedisClient, RedisClientPool}

import scala.concurrent.{ExecutionContext, Future}


trait LocalizerClient {

  def get(key: String): Future[Option[String]]

}

class RedisLocalizerClient @Inject() (redisClientPool: RedisClientPool)(
  implicit executionContext: ExecutionContext
) extends LocalizerClient {

  override def get(key: String): Future[Option[String]] = {
    futureWithClient(
      _.get(key)
    )
  }

  private def futureWithClient[T](block: RedisClient => T): Future[T] = Future {
    redisClientPool.withClient { client =>
      block(client)
    }
  }
}
