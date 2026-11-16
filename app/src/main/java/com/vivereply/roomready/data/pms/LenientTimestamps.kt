package com.vivereply.roomready.data.pms

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Lenient PMS timestamp parsing. Anything missing or unparseable degrades to
 * null and must NEVER fail a sync; callers log a warning for present-but-bad
 * values. Accepts ISO-8601 UTC (with/without millis, Z or offset) and epoch
 * millis (or seconds for short digit strings).
 */
object LenientTimestamps {
  private const val SECONDS_THRESHOLD_LENGTH = 10

  private val isoPatterns =
    listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")

  fun parse(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.all { it.isDigit() }) {
      val value = trimmed.toLongOrNull() ?: return null
      return if (trimmed.length <= SECONDS_THRESHOLD_LENGTH) value * 1000L else value
    }
    for (pattern in isoPatterns) {
      val parsed = tryPattern(pattern, trimmed)
      if (parsed != null) return parsed
    }
    return null
  }

  /** Accepts decoded JSON values (Number/String/null) as well as strings. */
  fun parseAny(value: Any?): Long? =
    when (value) {
      null -> null
      is Number -> value.toLong()
      is String -> parse(value)
      else -> null
    }

  private fun tryPattern(pattern: String, value: String): Long? =
    try {
      SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
      }.parse(value)?.time
    } catch (e: Exception) {
      null
    }
}
