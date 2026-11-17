package com.vivereply.roomready.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import com.vivereply.roomready.data.pms.BootstrapParser
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.components.UnitCard
import com.vivereply.roomready.ui.screens.HomeDashboardScreen
import com.vivereply.roomready.ui.screens.HomeDashboardUiState
import com.vivereply.roomready.ui.screens.RoomsBoardScreen
import com.vivereply.roomready.ui.screens.RoomsBoardUiState
import com.vivereply.roomready.ui.state.BoardFilter
import com.vivereply.roomready.ui.state.SyncStatusUi
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** R4 showcase acceptance: grouping, states, animation path, 1.3x font scale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShowcaseScreensTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `board shows all 19 replay units in 5 type groups`() {
    val (units, types) = replayBoard()
    compose.setContent {
      RoomReadyTheme {
        RoomsBoardScreen(
          state =
            RoomsBoardUiState(
              units = units,
              types = types,
              filter = BoardFilter.ALL,
              role = StaffRole.HOUSEKEEPER,
              nowEpochMillis = 1_794_872_400_000L,
            ),
          languageTag = "en",
          onFilter = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
          onRetry = {},
        )
      }
    }
    listOf("rt-terracotta", "rt-bamboo", "rt-clayhouse", "rt-palmloft", "rt-earthlodge")
      .forEach { id -> scrollBoardTo("type_header_$id") }
    val numbers =
      listOf("101", "102", "103", "104") + listOf("201", "202", "203", "204", "205", "206") +
        listOf("301", "302", "303", "304") + listOf("401", "402", "403") + listOf("501", "502")
    assertEquals(19, numbers.size)
    numbers.forEach { number -> scrollBoardTo("unit_card_u-$number") }
    compose.onNodeWithTag("board_filters").assertIsDisplayed()
  }

  private fun scrollBoardTo(tag: String) {
    compose.onNodeWithTag("board_list").performScrollToNode(hasTestTag(tag))
    compose.onNodeWithTag(tag).assertIsDisplayed()
  }

  @Test
  fun `dirty filter keeps dirty rooms and hides the rest`() {
    val (units, types) = replayBoard()
    compose.setContent {
      RoomReadyTheme {
        RoomsBoardScreen(
          state = RoomsBoardUiState(units = units, types = types, filter = BoardFilter.DIRTY),
          languageTag = "en",
          onFilter = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
          onRetry = {},
        )
      }
    }
    scrollBoardTo("unit_card_u-101")
    compose.onNodeWithTag("unit_card_u-104").assertDoesNotExist()
    compose.onNodeWithTag("filter_dirty").assertIsDisplayed()
  }

  @Test
  fun `empty filter result shows the caught-up state`() {
    val (units, types) = replayBoard()
    compose.setContent {
      RoomReadyTheme {
        RoomsBoardScreen(
          state = RoomsBoardUiState(units = units, types = types, filter = BoardFilter.CLEANING),
          languageTag = "en",
          onFilter = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
          onRetry = {},
        )
      }
    }
    compose.onNodeWithTag("board_caught_up").assertIsDisplayed()
    compose.onNodeWithText("All caught up").assertIsDisplayed()
  }

  @Test
  fun `loading shows shimmer and error shows retry`() {
    var loading by mutableStateOf(true)
    var retried = false
    compose.setContent {
      RoomReadyTheme {
        RoomsBoardScreen(
          state = RoomsBoardUiState(loading = loading, loadError = !loading),
          languageTag = "en",
          onFilter = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
          onRetry = { retried = true },
        )
      }
    }
    compose.onNodeWithTag("board_shimmer").assertIsDisplayed()

    loading = false
    compose.waitForIdle()
    compose.onNodeWithTag("board_error").assertIsDisplayed()
    compose.onNodeWithText("Try again").assertIsDisplayed().performClick()
    assertTrue("retry callback must fire", retried)
  }

  @Test
  fun `unit card flips status through the animated path`() {
    var unit by mutableStateOf(
      RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
    )
    var role by mutableStateOf<StaffRole?>(StaffRole.HOUSEKEEPER)
    compose.setContent {
      RoomReadyTheme {
        UnitCard(
          unit = unit,
          typeName = "Terracotta Villa",
          nowEpochMillis = 3_661_000L,
          role = role,
          busy = false,
          errorRes = null,
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
        )
      }
    }
    compose.onNodeWithText("Dirty").assertIsDisplayed()
    compose.onNodeWithText("Start cleaning").assertIsDisplayed()

    unit = unit.copy(hkState = HousekeepingState.CLEANED)
    role = StaffRole.SUPERVISOR
    compose.waitForIdle()
    compose.onNodeWithText("Cleaned").assertIsDisplayed()
    compose.onNodeWithText("Inspect").assertIsDisplayed()
    compose.onNodeWithTag("unit_card_u-101").assertIsDisplayed()
  }

  @Test
  fun `home dashboard survives 1_3x font scale`() {
    val (units, types) = replayBoard()
    scaledContent(1.3f) {
      HomeDashboardScreen(
        state =
          HomeDashboardUiState(
            status = SyncStatusUi(true, 1_794_729_600_000L, 2, false, null),
            staffName = "Ayesha",
            hourOfDay = 9,
            units = units,
            types = types,
            role = StaffRole.HOUSEKEEPER,
            nowEpochMillis = 1_794_872_400_000L,
          ),
        languageTag = "en",
        onSyncNow = {},
        onFilterSelected = {},
        onAction = { _, _ -> },
        onSendBack = {},
          onCardClick = {},
      )
    }
    compose.onNodeWithTag("home_hero").assertIsDisplayed()
    compose.onNodeWithTag("home_chip_ready").assertIsDisplayed()
    scrollHomeTo("home_sync_pill")
    scrollHomeTo("unit_card_u-101")
  }

  private fun scrollHomeTo(tag: String) {
    compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
    compose.onNodeWithTag(tag).assertIsDisplayed()
  }

  @Test
  fun `rooms board survives 1_3x font scale`() {
    val (units, types) = replayBoard()
    scaledContent(1.3f) {
      RoomsBoardScreen(
        state =
          RoomsBoardUiState(
            units = units,
            types = types,
            role = StaffRole.HOUSEKEEPER,
            nowEpochMillis = 1_794_872_400_000L,
          ),
        languageTag = "en",
        onFilter = {},
        onAction = { _, _ -> },
        onSendBack = {},
          onCardClick = {},
        onRetry = {},
      )
    }
    compose.onNodeWithTag("board_filters").assertIsDisplayed()
    scrollBoardTo("type_header_rt-bamboo")
    scrollBoardTo("unit_card_u-201")
    scrollBoardTo("unit_action_u-201")
    compose.onNodeWithTag("unit_action_u-201").assertTextContains("Start cleaning")
  }

  private fun scaledContent(scale: Float, content: @Composable () -> Unit) {
    compose.setContent {
      RoomReadyTheme {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
          content()
        }
      }
    }
  }

  private fun replayBoard() =
    run {
      val json = javaClass.classLoader!!.getResourceAsStream("bootstrap_replay.json")!!
        .bufferedReader().use { it.readText() }
      val parsed = BootstrapParser.parse(json, 9_000L).getOrThrow()
      parsed.units to parsed.roomTypes.associateBy { it.id }
    }
}
