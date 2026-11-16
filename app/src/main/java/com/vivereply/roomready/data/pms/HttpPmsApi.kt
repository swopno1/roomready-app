package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncPayloads
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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

  override suspend fun push(request: PushRequest) {
    val connection = open(PmsContract.outboundUrl(request.baseUrl.origin))
    try {
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Authorization", PmsContract.authHeaderValue(request.apiKey))
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      connection.setRequestProperty("Accept", "application/json")
      val body = pushBody(request)
      connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = connection.responseCode
      if (code !in 200..299) throw PmsApiException("POST outbound failed", code)
    } catch (e: PmsApiException) {
      throw e
    } catch (e: IOException) {
      throw PmsApiException("POST outbound failed", null, e)
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

  private fun pushBody(request: PushRequest): String =
    "{\"entityType\":\"${request.entityType}\"," +
      "\"entityId\":\"${SyncPayloads.escape(request.entityId)}\"," +
      "\"op\":\"${request.op}\"," +
      "\"payload\":${request.payloadJson}," +
      "\"actor\":\"${SyncPayloads.escape(request.actor)}\"," +
      "\"at\":${request.atEpochMillis}}"
}
