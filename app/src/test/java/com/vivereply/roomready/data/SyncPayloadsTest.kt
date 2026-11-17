package com.vivereply.roomready.data

import com.vivereply.roomready.data.sync.SyncPayloads
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadsTest {

  @Test
  fun `unit status payload carries pms code and local state`() {
    val payload = SyncPayloads.unitStatus("u-1", HousekeepingState.CLEANING, "Ayesha", 1000L)
    assertTrue(payload.contains("\"unitId\":\"u-1\""))
    assertTrue(payload.contains("\"housekeeping\":\"DIRTY\""))
    assertTrue(payload.contains("\"localState\":\"CLEANING\""))
    assertTrue(payload.contains("\"actor\":\"Ayesha\""))
    assertTrue(payload.contains("\"at\":1000"))
  }

  @Test
  fun `unit status payload carries finish evidence when present`() {
    val withEvidence =
      SyncPayloads.unitStatus("u-1", HousekeepingState.CLEANED, "Ayesha", 1000L, "f.jpg", null)
    assertTrue(withEvidence.contains("\"photoRef\":\"f.jpg\""))
    assertTrue(!withEvidence.contains("skipReason"))

    val withReason =
      SyncPayloads.unitStatus("u-1", HousekeepingState.CLEANED, "Ayesha", 1000L, null, "too dark")
    assertTrue(withReason.contains("\"skipReason\":\"too dark\""))
    assertTrue(!withReason.contains("photoRef"))

    val plain = SyncPayloads.unitStatus("u-1", HousekeepingState.DIRTY, "Ayesha", 1000L)
    assertTrue(!plain.contains("photoRef"))
    assertTrue(!plain.contains("skipReason"))
  }

  @Test
  fun `checklist payload carries ordered done keys and complete flag`() {
    val payload =
      SyncPayloads.checklist("u-1", listOf("bed_linen", "vacuum_mop"), false, "Ayesha", 1000L)
    assertTrue(payload.contains("\"done\":[\"bed_linen\",\"vacuum_mop\"]"))
    assertTrue(payload.contains("\"complete\":false"))
  }

  @Test
  fun `ticket payload carries category note and photo ref`() {
    val ticket =
      Ticket(
        id = "t-1",
        unitId = "u-1",
        roomNumber = "101",
        category = TicketCategory.PLUMBING,
        note = "Tap leaks",
        photoRef = "photo-1.jpg",
        status = TicketStatus.OPEN,
        createdBy = "Ayesha",
        createdAtEpochMillis = 1000L,
        updatedAtEpochMillis = 1000L,
      )
    val payload = SyncPayloads.ticket(ticket)
    assertTrue(payload.contains("\"category\":\"PLUMBING\""))
    assertTrue(payload.contains("\"note\":\"Tap leaks\""))
    assertTrue(payload.contains("\"photoRef\":\"photo-1.jpg\""))
  }

  @Test
  fun `ticket payload encodes missing photo as null`() {
    val ticket =
      Ticket(
        id = "t-1",
        unitId = "u-1",
        roomNumber = "101",
        category = TicketCategory.FURNITURE,
        note = "Wobbly chair",
        photoRef = null,
        status = TicketStatus.OPEN,
        createdBy = "Ayesha",
        createdAtEpochMillis = 1000L,
        updatedAtEpochMillis = 1000L,
      )
    assertTrue(SyncPayloads.ticket(ticket).contains("\"photoRef\":null"))
  }

  @Test
  fun `payload escaping handles quotes newlines and backslashes`() {
    assertEquals("a\\\"b\\\\c\\nd", SyncPayloads.escape("a\"b\\c\nd"))
    val payload = SyncPayloads.ticketStatus("t-1", TicketStatus.DONE, "Aye\"sha", 1000L)
    assertTrue(payload.contains("\"actor\":\"Aye\\\"sha\""))
  }

  @Test
  fun `no payload builder output can contain the api key sentinel`() {
    val sentinel = "SECRET-API-KEY-9f2a-UNIQUE"
    val ticket =
      Ticket(
        id = "t-1",
        unitId = "u-1",
        roomNumber = "101",
        category = TicketCategory.ELECTRICAL,
        note = "Socket sparking",
        photoRef = null,
        status = TicketStatus.OPEN,
        createdBy = "Rahim",
        createdAtEpochMillis = 1000L,
        updatedAtEpochMillis = 1000L,
      )
    val payloads =
      listOf(
        SyncPayloads.unitStatus("u-1", HousekeepingState.READY, "Rahim", 1000L),
        SyncPayloads.checklist("u-1", listOf("bed_linen"), false, "Rahim", 1000L),
        SyncPayloads.ticket(ticket),
        SyncPayloads.ticketStatus("t-1", TicketStatus.DONE, "Rahim", 1000L),
      )
    payloads.forEach { assertFalse(it.contains(sentinel)) }
  }
}
