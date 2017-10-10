package io.flow.localization

import java.nio.charset.StandardCharsets
import javax.inject.Inject

import com.twitter.finagle.redis
import com.twitter.util.{Return, Throw, Future => TwitterFuture}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise => ScalaPromise}

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get[T](key: String)(implicit converter: Array[Byte] => T = identity _, ec: ExecutionContext): Future[Option[T]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mGet[T](keys: Seq[String])(implicit converter: Array[Byte] => T = identity _, ec: ExecutionContext): Future[Seq[Option[T]]]

}

class RedisLocalizerClient @Inject() (redisClient: redis.Client) extends LocalizerClient {

  private val log = LoggerFactory.getLogger(getClass)

  import StandardCharsets._

  import RedisLocalizerClient._

  override def get[T](key: String)(implicit converter: Array[Byte] => T = identity _, ec: ExecutionContext): Future[Option[T]] = {
    redisClient
      .get(ChannelBuffers.copiedBuffer(key, UTF_8))
      .asScala
      .map(_.map(c => converter(toArray(c))))
      .recover {
        case ex: Throwable => {
          log.warn(s"FlowError - failed to get key $key from redis cache. ${ex.getMessage}", ex)
          None
        }
      }
  }

  override def mGet[T](keys: Seq[String])(implicit converter: Array[Byte] => T = identity _, ec: ExecutionContext): Future[Seq[Option[T]]] = {
    redisClient
      .mGet(keys.map(ChannelBuffers.copiedBuffer(_, UTF_8)))
      .asScala
      .map(_.map(_.map(c => converter(toArray(c)))))
      .recover {
        case ex: Throwable => {
          log.warn(s"FlowError - failed to mget keys ${keys.mkString(", ")} from redis cache. ${ex.getMessage}", ex)
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

  private def toArray(buf: ChannelBuffer): Array[Byte] = {
    if (buf.hasArray && buf.arrayOffset() == 0 && buf.readableBytes() == buf.capacity()) {
      // we have no offset and the length is the same as the capacity. Its safe to reuse the array without copy it first
      buf.array()
    } else {
      // copy the ChannelBuffer to a byte array
      val res = new Array[Byte](buf.readableBytes())
      buf.getBytes(0, res)
      res
    }
  }

}

object LocalizerClientConverter {
  implicit def identityConverter(bytes: Array[Byte]): Array[Byte] = bytes
  implicit def utf8StringConverter(bytes: Array[Byte]): String = new String(bytes, StandardCharsets.UTF_8)
}
