package com.vivereply.roomready.data.pms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PmsAddressValidatorTest {

  @Test
  fun `https origin normalizes host case and trailing slash`() {
    val address = PmsAddressValidator.validate("https://PMS.Demo13.com/").getOrThrow()
    assertEquals("https://pms.demo13.com", address.origin)
  }

  @Test
  fun `port and base path are kept`() {
    val address = PmsAddressValidator.validate("https://pms.example.com:8443/resort/").getOrThrow()
    assertEquals("https://pms.example.com:8443/resort", address.origin)
  }

  @Test
  fun `query and fragment are stripped`() {
    val address = PmsAddressValidator.validate("https://pms.example.com/?x=1#y").getOrThrow()
    assertEquals("https://pms.example.com", address.origin)
  }

  @Test
  fun `http is rejected without the loopback flag`() {
    val result = PmsAddressValidator.validate("http://pms.example.com")
    assertTrue(result.isFailure)
    assertEquals(AddressError.NotHttps, (result.exceptionOrNull() as AddressException).error)
  }

  @Test
  fun `http loopback is allowed only with the test flag`() {
    assertTrue(
      PmsAddressValidator.validate("http://127.0.0.1:8080", allowInsecureLoopback = true).isSuccess,
    )
    assertTrue(
      PmsAddressValidator.validate("http://localhost:8080", allowInsecureLoopback = true).isSuccess,
    )
    assertTrue(
      PmsAddressValidator.validate("http://127.0.0.1:8080", allowInsecureLoopback = false).isFailure,
    )
    // Non-loopback http stays rejected even with the flag.
    assertTrue(
      PmsAddressValidator.validate("http://pms.example.com", allowInsecureLoopback = true).isFailure,
    )
  }

  @Test
  fun `blank and malformed addresses fail typed`() {
    val blank = PmsAddressValidator.validate("   ")
    assertEquals(AddressError.Blank, (blank.exceptionOrNull() as AddressException).error)

    listOf("pms.example.com", "https://", "https://foo bar.com", "ftp://pms.example.com/x", "https://user@host.com")
      .forEach { raw ->
        val result = PmsAddressValidator.validate(raw)
        assertTrue("must fail: $raw", result.isFailure)
      }
  }
}
