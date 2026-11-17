package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ServerTicketsTest {

  @Test
  fun `bare array parses with done fields`() {
    val parsed =
      ServerTickets.parseArray(
        "[{\"id\":\"srv-9\",\"unitId\":\"u-101\",\"title\":\"AC\",\"description\":\"no cooling\"," +
          "\"priority\":\"NORMAL\",\"status\":\"DONE\",\"createdBy\":\"A\",\"createdAt\":1," +
          "\"doneBy\":\"Rahim\",\"doneAt\":2000}]",
      )
    assertEquals(1, parsed.size)
    val ticket = parsed.single()
    assertEquals("srv-9", ticket.id)
    assertEquals(TicketCategory.AIR_CONDITIONING, ticket.category)
    assertEquals(TicketStatus.DONE, ticket.status)
    assertEquals("Rahim", ticket.doneBy)
    assertEquals(2_000L, ticket.doneAt)
  }

  @Test
  fun `retired titles degrade to other on read`() {
    val parsed = ServerTickets.parseArray("[{\"id\":\"srv-1\",\"title\":\"Linen\",\"status\":\"OPEN\"}]")
    assertEquals(TicketCategory.OTHER, parsed.single().category)
  }

  @Test
  fun `bad items skip without failing the list`() {
    val parsed =
      ServerTickets.parseArray(
        "[" +
          "{\"id\":\"\",\"status\":\"OPEN\"}," +
          "{\"id\":\"srv-x\",\"status\":\"ARCHIVED\"}," +
          "\"junk\"," +
          "{\"id\":\"srv-ok\",\"status\":\"OPEN\"}" +
          "]",
      )
    assertEquals(listOf("srv-ok"), parsed.map { it.id })
  }

  @Test
  fun `missing done fields degrade to null`() {
    val parsed = ServerTickets.parseArray("[{\"id\":\"srv-1\",\"status\":\"OPEN\"}]")
    assertNull(parsed.single().doneBy)
    assertNull(parsed.single().doneAt)
  }

  @Test
  fun `malformed top level throws for lenient fetch handling`() {
    try {
      ServerTickets.parseArray("not-json")
      throw AssertionError("expected JSONException")
    } catch (e: org.json.JSONException) {
      assertTrue(true)
    }
  }
}
