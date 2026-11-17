package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.TicketStatus
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Wire-shape tests over fake connections (no network). Robolectric: 201 bodies parse as JSON. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
  fun `status post sends the exact body with note`() =
    runTest {
      val factory = FakeConnectionFactory(200, "")
      HttpPmsApi(factory).postRoomStatus(
        base,
        key,
        "u-101",
        StatusBody(PmsHousekeeping.DIRTY, StatusAction.MARK_CLEAN, "Ayesha", "too dark"),
      )
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/rooms/u-101/status", sent.url.toString())
      assertEquals("POST", sent.requestMethod)
      assertEquals("Bearer rk.k597.secret-77", sent.requestProps["Authorization"])
      assertEquals(
        "{\"expected\":\"DIRTY\",\"action\":\"MARK_CLEAN\",\"staffName\":\"Ayesha\",\"note\":\"too dark\"}",
        sent.postedBody,
      )
    }

  @Test
  fun `status post omits blank note and carries no actor`() =
    runTest {
      val factory = FakeConnectionFactory(200, "")
      HttpPmsApi(factory).postRoomStatus(
        base,
        key,
        "u-101",
        StatusBody(PmsHousekeeping.CLEAN, StatusAction.INSPECT_PASS, "Rahim", "  "),
      )
      assertEquals(
        "{\"expected\":\"CLEAN\",\"action\":\"INSPECT_PASS\",\"staffName\":\"Rahim\"}",
        factory.single.postedBody,
      )
      assertFalse(factory.single.postedBody.contains("actor"))
    }

  @Test
  fun `status post covers fail and redirty shapes`() =
    runTest {
      val failFactory = FakeConnectionFactory(200, "")
      HttpPmsApi(failFactory).postRoomStatus(
        base,
        key,
        "u-102",
        StatusBody(PmsHousekeeping.CLEAN, StatusAction.INSPECT_FAIL, "Rahim", "dust"),
      )
      assertEquals(
        "{\"expected\":\"CLEAN\",\"action\":\"INSPECT_FAIL\",\"staffName\":\"Rahim\",\"note\":\"dust\"}",
        failFactory.single.postedBody,
      )

      val dirtyFactory = FakeConnectionFactory(200, "")
      HttpPmsApi(dirtyFactory).postRoomStatus(
        base,
        key,
        "u-103",
        StatusBody(PmsHousekeeping.INSPECTED, StatusAction.MARK_DIRTY, "Rahim", null),
      )
      assertEquals(
        "{\"expected\":\"INSPECTED\",\"action\":\"MARK_DIRTY\",\"staffName\":\"Rahim\"}",
        dirtyFactory.single.postedBody,
      )
    }

  @Test
  fun `ticket create posts exact body and returns the server id`() =
    runTest {
      val factory = FakeConnectionFactory(201, "{\"id\":\"srv-9\",\"status\":\"OPEN\"}")
      val serverId =
        HttpPmsApi(factory).createTicket(
          base,
          key,
          TicketCreateBody("u-101", "AC", "no cooling", "NORMAL", "Ayesha"),
        )
      assertEquals("srv-9", serverId)
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/tickets", sent.url.toString())
      assertEquals("POST", sent.requestMethod)
      assertEquals(
        "{\"unitId\":\"u-101\",\"title\":\"AC\",\"description\":\"no cooling\"," +
          "\"priority\":\"NORMAL\",\"staffName\":\"Ayesha\"}",
        sent.postedBody,
      )
    }

  @Test
  fun `ticket create omits blank description`() =
    runTest {
      val factory = FakeConnectionFactory(201, "{\"id\":\"srv-1\"}")
      HttpPmsApi(factory).createTicket(
        base,
        key,
        TicketCreateBody("u-101", "Other", "  ", "NORMAL", "Ayesha"),
      )
      assertEquals(
        "{\"unitId\":\"u-101\",\"title\":\"Other\",\"priority\":\"NORMAL\",\"staffName\":\"Ayesha\"}",
        factory.single.postedBody,
      )
    }

  @Test
  fun `ticket create without an id fails loudly`() =
    runTest {
      val api = HttpPmsApi(FakeConnectionFactory(201, "{\"status\":\"OPEN\"}"))
      try {
        api.createTicket(base, key, TicketCreateBody("u-101", "AC", null, "NORMAL", "Ayesha"))
        throw AssertionError("expected PmsApiException")
      } catch (e: PmsApiException) {
        assertEquals(201, e.statusCode)
      }
    }

  @Test
  fun `ticket update patches the server id with exact body`() =
    runTest {
      val factory = FakeConnectionFactory(200, "")
      HttpPmsApi(factory).updateTicket(base, key, "srv-9", TicketUpdateBody(TicketStatus.DONE, "Rahim"))
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/tickets/srv-9", sent.url.toString())
      assertEquals("PATCH", sent.requestMethod)
      assertEquals("{\"status\":\"DONE\",\"staffName\":\"Rahim\"}", sent.postedBody)
    }

  @Test
  fun `ticket list fetch gets each status array`() =
    runTest {
      val factory = FakeConnectionFactory(200, "[{\"id\":\"srv-9\"}]")
      val body = HttpPmsApi(factory).fetchTickets(base, key, TicketStatus.OPEN)
      assertEquals("[{\"id\":\"srv-9\"}]", body)
      val sent = factory.single
      assertEquals("https://pms.demo13.com/api/housekeeping/tickets?status=OPEN", sent.url.toString())
      assertEquals("GET", sent.requestMethod)
    }

  @Test
  fun `write failures split parkable from retryable`() =
    runTest {
      val apiFor: (Int) -> HttpPmsApi = { code ->
        HttpPmsApi(FakeConnectionFactory(code, ""))
      }
      suspend fun statusOf(code: Int): PmsApiException =
        try {
          apiFor(code).postRoomStatus(
            base,
            key,
            "u-1",
            StatusBody(PmsHousekeeping.DIRTY, StatusAction.MARK_CLEAN, "A", null),
          )
          throw AssertionError("expected failure for $code")
        } catch (e: PmsApiException) {
          e
        }

      assertTrue(statusOf(400).parkable())
      assertTrue(statusOf(404).parkable())
      assertTrue(statusOf(409).parkable())
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
    private var recordedMethod: String? = null

    override fun connect() {
      connected = true
    }

    override fun disconnect() {}

    override fun usingProxy(): Boolean = false

    // The JDK base rejects PATCH; real Android (OkHttp-backed) accepts it.
    override fun setRequestMethod(method: String) {
      recordedMethod = method
    }

    override fun getRequestMethod(): String = recordedMethod ?: super.getRequestMethod()

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
