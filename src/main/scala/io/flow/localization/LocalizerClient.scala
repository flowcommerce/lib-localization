package io.flow.localization

import java.nio.charset.StandardCharsets
import javax.inject.Inject

import com.twitter.finagle.redis
import com.twitter.util.{Return, Throw, Future => TwitterFuture}
import org.jboss.netty.buffer.ChannelBuffers
import org.slf4j.LoggerFactory

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

  private val log = LoggerFactory.getLogger(getClass)

  import StandardCharsets._

  import RedisLocalizerClient._

  override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    redisClient.get(ChannelBuffers.copiedBuffer(key, UTF_8)).asScala.map(_.map(_.toString(UTF_8)))
      .recover {
        case ex: Throwable => {
          log.warn("FlowError - failed to get key ${key} from redis cache. ${ex.getMessage}", ex)
          None
        }
      }
  }

  override def mGet(keys: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Option[String]]] = {
    redisClient.mGet(keys.map(ChannelBuffers.copiedBuffer(_, UTF_8))).asScala.map(_.map(_.map(_.toString(UTF_8))))
      .recover {
        case ex: Throwable => {
          log.warn("FlowError - failed to mget keys ${keys} from redis cache. ${ex.getMessage}", ex)
          Nil
        }
      }
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
