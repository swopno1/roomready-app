package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiKeyMaskTest {

  @Test
  fun `masked key shows only the last four characters`() {
    assertEquals("••••9f2a", ApiKeyMask.masked("property-key-9f2a"))
  }

  @Test
  fun `last4 returns the tail of the key`() {
    assertEquals("9f2a", ApiKeyMask.last4("property-key-9f2a"))
  }

  @Test
  fun `missing key masks to empty`() {
    assertEquals("", ApiKeyMask.masked(null))
    assertEquals("", ApiKeyMask.masked(""))
    assertEquals("", ApiKeyMask.last4(null))
    assertEquals("", ApiKeyMask.last4(""))
  }

  @Test
  fun `short key never reveals more than it holds`() {
    assertEquals("••••abc", ApiKeyMask.masked("abc"))
    assertEquals("abc", ApiKeyMask.last4("abc"))
  }

  @Test
  fun `masked output never contains key head`() {
    val key = "sk-live-0123456789abcdef"
    val masked = ApiKeyMask.masked(key)
    assertEquals(false, masked.contains("sk-live"))
    assertEquals(true, masked.endsWith("cdef"))
  }
}
