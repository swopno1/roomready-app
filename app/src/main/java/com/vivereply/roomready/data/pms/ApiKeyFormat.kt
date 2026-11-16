package com.vivereply.roomready.data.pms

/**
 * Property API key format: rk.<keyId>.<secret>.
 * The keyId routes display + actor strings; the secret never leaves the vault
 * except inside the auth header at send time.
 */
object ApiKeyFormat {
  const val PREFIX: String = "rk."

  data class ParsedKey(val keyId: String, val secret: String)

  /** Null unless the raw value matches rk.<keyId>.<secret> with solid parts. */
  fun parse(raw: String?): ParsedKey? {
    val trimmed = raw?.trim() ?: return null
    if (!trimmed.startsWith(PREFIX)) return null
    val rest = trimmed.removePrefix(PREFIX)
    val dot = rest.indexOf('.')
    if (dot <= 0) return null
    val keyId = rest.substring(0, dot)
    val secret = rest.substring(dot + 1)
    if (keyId.isBlank() || secret.isBlank()) return null
    if (keyId.any { it.isWhitespace() } || secret.any { it.isWhitespace() }) return null
    return ParsedKey(keyId, secret)
  }

  fun keyIdOf(raw: String?): String? = parse(raw)?.keyId
}
