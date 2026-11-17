package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The live demo13 bootstrap shape (captured 2026-11-17): units carry
 * `unitNumber` + `unitStatus`, types carry `title`, staff/checklist may be
 * absent. The parser must surface friendly names from this shape while the
 * legacy `number`/`occupancy`/`nameEn` keys keep working.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BootstrapLiveShapeTest {

  private val liveJson =
    """
    {
      "contractVersion": 1,
      "property": {"name": "Krishnachura Retreat", "timezone": "Asia/Dhaka"},
      "businessDate": "2026-11-17",
      "roomTypes": [
        {"id": "rt-live-1", "title": "Bamboo Cottage", "checklistSteps": ["Change bed linen"]}
      ],
      "units": [
        {"id": "u-live-1", "roomTypeId": "rt-live-1", "unitNumber": "BC-1",
         "floor": null, "block": null, "unitStatus": "AVAILABLE",
         "housekeeping": "CLEAN", "housekeepingAt": "2026-11-07T05:16:50.651Z",
         "departsToday": true},
        {"id": "u-live-2", "roomTypeId": "rt-live-1", "unitNumber": "BC-2",
         "unitStatus": "OCCUPIED", "housekeeping": "DIRTY"}
      ]
    }
    """.trimIndent()

  @Test
  fun `live shape yields friendly numbers names and occupancy`() {
    val parsed = BootstrapParser.parse(liveJson, 9_000L).getOrThrow()
    assertEquals("Krishnachura Retreat", parsed.propertyName)
    assertEquals(listOf("rt-live-1"), parsed.roomTypes.map { it.id })
    assertEquals("Bamboo Cottage", parsed.roomTypes[0].nameEn)
    assertEquals("Bamboo Cottage", parsed.roomTypes[0].nameBn)

    val first = parsed.units[0]
    assertEquals("BC-1", first.number)
    assertEquals(Occupancy.VACANT, first.occupancy)
    assertEquals(HousekeepingState.CLEANED, first.hkState)

    val second = parsed.units[1]
    assertEquals("BC-2", second.number)
    assertEquals(Occupancy.OCCUPIED, second.occupancy)
    assertEquals(HousekeepingState.DIRTY, second.hkState)
  }

  @Test
  fun `live shape parses warning-light`() {
    val parsed = BootstrapParser.parse(liveJson, 9_000L).getOrThrow()
    assertTrue("unexpected warnings: ${parsed.warnings}", parsed.warnings.isEmpty())
  }

  @Test
  fun `legacy number key still works when unitNumber is absent`() {
    val json =
      """
      {"contractVersion": 1, "property": {"name": "P"},
       "roomTypes": [], "units": [
        {"id": "u-1", "roomTypeId": "", "number": "101", "occupancy": "VACANT",
         "housekeeping": "DIRTY"}
      ]}
      """.trimIndent()
    val parsed = BootstrapParser.parse(json, 9_000L).getOrThrow()
    assertEquals("101", parsed.units[0].number)
    assertEquals(Occupancy.VACANT, parsed.units[0].occupancy)
  }

  @Test
  fun `unknown unitStatus degrades to unknown with a warning`() {
    val json =
      """
      {"contractVersion": 1, "property": {"name": "P"},
       "roomTypes": [], "units": [
        {"id": "u-9", "roomTypeId": "", "unitNumber": "X", "unitStatus": "FUMIGATED",
         "housekeeping": "DIRTY"}
      ]}
      """.trimIndent()
    val parsed = BootstrapParser.parse(json, 9_000L).getOrThrow()
    assertEquals(Occupancy.UNKNOWN, parsed.units[0].occupancy)
    assertEquals(1, parsed.warnings.size)
  }
}
