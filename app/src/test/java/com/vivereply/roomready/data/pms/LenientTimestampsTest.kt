package com.vivereply.roomready.data.pms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LenientTimestampsTest {

  @Test
  fun `iso utc without millis parses`() {
    assertEquals(1_794_729_600_000L, LenientTimestamps.parse("2026-11-15T08:00:00Z"))
  }

  @Test
  fun `iso with millis and offset parses`() {
    assertEquals(1_794_729_600_500L, LenientTimestamps.parse("2026-11-15T08:00:00.500Z"))
    assertEquals(1_794_729_600_000L, LenientTimestamps.parse("2026-11-15T14:00:00+06:00"))
  }

  @Test
  fun `epoch millis and seconds parse`() {
    assertEquals(1_794_729_600_000L, LenientTimestamps.parse("1794729600000"))
    assertEquals(1_794_729_600_000L, LenientTimestamps.parse("1794729600"))
  }

  @Test
  fun `missing blank and garbage degrade to null`() {
    assertNull(LenientTimestamps.parse(null))
    assertNull(LenientTimestamps.parse(""))
    assertNull(LenientTimestamps.parse("   "))
    assertNull(LenientTimestamps.parse("yesterday"))
    assertNull(LenientTimestamps.parse("2026-13-45T99:99:99Z"))
    assertNull(LenientTimestamps.parse("15/11/2026"))
  }

  @Test
  fun `parseAny accepts json numbers and rejects objects`() {
    assertEquals(1_000L, LenientTimestamps.parseAny(1000L))
    assertEquals(1_000L, LenientTimestamps.parseAny(1000))
    assertEquals(1_794_729_600_000L, LenientTimestamps.parseAny("2026-11-15T08:00:00Z"))
    assertNull(LenientTimestamps.parseAny(null))
    assertNull(LenientTimestamps.parseAny(listOf(1)))
    assertNull(LenientTimestamps.parseAny(true))
  }
}
