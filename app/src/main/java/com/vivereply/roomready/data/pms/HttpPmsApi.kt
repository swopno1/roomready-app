package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncPayloads
import com.vivereply.roomready.domain.model.TicketStatus
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** Opens HTTP connections; faked in tests. */
interface ConnectionFactory {
  fun open(url: URL): HttpURLConnection
}

object RealConnectionFactory : ConnectionFactory {
  override fun open(url: URL): HttpURLConnection = url.openConnection() as HttpURLConnection
}

/**
 * [PmsApi] over HttpURLConnection (no third-party HTTP stack: lean on 2 GB
 * devices). Auth header only; the key never appears in messages or bodies.
 * No request carries an actor field: the PMS builds app:<keyId>:<staffName>
 * itself from the key and staffName.
 */
class HttpPmsApi(private val connections: ConnectionFactory = RealConnectionFactory) : PmsApi {

  override suspend fun fetchBootstrap(baseUrl: PmsAddress, apiKey: String): String {
    val connection = open(PmsContract.bootstrapUrl(baseUrl.origin))
    try {
      connection.requestMethod = "GET"
      connection.setRequestProperty("Authorization", PmsContract.authHeaderValue(apiKey))
      connection.setRequestProperty("Accept", "application/json")
      val code = connection.responseCode
      if (code == HttpURLConnection.HTTP_OK) {
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
      }
      throw PmsApiException("GET bootstrap failed", code)
    } catch (e: PmsApiException) {
      throw e
    } catch (e: IOException) {
      throw PmsApiException("GET bootstrap failed", null, e)
    } finally {
      connection.disconnect()
    }
  }

  override suspend fun postRoomStatus(
    baseUrl: PmsAddress,
    apiKey: String,
    unitId: String,
    body: StatusBody,
  ) {
    sendJson(
      method = "POST",
      url = PmsContract.roomStatusUrl(baseUrl.origin, unitId),
      apiKey = apiKey,
      body = statusBodyJson(body),
      label = "POST room status",
    )
  }

  override suspend fun createTicket(
    baseUrl: PmsAddress,
    apiKey: String,
    body: TicketCreateBody,
  ): String {
    val response =
      sendJson(
        method = "POST",
        url = PmsContract.ticketsUrl(baseUrl.origin),
        apiKey = apiKey,
        body = ticketCreateJson(body),
        label = "POST tickets",
        readBody = true,
      )
    val id =
      try {
        JSONObject(response).optString("id", "").trim()
      } catch (e: Exception) {
        ""
      }
    if (id.isEmpty()) throw PmsApiException("ticket create missing id", HttpURLConnection.HTTP_CREATED)
    return id
  }

  override suspend fun updateTicket(
    baseUrl: PmsAddress,
    apiKey: String,
    serverId: String,
    body: TicketUpdateBody,
  ) {
    sendJson(
      method = "PATCH",
      url = PmsContract.ticketUrl(baseUrl.origin, serverId),
      apiKey = apiKey,
      body = ticketUpdateJson(body),
      label = "PATCH ticket",
    )
  }

  override suspend fun fetchTickets(
    baseUrl: PmsAddress,
    apiKey: String,
    status: TicketStatus,
  ): String {
    val connection = open(PmsContract.ticketsQueryUrl(baseUrl.origin, status))
    try {
      connection.requestMethod = "GET"
      connection.setRequestProperty("Authorization", PmsContract.authHeaderValue(apiKey))
      connection.setRequestProperty("Accept", "application/json")
      val code = connection.responseCode
      if (code == HttpURLConnection.HTTP_OK) {
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
      }
      throw PmsApiException("GET tickets failed", code)
    } catch (e: PmsApiException) {
      throw e
    } catch (e: IOException) {
      throw PmsApiException("GET tickets failed", null, e)
    } finally {
      connection.disconnect()
    }
  }

  /**
   * Sends a JSON write. Any 2xx delivers; anything else (or a transport
   * failure) throws with the status preserved for the error matrix.
   */
  private fun sendJson(
    method: String,
    url: String,
    apiKey: String,
    body: String,
    label: String,
    readBody: Boolean = false,
  ): String {
    val connection = open(url)
    try {
      connection.requestMethod = method
      connection.doOutput = true
      connection.setRequestProperty("Authorization", PmsContract.authHeaderValue(apiKey))
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      connection.setRequestProperty("Accept", "application/json")
      connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = connection.responseCode
      if (code !in 200..299) throw PmsApiException("$label failed", code)
      return if (readBody) {
        connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
      } else {
        ""
      }
    } catch (e: PmsApiException) {
      throw e
    } catch (e: IOException) {
      throw PmsApiException("$label failed", null, e)
    } finally {
      connection.disconnect()
    }
  }

  private fun open(url: String): HttpURLConnection {
    val connection = connections.open(URL(url))
    connection.connectTimeout = PmsContract.CONNECT_TIMEOUT_MILLIS
    connection.readTimeout = PmsContract.READ_TIMEOUT_MILLIS
    return connection
  }

  private fun statusBodyJson(body: StatusBody): String {
    val fields =
      mutableListOf(
        "\"expected\":\"${body.expected.code}\"",
        "\"action\":\"${body.action.wire}\"",
        "\"staffName\":\"${SyncPayloads.escape(body.staffName)}\"",
      )
    if (!body.note.isNullOrBlank()) fields += "\"note\":\"${SyncPayloads.escape(body.note)}\""
    return "{${fields.joinToString(",")}}"
  }

  private fun ticketCreateJson(body: TicketCreateBody): String {
    val fields =
      mutableListOf(
        "\"unitId\":\"${SyncPayloads.escape(body.unitId)}\"",
        "\"title\":\"${SyncPayloads.escape(body.title)}\"",
      )
    if (!body.description.isNullOrBlank()) {
      fields += "\"description\":\"${SyncPayloads.escape(body.description)}\""
    }
    fields += "\"priority\":\"${SyncPayloads.escape(body.priority)}\""
    fields += "\"staffName\":\"${SyncPayloads.escape(body.staffName)}\""
    return "{${fields.joinToString(",")}}"
  }

  private fun ticketUpdateJson(body: TicketUpdateBody): String =
    "{\"status\":\"${body.status.name}\"," +
      "\"staffName\":\"${SyncPayloads.escape(body.staffName)}\"}"
}
