package com.vivereply.roomready.data.pms

import androidx.annotation.StringRes
import com.vivereply.roomready.R
import java.net.URI
import java.net.URLDecoder

/** Pairing payload decoded from a RoomReady QR code. */
data class QrPairing(val baseUrl: PmsAddress, val apiKey: String)

sealed class QrError(@StringRes val messageRes: Int) {
  data object NotRoomReadyCode : QrError(R.string.qr_error_not_roomready)

  data object UnsupportedVersion : QrError(R.string.qr_error_version)

  data class MissingField(val field: String) : QrError(R.string.qr_error_missing_field)

  data object BadUrl : QrError(R.string.qr_error_bad_url)

  data object BadKey : QrError(R.string.qr_error_bad_key)
}

class QrException(val error: QrError) : IllegalArgumentException(error.toString())

/**
 * Parses roomready://connect?v=1&url=<url-encoded origin>&key=<full key>.
 * Any other shape is rejected with a typed error. Pure java.net parsing so
 * it is unit-testable on the JVM.
 */
object QrPairingParser {
  private const val SCHEME = "roomready"
  private const val HOST = "connect"
  private const val VERSION = "1"

  fun parse(raw: String?): Result<QrPairing> {
    val trimmed = raw?.trim()
    if (trimmed.isNullOrEmpty()) return failure(QrError.NotRoomReadyCode)
    val uri =
      try {
        URI(trimmed)
      } catch (e: Exception) {
        return failure(QrError.NotRoomReadyCode)
      }
    if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return failure(QrError.NotRoomReadyCode)
    if (!uri.host.equals(HOST, ignoreCase = true)) return failure(QrError.NotRoomReadyCode)

    val params = queryParams(uri.rawQuery ?: return failure(QrError.MissingField("v")))
    val version = params["v"] ?: return failure(QrError.MissingField("v"))
    if (version != VERSION) return failure(QrError.UnsupportedVersion)
    val url = params["url"] ?: return failure(QrError.MissingField("url"))
    val key = params["key"] ?: return failure(QrError.MissingField("key"))

    val address =
      PmsAddressValidator.validate(url).getOrElse { return failure(QrError.BadUrl) }
    if (ApiKeyFormat.parse(key) == null) return failure(QrError.BadKey)
    return Result.success(QrPairing(address, key.trim()))
  }

  private fun queryParams(rawQuery: String): Map<String, String> {
    if (rawQuery.isEmpty()) return emptyMap()
    val out = mutableMapOf<String, String>()
    for (pair in rawQuery.split('&')) {
      val cut = pair.indexOf('=')
      if (cut <= 0) continue
      val name = pair.substring(0, cut)
      if (name.isEmpty() || out.containsKey(name)) continue
      out[name] =
        try {
          URLDecoder.decode(pair.substring(cut + 1), Charsets.UTF_8.name())
        } catch (e: Exception) {
          pair.substring(cut + 1)
        }
    }
    return out
  }

  private fun failure(error: QrError): Result<QrPairing> = Result.failure(QrException(error))
}
