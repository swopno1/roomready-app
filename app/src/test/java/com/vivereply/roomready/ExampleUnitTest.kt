package com.vivereply.roomready

import androidx.compose.ui.graphics.Color
import com.vivereply.roomready.ui.navigation.RoomReadyDestination
import com.vivereply.roomready.ui.theme.DefaultRoomReadyColors
import com.vivereply.roomready.ui.theme.RoomReadyTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemUnitTest {

  @Test
  fun `brand color tokens match ViveReply ink mint and warm off-white palette`() {
    assertEquals(Color(0xFF0F172A), DefaultRoomReadyColors.ink)
    assertEquals(Color(0xFF00FF94), DefaultRoomReadyColors.mintStart)
    assertEquals(Color(0xFF00EEDA), DefaultRoomReadyColors.mintEnd)
    assertEquals(Color(0xFF00F1CE), DefaultRoomReadyColors.mintSolid)
    assertEquals(Color(0xFFFAF8F4), DefaultRoomReadyColors.canvas)
  }

  @Test
  fun `semantic status colors are distinct`() {
    val statuses =
      setOf(
        DefaultRoomReadyColors.statusDirty,
        DefaultRoomReadyColors.statusCleaning,
        DefaultRoomReadyColors.statusCleaned,
        DefaultRoomReadyColors.statusReady,
      )
    assertEquals(4, statuses.size)
    assertNotEquals(DefaultRoomReadyColors.statusDirty, DefaultRoomReadyColors.statusReady)
  }

  @Test
  fun `typography meets low-literacy minimum font sizes`() {
    assertTrue(RoomReadyTypography.bodyLarge.fontSize.value >= 16f)
    assertTrue(RoomReadyTypography.bodyMedium.fontSize.value >= 16f)
    assertTrue(RoomReadyTypography.titleLarge.fontSize.value >= 20f)
    assertTrue(RoomReadyTypography.headlineSmall.fontSize.value >= 20f)
    assertTrue(RoomReadyTypography.labelLarge.fontSize.value >= 16f)
  }

  @Test
  fun `bottom navigation defines five destinations in exact order`() {
    assertEquals(
      listOf("home", "rooms", "tickets", "activity", "profile"),
      RoomReadyDestination.entries.map { it.route },
    )
  }
}
