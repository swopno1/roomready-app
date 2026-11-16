package com.vivereply.roomready.data.pms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiKeyFormatTest {

  @Test
  fun `valid key splits into id and secret`() {
    val parsed = ApiKeyFormat.parse("rk.k597.q8w-secret-77")
    assertEquals("k597", parsed?.keyId)
    assertEquals("q8w-secret-77", parsed?.secret)
    assertEquals("k597", ApiKeyFormat.keyIdOf("rk.k597.q8w-secret-77"))
  }

  @Test
  fun `secret may contain dots`() {
    assertEquals("a.b.c", ApiKeyFormat.parse("rk.k1.a.b.c")?.secret)
  }

  @Test
  fun `surrounding whitespace is tolerated`() {
    assertEquals("k1", ApiKeyFormat.keyIdOf("  rk.k1.s3  \n"))
  }

  @Test
  fun `rejects blank missing and malformed keys`() {
    listOf(null, "", "   ", "k1.s3", "rk.", "rk.k1", "rk..s3", "rk. .s3", "rk.k1. ", "xk.k1.s3", "rk.k 1.s3")
      .forEach { assertNull("must reject <$it>", ApiKeyFormat.parse(it)) }
  }
}
