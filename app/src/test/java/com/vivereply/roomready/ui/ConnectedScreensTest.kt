package com.vivereply.roomready.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.ui.screens.ActivityScreen
import com.vivereply.roomready.ui.screens.HomeBoardScreen
import com.vivereply.roomready.ui.screens.SetupFormState
import com.vivereply.roomready.ui.screens.SetupScreen
import com.vivereply.roomready.ui.state.SyncStatusUi
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
        HomeBoardScreen(
          status =
            SyncStatusUi(
              connected = true,
              lastSyncedAt = 1_794_729_600_000L,
              pendingCount = 3,
              syncing = false,
              error = null,
            ),
          units =
            listOf(
              RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
              RoomUnit("u-102", "102", "rt-t", "1", Occupancy.OCCUPIED, HousekeepingState.READY, 1L),
            ),
          typeNames = mapOf("rt-t" to "Terracotta Villa"),
          languageTag = "en",
          onSyncNow = {},
        )
      }
    }
    compose.onNodeWithTag("home_last_synced").assertIsDisplayed()
      .assertTextContains("Last synced:", substring = true)
    compose.onNodeWithTag("home_pending").assertIsDisplayed()
      .assertTextContains("3 waiting to sync")
    compose.onNodeWithTag("home_counts").assertIsDisplayed()
    compose.onAllNodesWithText("Terracotta Villa", substring = true)[0].performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `home shows never-synced state honestly`() {
    compose.setContent {
      RoomReadyTheme {
        HomeBoardScreen(
          status = SyncStatusUi(true, null, 0, false, null),
          units = emptyList(),
          typeNames = emptyMap(),
          languageTag = "en",
          onSyncNow = {},
        )
      }
    }
    compose.onNodeWithText("Not synced yet").assertIsDisplayed()
    compose.onNodeWithText("No rooms from the PMS yet").assertIsDisplayed()
  }

  @Test
  fun `activity flags a parked push`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
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
    compose.onNodeWithTag("activity_parked_flag").assertIsDisplayed()
    compose.onNodeWithText("Needs attention").assertIsDisplayed()
    compose.onNodeWithText("TICKET t-bad HTTP 400").assertIsDisplayed()
    compose.onNodeWithText("Push parked").assertIsDisplayed()
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
