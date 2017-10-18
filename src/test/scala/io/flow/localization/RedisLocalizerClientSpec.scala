package io.flow.localization

import java.nio.charset.StandardCharsets.UTF_8

import com.twitter.finagle.redis
import com.twitter.util.Future
import io.flow.utils.RedisDataClient
import org.jboss.netty.buffer.ChannelBuffers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class RedisLocalizerClientSpec extends WordSpec with MockitoSugar with Matchers with ScalaFutures {

  import io.flow.utils.DataClientConversions._

  "RedisLocalizerClient" should {

    "get a string value" in {
      val res = "hop"
      val key = "key"
      val client = mock[redis.Client]
      when(client.get(ChannelBuffers.copiedBuffer(key, UTF_8))).thenReturn(Future.value(Some(ChannelBuffers.wrappedBuffer(res.getBytes(UTF_8)))))
      val dataClient = new RedisDataClient(client)

      whenReady(dataClient.get[String](key)) { result =>
        result.get shouldBe res
      }
    }

    "mget string values" in {
      val res = Seq("hop1", "hop2")
      val keys = Seq("key1", "key2")
      val redisKeys = keys.map(ChannelBuffers.copiedBuffer(_, UTF_8))
      val client = mock[redis.Client]
      when(client.mGet(redisKeys)).thenReturn(Future.value(res.map(r => Some(ChannelBuffers.wrappedBuffer(r.getBytes(UTF_8))))))
      val dataClient = new RedisDataClient(client)

      whenReady(dataClient.mGet[String](keys)) { results =>
        results should have size 2
        results.map(_.get) should contain theSameElementsInOrderAs res
      }
    }

    "get a byte array value" in {
      val res = Array[Byte](1, 2, 3, 4, 5)
      val key = "key"
      val client = mock[redis.Client]
      when(client.get(ChannelBuffers.copiedBuffer(key, UTF_8))).thenReturn(Future.value(Some(ChannelBuffers.wrappedBuffer(res))))
      val dataClient = new RedisDataClient(client)

      whenReady(dataClient.get[Array[Byte]](key)) { result =>
        result.get shouldBe res
      }
    }

    "mget byte array values" in {
      val res = Seq(Array[Byte](1, 2, 3, 4, 5), Array[Byte](7, 8, 9))
      val keys = Seq("key1", "key2")
      val redisKeys = keys.map(ChannelBuffers.copiedBuffer(_, UTF_8))
      val client = mock[redis.Client]
      when(client.mGet(redisKeys)).thenReturn(Future.value(res.map(r => Some(ChannelBuffers.wrappedBuffer(r)))))
      val redisDataClient = new RedisDataClient(client)

      whenReady(redisDataClient.mGet[Array[Byte]](keys)) { results =>
        results should have size 2
        results.map(_.get) should contain theSameElementsInOrderAs res
      }
    }

  }

}
