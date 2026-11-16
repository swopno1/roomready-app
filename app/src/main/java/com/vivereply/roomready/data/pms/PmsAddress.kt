package com.vivereply.roomready.data.pms

import java.net.URI

/** Normalized PMS base address: scheme://host[:port][/basePath], no trailing slash. */
@JvmInline
value class PmsAddress(val origin: String)

sealed interface AddressError {
  data object Blank : AddressError

  data object NotHttps : AddressError

  data object Malformed : AddressError
}

class AddressException(val error: AddressError, detail: String? = null) :
  IllegalArgumentException(detail ?: error.toString())

/**
 * Validates a user-entered PMS address. Only https is accepted, except for
 * loopback hosts when [allowInsecureLoopback] is true (local test servers;
 * the shipped setup flow passes false). An optional base path is kept.
 */
object PmsAddressValidator {

  fun validate(raw: String?, allowInsecureLoopback: Boolean = false): Result<PmsAddress> {
    val trimmed = raw?.trim() ?: ""
    if (trimmed.isEmpty()) return failure(AddressError.Blank)
    if (trimmed.any { it.isWhitespace() }) return failure(AddressError.Malformed)

    val uri =
      try {
        URI(trimmed)
      } catch (e: Exception) {
        return failure(AddressError.Malformed, e.message)
      }
    val scheme = uri.scheme?.lowercase() ?: return failure(AddressError.Malformed)
    val host = uri.host ?: return failure(AddressError.Malformed)
    if (host.isBlank()) return failure(AddressError.Malformed)
    if (uri.userInfo != null) return failure(AddressError.Malformed, "credentials in URL")

    val isLoopback = host.equals("localhost", ignoreCase = true) || host == "127.0.0.1"
    val secure = scheme == "https"
    val insecureLoopback = allowInsecureLoopback && scheme == "http" && isLoopback
    if (!secure && !insecureLoopback) return failure(AddressError.NotHttps)

    val port = if (uri.port == -1) "" else ":${uri.port}"
    val path = uri.path?.trimEnd('/') ?: ""
    return Result.success(PmsAddress("$scheme://${host.lowercase()}$port$path"))
  }

  private fun failure(error: AddressError, detail: String? = null): Result<PmsAddress> =
    Result.failure(AddressException(error, detail))
}
