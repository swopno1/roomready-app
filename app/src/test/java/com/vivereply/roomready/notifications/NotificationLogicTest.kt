package com.vivereply.roomready.notifications

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationLogicTest {

  @Test
  fun `morning summary stays silent when every room is ready`() {
    assertNull(MorningSummary.build(0, 0, "T", { "d$it" }, { "w$it" }))
    assertNull(MorningSummary.build(-1, 0, "T", { "d$it" }, { "w$it" }))
    assertNull(MorningSummary.build(0, -2, "T", { "d$it" }, { "w$it" }))
  }

  @Test
  fun `morning summary shows only the dirty line when nothing waits`() {
    val payload = MorningSummary.build(5, 0, "Today", { "dirty:$it" }, { "waiting:$it" })!!
    assertEquals("Today", payload.title)
    assertEquals("dirty:5", payload.body)
  }

  @Test
  fun `morning summary combines dirty and waiting lines`() {
    val payload = MorningSummary.build(5, 2, "Today", { "dirty:$it" }, { "waiting:$it" })!!
    assertEquals("dirty:5\nwaiting:2", payload.body)
  }

  @Test
  fun `inspection nudge stays silent when nothing waits`() {
    assertNull(WaitingInspection.build(0, "T") { "b$it" })
    assertNull(WaitingInspection.build(-1, "T") { "b$it" })
  }

  @Test
  fun `inspection nudge resolves the body with the exact count`() {
    val payload = WaitingInspection.build(3, "Wait", { "n=$it" })!!
    assertEquals("Wait", payload.title)
    assertEquals("n=3", payload.body)
  }

  @Test
  fun `next occurrence is later today when the hour has not passed`() {
    val now = localMillis(2026, Calendar.NOVEMBER, 17, 5, 0)
    val expected = localMillis(2026, Calendar.NOVEMBER, 17, 6, 0)
    assertEquals(expected, AlarmScheduler.nextOccurrence(6, 0, now))
  }

  @Test
  fun `next occurrence rolls to tomorrow once the hour has passed`() {
    val now = localMillis(2026, Calendar.NOVEMBER, 17, 10, 30)
    val expected = localMillis(2026, Calendar.NOVEMBER, 18, 6, 0)
    assertEquals(expected, AlarmScheduler.nextOccurrence(6, 0, now))
  }

  @Test
  fun `next occurrence at the exact minute waits a full day`() {
    val now = localMillis(2026, Calendar.NOVEMBER, 17, 6, 0)
    val expected = localMillis(2026, Calendar.NOVEMBER, 18, 6, 0)
    assertEquals(expected, AlarmScheduler.nextOccurrence(6, 0, now))
  }

  @Test
  fun `next occurrence honours the minute`() {
    val now = localMillis(2026, Calendar.NOVEMBER, 17, 6, 15)
    val expected = localMillis(2026, Calendar.NOVEMBER, 17, 6, 30)
    assertEquals(expected, AlarmScheduler.nextOccurrence(6, 30, now))
  }

  @Test
  fun `next occurrence is always strictly in the future`() {
    val base = localMillis(2026, Calendar.NOVEMBER, 17, 0, 0)
    for (hour in 0..23) {
      val now = base + hour * 3_600_000L + 1234L
      assertTrue(AlarmScheduler.nextOccurrence(6, 0, now) > now)
      assertTrue(AlarmScheduler.nextOccurrence(10, 0, now) > now)
    }
  }

  private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
