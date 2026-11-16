package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BootstrapParserTest {

  @Test
  fun `full payload parses with property types units staff and checklist`() {
    val parsed = BootstrapParser.parse(FULL, 9_000L).getOrThrow()
    assertEquals("demo13", parsed.propertyId)
    assertEquals("Krishnachura Retreat", parsed.propertyName)
    assertEquals(1, parsed.roomTypes.size)
    assertEquals("Terracotta Villa", parsed.roomTypes.single().nameEn)
    assertEquals(1, parsed.units.size)
    val unit = parsed.units.single()
    assertEquals("101", unit.number)
    assertEquals(HousekeepingState.CLEANED, unit.hkState)
    assertEquals(Occupancy.VACANT, unit.occupancy)
    assertEquals(1_794_729_600_000L, unit.housekeepingAtEpochMillis)
    assertEquals(listOf("Ayesha"), parsed.staff.map { it.name })
    assertEquals(StaffRole.SUPERVISOR, parsed.staff.single().role)
    assertEquals(listOf("bed_linen", "vacuum_mop"), parsed.checklistKeys)
    assertTrue(parsed.warnings.isEmpty())
  }

  @Test
  fun `missing housekeepingAt degrades to null and never fails`() {
    val parsed = BootstrapParser.parse(UNIT_NO_TIMESTAMP, 9_000L).getOrThrow()
    assertEquals(1, parsed.units.size)
    assertNull(parsed.units.single().housekeepingAtEpochMillis)
    assertEquals(9_000L, parsed.units.single().updatedAtEpochMillis)
  }

  @Test
  fun `unparseable housekeepingAt degrades to null with a warning`() {
    listOf("\"yesterday\"", "\"2026-13-45T99:99:99Z\"", "{\"odd\":true}", "[1]").forEach { raw ->
      val json = UNIT_TEMPLATE.format(raw)
      val parsed = BootstrapParser.parse(json, 9_000L).getOrThrow()
      assertNull("for $raw", parsed.units.single().housekeepingAtEpochMillis)
      assertTrue(
        "warning for $raw",
        parsed.warnings.any { it is BootstrapWarning.BadTimestamp },
      )
    }
  }

  @Test
  fun `unknown housekeeping degrades to dirty with a warning`() {
    val parsed = BootstrapParser.parse(UNIT_UNKNOWN_ENUMS, 9_000L).getOrThrow()
    val unit = parsed.units.single()
    assertEquals(HousekeepingState.DIRTY, unit.hkState)
    assertEquals(Occupancy.UNKNOWN, unit.occupancy)
    assertTrue(parsed.warnings.any { it is BootstrapWarning.UnknownHousekeeping })
    assertTrue(parsed.warnings.any { it is BootstrapWarning.UnknownOccupancy })
  }

  @Test
  fun `unknown staff role degrades to housekeeper with a warning`() {
    val parsed = BootstrapParser.parse(STAFF_UNKNOWN_ROLE, 9_000L).getOrThrow()
    assertEquals(StaffRole.HOUSEKEEPER, parsed.staff.single().role)
    assertTrue(parsed.warnings.any { it is BootstrapWarning.UnknownStaffRole })
  }

  @Test
  fun `unknown checklist keys are skipped with a warning`() {
    val parsed = BootstrapParser.parse(CHECKLIST_UNKNOWN_KEY, 9_000L).getOrThrow()
    assertEquals(listOf("bed_linen"), parsed.checklistKeys)
    assertTrue(parsed.warnings.any { it is BootstrapWarning.UnknownChecklistKey })
  }

  @Test
  fun `units without id and staff without name are skipped never fatal`() {
    val parsed = BootstrapParser.parse(SKIPPABLE, 9_000L).getOrThrow()
    assertTrue(parsed.units.isEmpty())
    assertTrue(parsed.staff.isEmpty())
    assertTrue(parsed.warnings.any { it is BootstrapWarning.SkippedUnit })
    assertTrue(parsed.warnings.any { it is BootstrapWarning.SkippedStaff })
  }

  @Test
  fun `dangling room type warns but keeps the unit`() {
    val parsed = BootstrapParser.parse(UNIT_DANGLING_TYPE, 9_000L).getOrThrow()
    assertEquals(1, parsed.units.size)
    assertTrue(parsed.warnings.any { it is BootstrapWarning.DanglingRoomType })
  }

  @Test
  fun `contract mismatch fails typed`() {
    val result = BootstrapParser.parse("{\"contractVersion\":2,\"units\":[]}", 9_000L)
    assertTrue(result.isFailure)
    assertEquals(
      BootstrapException.ContractMismatch(2),
      result.exceptionOrNull() as BootstrapException,
    )
  }

  @Test
  fun `missing contract version fails typed`() {
    val result = BootstrapParser.parse("{\"units\":[]}", 9_000L)
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BootstrapException.ContractMismatch)
  }

  @Test
  fun `malformed json fails typed`() {
    val result = BootstrapParser.parse("{\"contractVersion\":1,", 9_000L)
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BootstrapException.MalformedJson)
  }

  @Test
  fun `missing arrays parse as empty without failure`() {
    val parsed = BootstrapParser.parse("{\"contractVersion\":1}", 9_000L).getOrThrow()
    assertTrue(parsed.units.isEmpty())
    assertTrue(parsed.roomTypes.isEmpty())
    assertTrue(parsed.staff.isEmpty())
    assertTrue(parsed.checklistKeys.isEmpty())
    assertNull(parsed.propertyName)
  }

  companion object {
    private const val FULL =
      """{"contractVersion":1,
        "property":{"id":"demo13","name":"Krishnachura Retreat"},
        "roomTypes":[{"id":"rt-terra","code":"TERRA","nameEn":"Terracotta Villa","nameBn":"টেরাকোটা ভিলা","bedSummary":"1 king","baseOccupancy":2}],
        "units":[{"id":"u-101","number":"101","roomTypeId":"rt-terra","floor":"1","occupancy":"VACANT","housekeeping":"CLEAN","housekeepingAt":"2026-11-15T08:00:00Z"}],
        "staff":[{"name":"Ayesha","role":"SUPERVISOR"}],
        "checklist":[{"key":"bed_linen","order":1},"vacuum_mop"]}"""

    private const val UNIT_NO_TIMESTAMP =
      """{"contractVersion":1,
        "units":[{"id":"u-101","number":"101","roomTypeId":"rt-x","housekeeping":"DIRTY"}]}"""

    private const val UNIT_TEMPLATE =
      """{"contractVersion":1,
        "units":[{"id":"u-101","number":"101","roomTypeId":"rt-x","housekeeping":"DIRTY","housekeepingAt":%s}]}"""

    private const val UNIT_UNKNOWN_ENUMS =
      """{"contractVersion":1,
        "units":[{"id":"u-101","number":"101","roomTypeId":"rt-x","occupancy":"GUESTS?!","housekeeping":"SCRUBBING"}]}"""

    private const val STAFF_UNKNOWN_ROLE =
      """{"contractVersion":1,"staff":[{"name":"Karim","role":"NIGHT_AUDIT"}]}"""

    private const val CHECKLIST_UNKNOWN_KEY =
      """{"contractVersion":1,"checklist":["bed_linen","polish_chandelier"]}"""

    private const val SKIPPABLE =
      """{"contractVersion":1,
        "units":[{"number":"101"}],
        "staff":[{"role":"HOUSEKEEPER"}]}"""

    private const val UNIT_DANGLING_TYPE =
      """{"contractVersion":1,
        "roomTypes":[{"id":"rt-a","code":"A"}],
        "units":[{"id":"u-101","number":"101","roomTypeId":"rt-ghost","housekeeping":"DIRTY"}]}"""
  }
}
