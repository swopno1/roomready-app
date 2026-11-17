package com.vivereply.roomready.ui.state

import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarizeTodayTest {

  @Test
  fun `today counts cleanings and tickets and sums synced deliveries`() {
    val now = 1_794_729_600_000L
    val start = startOfLocalDay(now)
    val events =
      listOf(
        event(1L, ActivityType.CHECKLIST_COMPLETED, start + 1_000L),
        event(2L, ActivityType.CHECKLIST_COMPLETED, start + 2_000L),
        event(3L, ActivityType.TICKET_RAISED, start + 3_000L),
        event(4L, ActivityType.SYNC_COMPLETED, start + 4_000L, detail = "5"),
        event(5L, ActivityType.SYNC_COMPLETED, start + 5_000L, detail = "2"),
        event(6L, ActivityType.STATUS_CHANGED, start + 6_000L),
      )
    assertEquals(TodayStats(cleaned = 2, tickets = 1, synced = 7), summarizeToday(events, now))
  }

  @Test
  fun `events before today are excluded`() {
    val now = 1_794_729_600_000L
    val start = startOfLocalDay(now)
    val events =
      listOf(
        event(1L, ActivityType.CHECKLIST_COMPLETED, start - 1L),
        event(2L, ActivityType.TICKET_RAISED, start - 3_600_000L),
      )
    assertEquals(TodayStats(cleaned = 0, tickets = 0, synced = 0), summarizeToday(events, now))
  }

  @Test
  fun `an event exactly at midnight counts as today`() {
    val now = 1_794_729_600_000L
    val start = startOfLocalDay(now)
    val events = listOf(event(1L, ActivityType.CHECKLIST_COMPLETED, start))
    assertEquals(1, summarizeToday(events, now).cleaned)
  }

  @Test
  fun `non-numeric sync details count as zero`() {
    val now = 1_794_729_600_000L
    val start = startOfLocalDay(now)
    val events =
      listOf(
        event(1L, ActivityType.SYNC_COMPLETED, start + 1_000L, detail = null),
        event(2L, ActivityType.SYNC_COMPLETED, start + 2_000L, detail = "oops"),
      )
    assertEquals(0, summarizeToday(events, now).synced)
  }

  private fun event(id: Long, type: ActivityType, at: Long, detail: String? = null): ActivityEvent =
    ActivityEvent(
      id = id,
      type = type,
      actorName = "Ayesha",
      role = null,
      unitId = null,
      roomNumber = null,
      detail = detail,
      createdAtEpochMillis = at,
    )
}
