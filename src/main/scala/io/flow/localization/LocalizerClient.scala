package io.flow.localization

import java.nio.charset.StandardCharsets
import javax.inject.Inject

import com.twitter.finagle.redis
import com.twitter.util.{Return, Throw, Future => TwitterFuture}
import org.jboss.netty.buffer.ChannelBuffers

import scala.concurrent.{ExecutionContext, Future, Promise => ScalaPromise}

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mGet(keys: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Option[String]]]

}

class RedisLocalizerClient @Inject() (redisClient: redis.Client) extends LocalizerClient {

  import StandardCharsets._

  import RedisLocalizerClient._

  override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    redisClient.get(ChannelBuffers.copiedBuffer(key, UTF_8)).asScala.map(_.map(_.toString(UTF_8)))
  }

  override def mGet(keys: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Option[String]]] = {
    redisClient.mGet(keys.map(ChannelBuffers.copiedBuffer(_, UTF_8))).asScala.map(_.map(_.map(_.toString(UTF_8))))
  }

}

object RedisLocalizerClient {

  // as defined in https://twitter.github.io/util/guide/util-cookbook/futures.html
  private implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
    def asScala(implicit e: ExecutionContext): Future[A] = {
      val promise: ScalaPromise[A] = ScalaPromise()
      tf.respond {
        case Return(value) => promise.success(value)
        case Throw(exception) => promise.failure(exception)
      }
      promise.future
    }
  }

}
