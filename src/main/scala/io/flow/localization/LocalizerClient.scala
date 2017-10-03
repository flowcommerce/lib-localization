package io.flow.localization

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import javax.inject.Inject

import com.twitter.finagle.redis
import com.twitter.util.{Return, Throw, Future => TwitterFuture}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise => ScalaPromise}
import scala.io.Codec

trait LocalizerClient {

  /**
    * Returns the value associated with the specified key, if any
    */
  def get(key: String, gzipped: Boolean)(implicit ec: ExecutionContext): Future[Option[String]]

  /**
    * Returns the values associated with the specified keys, if any
    */
  def mGet(keys: Seq[String], gzipped: Boolean)(implicit ec: ExecutionContext): Future[Seq[Option[String]]]

}

class RedisLocalizerClient @Inject() (redisClient: redis.Client) extends LocalizerClient {

  private val log = LoggerFactory.getLogger(getClass)

  import StandardCharsets._

  import RedisLocalizerClient._

  override def get(key: String, gzipped: Boolean)(implicit ec: ExecutionContext): Future[Option[String]] = {
    redisClient
      .get(ChannelBuffers.copiedBuffer(key, UTF_8))
      .asScala
      .map(_.map { buf =>
        if (gzipped) decompress(toArray(buf))
        else buf.toString(UTF_8)
      })
      .recover {
        case ex: Throwable => {
          log.warn(s"FlowError - failed to get key $key from redis cache. ${ex.getMessage}", ex)
          None
        }
      }
  }

  override def mGet(keys: Seq[String], gzipped: Boolean)(implicit ec: ExecutionContext): Future[Seq[Option[String]]] = {
    redisClient
      .mGet(keys.map(ChannelBuffers.copiedBuffer(_, UTF_8)))
      .asScala
      .map(_.map(_.map { buf =>
        if (gzipped) decompress(toArray(buf))
        else buf.toString(UTF_8)
      }))
      .recover {
        case ex: Throwable => {
          log.warn(s"FlowError - failed to mget keys ${keys.mkString(",")} from redis cache. ${ex.getMessage}", ex)
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

  private def decompress(compressed: Array[Byte]): String = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
    val res = scala.io.Source.fromInputStream(inputStream)(Codec.UTF8).mkString
    inputStream.close()
    res
  }

}
