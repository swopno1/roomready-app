package com.vivereply.roomready.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG contrast proof for every status pairing and text color in the
 * theme: normal text needs 4.5 (AA), large text and UI components 3.0.
 * Each pairing names the screens that rely on it.
 */
class ContrastTest {

  private val colors = DefaultRoomReadyColors

  @Test
  fun `status chip text passes on its container`() {
    // Home chips, unit cards, ticket cards, activity flags.
    assertText(colors.onStatusDirtyContainer, colors.statusDirtyContainer, "dirty chip")
    assertText(colors.onStatusCleaningContainer, colors.statusCleaningContainer, "cleaning chip")
    assertText(colors.onStatusCleanedContainer, colors.statusCleanedContainer, "cleaned chip")
    assertText(colors.onStatusReadyContainer, colors.statusReadyContainer, "ready chip")
  }

  @Test
  fun `bare status text passes on canvas`() {
    // Activity parked reasons, send-back buttons, inspection accents.
    assertText(colors.statusDirty, colors.canvas, "dirty text")
    assertText(colors.statusCleaning, colors.canvas, "cleaning text")
    assertText(colors.statusCleaned, colors.canvas, "cleaned text")
    assertText(colors.statusReadyAccessible, colors.canvas, "ready text")
    assertText(colors.mintAccessibleInk, colors.canvas, "mint accent")
  }

  @Test
  fun `neutral text passes on every light background`() {
    // Titles, body, timestamps and hints on all screens.
    listOf(colors.canvas, colors.surface, colors.surfaceSubtle).forEach { bg ->
      assertText(colors.ink, bg, "ink")
      assertText(colors.inkSecondary, bg, "inkSecondary")
      assertText(colors.inkMuted, bg, "inkMuted")
    }
  }

  @Test
  fun `dark card pairings pass`() {
    // Home hero, profile header, pre-connect profile card.
    assertText(colors.onInk, colors.ink, "onInk on ink")
    assertText(colors.ink, colors.mintSolid, "ink on mint")
    // Profile rows dim their labels to 70% white on the ink card.
    assertText(blend(colors.onInk.copy(alpha = 0.7f), colors.ink), colors.ink, "dimmed label on ink")
    // Unselected dark language chip: white on translucent slate over ink.
    val chipBg = blend(colors.inkSecondary.copy(alpha = 0.45f), colors.ink)
    assertText(colors.onInk, chipBg, "dark language chip")
    // Unselected light language chip on the profile card.
    assertText(colors.inkSecondary, colors.surfaceSubtle, "light language chip")
  }

  @Test
  fun `status rails pass as non-text indicators`() {
    // Card left rails on Rooms/Home; the mint rail is decorative only —
    // every card also carries a labeled chip, which the first test covers.
    assertUi(colors.statusDirty, colors.surface, "dirty rail")
    assertUi(colors.statusCleaning, colors.surface, "cleaning rail")
    assertUi(colors.statusCleaned, colors.surface, "cleaned rail")
  }

  private fun assertText(fg: Color, bg: Color, name: String) {
    assertTrue("$name ratio ${ratio(fg, bg)} below 4.5", ratio(fg, bg) >= 4.5)
  }

  private fun assertUi(fg: Color, bg: Color, name: String) {
    assertTrue("$name ratio ${ratio(fg, bg)} below 3.0", ratio(fg, bg) >= 3.0)
  }

  private fun ratio(a: Color, b: Color): Double {
    val la = luminance(blendIfNeeded(a)) + 0.05
    val lb = luminance(blendIfNeeded(b)) + 0.05
    return maxOf(la, lb) / minOf(la, lb)
  }

  /** Flattens a translucent color over its backdrop; opaque passes through. */
  private fun blend(fg: Color, bg: Color): Color {
    val a = fg.alpha
    return Color(
      red = fg.red * a + bg.red * (1 - a),
      green = fg.green * a + bg.green * (1 - a),
      blue = fg.blue * a + bg.blue * (1 - a),
    )
  }

  private fun blendIfNeeded(color: Color): Color =
    if (color.alpha >= 1f) color else blend(color, Color.White)

  private fun luminance(color: Color): Double {
    fun channel(c: Float): Double =
      if (c <= 0.03928f) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) +
      0.0722 * channel(color.blue)
  }
}
