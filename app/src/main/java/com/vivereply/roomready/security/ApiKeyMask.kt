package com.vivereply.roomready.security

/**
 * Display masking for the property API key.
 * On screen the key is always masked except the last 4 characters.
 */
object ApiKeyMask {
  const val BULLETS: String = "••••"
  const val VISIBLE_TAIL: Int = 4

  /** Last 4 characters, or "" when there is no key. */
  fun last4(apiKey: String?): String {
    if (apiKey.isNullOrEmpty()) return ""
    return apiKey.takeLast(VISIBLE_TAIL)
  }

  /** Masked form for display, e.g. "••••9f2a". Empty when there is no key. */
  fun masked(apiKey: String?): String {
    if (apiKey.isNullOrEmpty()) return ""
    return BULLETS + apiKey.takeLast(VISIBLE_TAIL)
  }
}
