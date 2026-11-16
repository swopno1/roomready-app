package com.vivereply.roomready.domain

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.PmsHousekeeping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PmsMappingTest {

  @Test
  fun `pms codes map to local states per contract`() {
    assertEquals(HousekeepingState.DIRTY, PmsHousekeeping.fromCode("DIRTY")?.toLocal())
    assertEquals(HousekeepingState.CLEANED, PmsHousekeeping.fromCode("CLEAN")?.toLocal())
    assertEquals(HousekeepingState.READY, PmsHousekeeping.fromCode("INSPECTED")?.toLocal())
  }

  @Test
  fun `pms code parsing is tolerant of case and whitespace`() {
    assertEquals(PmsHousekeeping.CLEAN, PmsHousekeeping.fromCode("  clean "))
    assertEquals(PmsHousekeeping.INSPECTED, PmsHousekeeping.fromCode("Inspected"))
  }

  @Test
  fun `unknown pms codes parse to null so import keeps local state`() {
    assertNull(PmsHousekeeping.fromCode(null))
    assertNull(PmsHousekeeping.fromCode(""))
    assertNull(PmsHousekeeping.fromCode("   "))
    assertNull(PmsHousekeeping.fromCode("SCRUBBING"))
  }

  @Test
  fun `local states map back to pms codes with cleaning reported dirty`() {
    assertEquals(PmsHousekeeping.DIRTY, PmsHousekeeping.fromLocal(HousekeepingState.DIRTY))
    assertEquals(PmsHousekeeping.DIRTY, PmsHousekeeping.fromLocal(HousekeepingState.CLEANING))
    assertEquals(PmsHousekeeping.CLEAN, PmsHousekeeping.fromLocal(HousekeepingState.CLEANED))
    assertEquals(PmsHousekeeping.INSPECTED, PmsHousekeeping.fromLocal(HousekeepingState.READY))
  }

  @Test
  fun `pms to local to pms round trips are stable`() {
    for (pms in PmsHousekeeping.entries) {
      assertEquals(pms, PmsHousekeeping.fromLocal(pms.toLocal()))
    }
  }

  @Test
  fun `occupancy parsing covers known codes and falls back to unknown`() {
    assertEquals(Occupancy.VACANT, Occupancy.fromCode("VACANT"))
    assertEquals(Occupancy.OCCUPIED, Occupancy.fromCode("occupied"))
    assertEquals(Occupancy.OUT_OF_ORDER, Occupancy.fromCode("OOO"))
    assertEquals(Occupancy.OUT_OF_ORDER, Occupancy.fromCode("OUT_OF_ORDER"))
    assertEquals(Occupancy.UNKNOWN, Occupancy.fromCode(null))
    assertEquals(Occupancy.UNKNOWN, Occupancy.fromCode(""))
    assertEquals(Occupancy.UNKNOWN, Occupancy.fromCode("SOMETHING_NEW"))
  }
}
