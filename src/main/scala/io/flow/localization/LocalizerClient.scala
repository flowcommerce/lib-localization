package io.flow.localization

import javax.inject.Inject

import com.twitter.finagle.redis
import com.twitter.finagle.redis.util._
import com.twitter.util.{Return, Throw, Future => TwitterFuture}

import scala.concurrent.{ExecutionContext, Future => ScalaFuture, Promise => ScalaPromise}

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get(key: String)(implicit ec: ExecutionContext): ScalaFuture[Option[String]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mGet(keys: Seq[String])(implicit ec: ExecutionContext): ScalaFuture[Seq[Option[String]]]

}

class RedisLocalizerClient @Inject() (redisClient: redis.Client) extends LocalizerClient {

  import RedisLocalizerClient._

  override def get(key: String)(implicit ec: ExecutionContext): ScalaFuture[Option[String]] = {
    redisClient.get(StringToBuf(key)).asScala.map(_.map(BufToString.apply))
  }

  override def mGet(keys: Seq[String])(implicit ec: ExecutionContext): ScalaFuture[Seq[Option[String]]] = {
    redisClient.mGet(keys.map(StringToBuf.apply)).asScala.map(_.map(_.map(BufToString.apply)))
  }

}

object RedisLocalizerClient {

  // as defined in https://twitter.github.io/util/guide/util-cookbook/futures.html
  private implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
    def asScala(implicit e: ExecutionContext): ScalaFuture[A] = {
      val promise: ScalaPromise[A] = ScalaPromise()
      tf.respond {
        case Return(value) => promise.success(value)
        case Throw(exception) => promise.failure(exception)
      }
      promise.future
    }
  }

}
