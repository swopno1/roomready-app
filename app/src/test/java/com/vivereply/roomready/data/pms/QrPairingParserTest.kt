package com.vivereply.roomready.data.pms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPairingParserTest {

  private val valid =
    "roomready://connect?v=1&url=https%3A%2F%2Fpms.demo13.com&key=rk.k597.q8w-secret"

  @Test
  fun `valid code fills address and key`() {
    val pairing = QrPairingParser.parse(valid).getOrThrow()
    assertEquals("https://pms.demo13.com", pairing.baseUrl.origin)
    assertEquals("rk.k597.q8w-secret", pairing.apiKey)
  }

  @Test
  fun `non-roomready shapes are rejected`() {
    listOf(
      null,
      "",
      "   ",
      "https://pms.demo13.com",
      "roomready://pair?v=1&url=https%3A%2F%2Fa.com&key=rk.k.s",
      "other://connect?v=1&url=https%3A%2F%2Fa.com&key=rk.k.s",
      "not a uri at all ::::",
    ).forEach { raw ->
      val error = QrPairingParser.parse(raw).exceptionOrNull() as QrException
      assertEquals("for <$raw>", QrError.NotRoomReadyCode, error.error)
    }
  }

  @Test
  fun `wrong version is rejected`() {
    val error =
      QrPairingParser.parse("roomready://connect?v=2&url=https%3A%2F%2Fa.com&key=rk.k.s")
        .exceptionOrNull() as QrException
    assertEquals(QrError.UnsupportedVersion, error.error)
  }

  @Test
  fun `missing fields name the field`() {
    val noKey =
      QrPairingParser.parse("roomready://connect?v=1&url=https%3A%2F%2Fa.com")
        .exceptionOrNull() as QrException
    assertEquals(QrError.MissingField("key"), noKey.error)

    val noUrl =
      QrPairingParser.parse("roomready://connect?v=1&key=rk.k.s")
        .exceptionOrNull() as QrException
    assertEquals(QrError.MissingField("url"), noUrl.error)

    val noVersion =
      QrPairingParser.parse("roomready://connect?url=https%3A%2F%2Fa.com&key=rk.k.s")
        .exceptionOrNull() as QrException
    assertEquals(QrError.MissingField("v"), noVersion.error)
  }

  @Test
  fun `bad url and bad key inside the code are rejected distinctly`() {
    val badUrl =
      QrPairingParser.parse("roomready://connect?v=1&url=http%3A%2F%2Fa.com&key=rk.k.s")
        .exceptionOrNull() as QrException
    assertEquals(QrError.BadUrl, badUrl.error)

    val badKey =
      QrPairingParser.parse("roomready://connect?v=1&url=https%3A%2F%2Fa.com&key=nope")
        .exceptionOrNull() as QrException
    assertEquals(QrError.BadKey, badKey.error)
  }

  @Test
  fun `first of duplicate params wins`() {
    val pairing =
      QrPairingParser.parse(
        "roomready://connect?v=1&url=https%3A%2F%2Fa.com&url=https%3A%2F%2Fb.com&key=rk.k.s",
      ).getOrThrow()
    assertEquals("https://a.com", pairing.baseUrl.origin)
  }
}
