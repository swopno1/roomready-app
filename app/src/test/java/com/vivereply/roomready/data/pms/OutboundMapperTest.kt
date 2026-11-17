package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.domain.model.PmsHousekeeping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exact op-table mapping from stored queue entries to wire calls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OutboundMapperTest {

  private fun unitPayload(localState: String, extra: String = ""): String =
    "{\"unitId\":\"u-101\",\"housekeeping\":\"DIRTY\",\"localState\":\"$localState\"," +
      "\"actor\":\"Ayesha\",\"at\":1000$extra}"

  @Test
  fun `start cleaning resolves locally with zero wire shape`() {
    assertEquals(
      MappedCall.LocalResolve,
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANING")),
    )
  }

  @Test
  fun `checklist entries always resolve locally`() {
    assertEquals(
      MappedCall.LocalResolve,
      OutboundMapper.map(SyncEntityType.CHECKLIST, "u-101", "{\"unitId\":\"u-101\"}"),
    )
    assertEquals(
      MappedCall.LocalResolve,
      OutboundMapper.map(SyncEntityType.CHECKLIST, "u-101", "not-json{{"),
    )
  }

  @Test
  fun `finish maps to mark clean with dirty expected`() {
    assertEquals(
      MappedCall.RoomStatus("u-101", PmsHousekeeping.DIRTY, StatusAction.MARK_CLEAN, null),
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANED")),
    )
  }

  @Test
  fun `finish carries the skip reason and prefers note over legacy skipReason`() {
    val legacy =
      OutboundMapper.map(
        SyncEntityType.UNIT_STATUS,
        "u-101",
        unitPayload("CLEANED", ",\"skipReason\":\"too dark\""),
      ) as MappedCall.RoomStatus
    assertEquals("too dark", legacy.note)

    val current =
      OutboundMapper.map(
        SyncEntityType.UNIT_STATUS,
        "u-101",
        unitPayload("CLEANED", ",\"skipReason\":\"old\",\"note\":\"new\""),
      ) as MappedCall.RoomStatus
    assertEquals("new", current.note)
  }

  @Test
  fun `inspect pass maps to clean expected without note`() {
    assertEquals(
      MappedCall.RoomStatus("u-101", PmsHousekeeping.CLEAN, StatusAction.INSPECT_PASS, null),
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("READY")),
    )
  }

  @Test
  fun `send back maps to inspect fail with the reason`() {
    assertEquals(
      MappedCall.RoomStatus("u-101", PmsHousekeeping.CLEAN, StatusAction.INSPECT_FAIL, "dust"),
      OutboundMapper.map(
        SyncEntityType.UNIT_STATUS,
        "u-101",
        unitPayload("DIRTY", ",\"note\":\"dust\""),
      ),
    )
  }

  @Test
  fun `legacy send back without a reason cannot translate`() {
    val mapped =
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("DIRTY"))
    assertTrue(mapped is MappedCall.Untranslatable)
  }

  @Test
  fun `corrupt unit entries park visibly`() {
    assertTrue(
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "u-101", "not-json{{") is MappedCall.Untranslatable,
    )
    assertTrue(
      OutboundMapper.map(
        SyncEntityType.UNIT_STATUS,
        "u-101",
        unitPayload("SCRUBBING"),
      ) is MappedCall.Untranslatable,
    )
    assertTrue(
      OutboundMapper.map(SyncEntityType.UNIT_STATUS, "  ", unitPayload("CLEANED")) is MappedCall.Untranslatable,
    )
  }

  @Test
  fun `ticket create maps fields with english title`() {
    val mapped =
      OutboundMapper.map(
        SyncEntityType.TICKET,
        "local-1",
        "{\"ticketId\":\"local-1\",\"unitId\":\"u-101\",\"category\":\"AIR_CONDITIONING\"," +
          "\"note\":\"no cooling\",\"status\":\"OPEN\"}",
      ) as MappedCall.TicketCreate
    assertEquals("local-1", mapped.localId)
    assertEquals("u-101", mapped.unitId)
    assertEquals("AC", mapped.title)
    assertEquals("no cooling", mapped.description)
  }

  @Test
  fun `retired ticket categories degrade to other`() {
    listOf("LINEN", "MINIBAR", "BOGUS", null).forEach { raw ->
      val categoryField = if (raw == null) "" else ",\"category\":\"$raw\""
      val mapped =
        OutboundMapper.map(
          SyncEntityType.TICKET,
          "local-1",
          "{\"ticketId\":\"local-1\",\"unitId\":\"u-101\",\"note\":\"x\"$categoryField}",
        ) as MappedCall.TicketCreate
      assertEquals("for $raw", "Other", mapped.title)
    }
  }

  @Test
  fun `ticket without a unit cannot translate`() {
    assertTrue(
      OutboundMapper.map(
        SyncEntityType.TICKET,
        "local-1",
        "{\"ticketId\":\"local-1\",\"note\":\"x\"}",
      ) is MappedCall.Untranslatable,
    )
    assertTrue(
      OutboundMapper.map(SyncEntityType.TICKET, "local-1", "nope") is MappedCall.Untranslatable,
    )
  }

  @Test
  fun `ticket done maps to its local id for server resolution`() {
    assertEquals(
      MappedCall.TicketDone("local-1"),
      OutboundMapper.map(SyncEntityType.TICKET_STATUS, "local-1", "{}"),
    )
  }

  @Test
  fun `blank ticket note degrades to omitted description`() {
    val mapped =
      OutboundMapper.map(
        SyncEntityType.TICKET,
        "local-1",
        "{\"ticketId\":\"local-1\",\"unitId\":\"u-101\",\"category\":\"OTHER\",\"note\":\"   \"}",
      ) as MappedCall.TicketCreate
    assertEquals(null, mapped.description)
    assertEquals("Other", mapped.title)
  }
}
