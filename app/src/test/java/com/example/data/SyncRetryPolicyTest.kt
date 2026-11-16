package com.example.data

import com.example.data.sync.SyncRetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRetryPolicyTest {

  @Test
  fun `backoff doubles from 30 seconds`() {
    assertEquals(30_000L, SyncRetryPolicy.delayForAttempt(1))
    assertEquals(60_000L, SyncRetryPolicy.delayForAttempt(2))
    assertEquals(120_000L, SyncRetryPolicy.delayForAttempt(3))
    assertEquals(960_000L, SyncRetryPolicy.delayForAttempt(6))
  }

  @Test
  fun `backoff caps at one hour`() {
    assertEquals(3_600_000L, SyncRetryPolicy.delayForAttempt(10))
    assertEquals(3_600_000L, SyncRetryPolicy.delayForAttempt(50))
  }

  @Test
  fun `retries stop after max attempts`() {
    assertTrue(SyncRetryPolicy.shouldRetry(0))
    assertTrue(SyncRetryPolicy.shouldRetry(9))
    assertFalse(SyncRetryPolicy.shouldRetry(10))
    assertFalse(SyncRetryPolicy.shouldRetry(11))
  }

  @Test
  fun `next due date is now plus backoff`() {
    assertEquals(1_060_000L, SyncRetryPolicy.nextDueAt(1_000_000L, 2))
  }

  @Test
  fun `queue order is earliest due first then fifo by id`() {
    data class Row(val id: Long, val dueAt: Long)

    val rows =
      listOf(
        Row(id = 3, dueAt = 2000),
        Row(id = 1, dueAt = 1000),
        Row(id = 2, dueAt = 1000),
        Row(id = 4, dueAt = 3000),
      )
    val ordered = SyncRetryPolicy.orderByDueAt(rows, { it.dueAt }, { it.id })
    assertEquals(listOf(1L, 2L, 3L, 4L), ordered.map { it.id })
  }

  @Test
  fun `overdue retry sorts ahead of newer pending entries`() {
    data class Row(val id: Long, val dueAt: Long)

    val rows =
      listOf(
        Row(id = 10, dueAt = 5000),
        Row(id = 2, dueAt = 1000),
      )
    val ordered = SyncRetryPolicy.orderByDueAt(rows, { it.dueAt }, { it.id })
    assertEquals(listOf(2L, 10L), ordered.map { it.id })
  }
}
