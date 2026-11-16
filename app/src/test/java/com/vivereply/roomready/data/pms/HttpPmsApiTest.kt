package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpPmsApiTest {

  private val base = PmsAddress("https://pms.demo13.com")
  private val key = "rk.k597.secret-77"

  @Test
  fun `bootstrap sends bearer auth and returns body`() =
    runTest {
      val factory = FakeConnectionFactory(200, "{\"ok\":true}")
      val api = HttpPmsApi(factory)

      val body = api.fetchBootstrap(base, key)
      assertEquals("{\"ok\":true}", body)
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/bootstrap", sent.url.toString())
      assertEquals("GET", sent.requestMethod)
      assertEquals("Bearer rk.k597.secret-77", sent.requestProps["Authorization"])
    }

  @Test
  fun `bootstrap maps 401 and 403 to auth failures`() =
    runTest {
      listOf(401, 403).forEach { code ->
        val api = HttpPmsApi(FakeConnectionFactory(code, ""))
        try {
          api.fetchBootstrap(base, key)
          throw AssertionError("expected PmsApiException for $code")
        } catch (e: PmsApiException) {
          assertEquals(code, e.statusCode)
          assertTrue(e.isAuthFailure())
          assertTrue(e.parkable())
        }
      }
    }

  @Test
  fun `bootstrap 404 is not found and never carries the key`() =
    runTest {
      val api = HttpPmsApi(FakeConnectionFactory(404, ""))
      try {
        api.fetchBootstrap(base, key)
        throw AssertionError("expected PmsApiException")
      } catch (e: PmsApiException) {
        assertTrue(e.isNotFound())
        assertFalse((e.message ?: "").contains("secret-77"))
      }
    }

  @Test
  fun `push posts the envelope with actor and embedded payload`() =
    runTest {
      val factory = FakeConnectionFactory(202, "")
      val api = HttpPmsApi(factory)

      api.push(
        PushRequest(
          baseUrl = base,
          apiKey = key,
          entityType = SyncEntityType.UNIT_STATUS,
          entityId = "u-101",
          op = SyncOp.UPSERT,
          payloadJson = "{\"housekeeping\":\"CLEAN\"}",
          actor = "app:k597:Ayesha",
          atEpochMillis = 1_000L,
        ),
      )
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/outbound", sent.url.toString())
      assertEquals("POST", sent.requestMethod)
      assertEquals("Bearer rk.k597.secret-77", sent.requestProps["Authorization"])
      val body = sent.postedBody
      assertTrue(body.contains("\"entityType\":\"UNIT_STATUS\""))
      assertTrue(body.contains("\"entityId\":\"u-101\""))
      assertTrue(body.contains("\"payload\":{\"housekeeping\":\"CLEAN\"}"))
      assertTrue(body.contains("\"actor\":\"app:k597:Ayesha\""))
      assertTrue(body.contains("\"at\":1000"))
    }

  @Test
  fun `push failures split parkable from retryable`() =
    runTest {
      val apiFor: (Int) -> HttpPmsApi = { code ->
        HttpPmsApi(FakeConnectionFactory(code, ""))
      }
      suspend fun statusOf(code: Int): PmsApiException =
        try {
          apiFor(code).push(push("u-1"))
          throw AssertionError("expected failure for $code")
        } catch (e: PmsApiException) {
          e
        }

      assertTrue(statusOf(400).parkable())
      assertTrue(statusOf(404).parkable())
      assertTrue(statusOf(422).parkable())
      assertFalse(statusOf(408).parkable())
      assertFalse(statusOf(429).parkable())
      assertFalse(statusOf(500).parkable())
      assertFalse(statusOf(503).parkable())
    }

  @Test
  fun `connection failures surface with null status and cause`() =
    runTest {
      val factory =
        FakeConnectionFactory(
          200, "", failure = UnknownHostException("dns"),
        )
      try {
        HttpPmsApi(factory).fetchBootstrap(base, key)
        throw AssertionError("expected PmsApiException")
      } catch (e: PmsApiException) {
        assertEquals(null, e.statusCode)
        assertFalse(e.parkable())
        assertTrue(e.cause is UnknownHostException)
      }
    }

  private fun push(entityId: String): PushRequest =
    PushRequest(base, key, SyncEntityType.TICKET, entityId, SyncOp.UPSERT, "{}", "app:k:A", 1L)

  private class FakeConnectionFactory(
    private val code: Int,
    private val body: String,
    private val failure: IOException? = null,
  ) : ConnectionFactory {
    val opened = mutableListOf<FakeConnection>()
    val single: FakeConnection get() = opened.single()

    override fun open(url: URL): HttpURLConnection =
      FakeConnection(url, code, body, failure).also { opened += it }
  }

  private class FakeConnection(
    url: URL,
    private val code: Int,
    private val body: String,
    private val failure: IOException? = null,
  ) : HttpURLConnection(url) {
    val requestProps = mutableMapOf<String, String>()
    var postedBody: String = ""

    override fun connect() {
      connected = true
    }

    override fun disconnect() {}

    override fun usingProxy(): Boolean = false

    override fun setRequestProperty(key: String, value: String) {
      requestProps[key] = value
    }

    override fun getResponseCode(): Int {
      failure?.let { throw it }
      return code
    }

    override fun getInputStream(): InputStream {
      failure?.let { throw it }
      if (code !in 200..299) throw IOException("error stream")
      return ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }

    override fun getOutputStream(): OutputStream =
      object : ByteArrayOutputStream() {
        override fun close() {
          super.close()
          postedBody = toByteArray().toString(Charsets.UTF_8)
        }
      }
  }
}
