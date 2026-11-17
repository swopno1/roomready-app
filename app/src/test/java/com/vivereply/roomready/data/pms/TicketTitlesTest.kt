package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.TicketCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class TicketTitlesTest {

  @Test
  fun `stored names map to exact english wire titles`() {
    assertEquals("Plumbing", TicketTitles.wireTitle("PLUMBING"))
    assertEquals("Electrical", TicketTitles.wireTitle("ELECTRICAL"))
    assertEquals("AC", TicketTitles.wireTitle("AIR_CONDITIONING"))
    assertEquals("Furniture", TicketTitles.wireTitle("FURNITURE"))
    assertEquals("Other", TicketTitles.wireTitle("OTHER"))
  }

  @Test
  fun `unknown stored names degrade to other`() {
    listOf(null, "", "  ", "LINEN", "MINIBAR", "BOGUS").forEach {
      assertEquals("for <$it>", "Other", TicketTitles.wireTitle(it))
    }
  }

  @Test
  fun `server titles parse back to categories`() {
    assertEquals(TicketCategory.PLUMBING, TicketTitles.parseCategory("Plumbing"))
    assertEquals(TicketCategory.ELECTRICAL, TicketTitles.parseCategory("Electrical"))
    assertEquals(TicketCategory.AIR_CONDITIONING, TicketTitles.parseCategory("AC"))
    assertEquals(TicketCategory.FURNITURE, TicketTitles.parseCategory("Furniture"))
    assertEquals(TicketCategory.OTHER, TicketTitles.parseCategory("Other"))
  }

  @Test
  fun `retired and unknown server titles degrade to other`() {
    listOf(null, "", "  ", "Linen", "Minibar", "Chandelier").forEach {
      assertEquals("for <$it>", TicketCategory.OTHER, TicketTitles.parseCategory(it))
    }
  }
}
