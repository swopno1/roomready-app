package com.vivereply.roomready.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.screens.ActivityScreen
import com.vivereply.roomready.ui.screens.HomeDashboardScreen
import com.vivereply.roomready.ui.screens.HomeDashboardUiState
import com.vivereply.roomready.ui.screens.SetupFormState
import com.vivereply.roomready.ui.screens.SetupScreen
import com.vivereply.roomready.ui.state.SyncStatusUi
import com.vivereply.roomready.ui.state.TodayStats
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Connected screens render their acceptance-critical content. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConnectedScreensTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `home shows last synced timestamp and pending count`() {
    compose.setContent {
      RoomReadyTheme {
        HomeDashboardScreen(
          state =
            HomeDashboardUiState(
              status =
                SyncStatusUi(
                  connected = true,
                  lastSyncedAt = 1_794_729_600_000L,
                  pendingCount = 3,
                  syncing = false,
                  error = null,
                ),
              staffName = "Ayesha",
              hourOfDay = 9,
              units =
                listOf(
                  RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
                  RoomUnit("u-102", "102", "rt-t", "1", Occupancy.OCCUPIED, HousekeepingState.READY, 1L),
                ),
              types = mapOf("rt-t" to RoomType("rt-t", "TERRA", "Terracotta Villa", "টেরাকোটা ভিলা", "1 king", 2)),
              role = StaffRole.HOUSEKEEPER,
              nowEpochMillis = 3_661_000L,
            ),
          languageTag = "en",
          onSyncNow = {},
          onFilterSelected = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
        )
      }
    }
    compose.onNodeWithTag("home_greeting").assertIsDisplayed().assertTextContains("Ayesha")
    compose.onNodeWithTag("home_hero").assertIsDisplayed()
    compose.onNodeWithTag("home_chip_dirty").assertIsDisplayed()
    scrollHomeTo("home_last_synced")
    compose.onNodeWithTag("home_last_synced").assertTextContains("Last synced:", substring = true)
    scrollHomeTo("home_pending")
    compose.onNodeWithTag("home_pending").assertTextContains("3 waiting to sync")
    scrollHomeTo("home_sync_pill")
    scrollHomeTo("unit_card_u-101")
    compose.onAllNodesWithText("Terracotta Villa", substring = true)[0].assertIsDisplayed()
  }

  private fun scrollHomeTo(tag: String) {
    compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
    compose.onNodeWithTag(tag).assertIsDisplayed()
  }

  @Test
  fun `home shows never-synced state honestly`() {
    compose.setContent {
      RoomReadyTheme {
        HomeDashboardScreen(
          state =
            HomeDashboardUiState(
              status = SyncStatusUi(true, null, 0, false, null),
              staffName = null,
              hourOfDay = 20,
              units = emptyList(),
              types = emptyMap(),
              role = null,
            ),
          languageTag = "en",
          onSyncNow = {},
          onFilterSelected = {},
          onAction = { _, _ -> },
          onSendBack = {},
          onCardClick = {},
        )
      }
    }
    compose.onNodeWithText("No rooms from the PMS yet").assertIsDisplayed()
    scrollHomeTo("home_last_synced")
    compose.onNodeWithText("Not synced yet").assertIsDisplayed()
  }

  @Test
  fun `activity flags a parked push`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
          lastSyncedText = null,
          pendingCount = 0,
          flagged = emptyList(),
          today = TodayStats(0, 0, 0),
          events =
            listOf(
              ActivityEvent(
                id = 2L,
                type = ActivityType.PUSH_PARKED,
                actorName = "system",
                role = null,
                unitId = null,
                roomNumber = null,
                detail = "TICKET t-bad HTTP 400",
                createdAtEpochMillis = 2_000L,
              ),
              ActivityEvent(
                id = 1L,
                type = ActivityType.STATUS_CHANGED,
                actorName = "Ayesha",
                role = null,
                unitId = "u-101",
                roomNumber = "101",
                detail = "DIRTY>CLEANING",
                createdAtEpochMillis = 1_000L,
              ),
            ),
          languageTag = "en",
        )
      }
    }
    compose.onNodeWithTag("activity_list").assertIsDisplayed()
    compose.onNodeWithTag("activity_parked_flag").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Needs attention").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("TICKET t-bad HTTP 400").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Push parked").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `setup shows the wrong-key refusal plainly`() {
    compose.setContent {
      RoomReadyTheme {
        SetupScreen(
          state =
            SetupFormState(
              address = "https://pms.demo13.com",
              apiKey = "rk.k597.wrong",
              staffName = "Ayesha",
              connecting = false,
              error = PmsError.WrongKey,
            ),
          onAddressChange = {},
          onApiKeyChange = {},
          onStaffNameChange = {},
          onConnect = {},
          onScanQr = {},
          onPasteCode = {},
        )
      }
    }
    compose.onNodeWithTag("setup_error").assertIsDisplayed()
      .assertTextContains("That API key was refused", substring = true)
  }
}
