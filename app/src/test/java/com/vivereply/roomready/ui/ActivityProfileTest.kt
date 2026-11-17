package com.vivereply.roomready.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.sync.SyncEntryStatus
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.screens.ActivityScreen
import com.vivereply.roomready.ui.screens.ProfileScreen
import com.vivereply.roomready.ui.state.TodayStats
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Activity sync log + offline panel and Profile identity + disconnect flow. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivityProfileTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `activity sync log shows last synced pending and parked reasons`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
          lastSyncedText = "17 Nov, 09:30",
          pendingCount = 3,
          flagged =
            listOf(
              parkedEntry(1L, "The PMS refused this entry (HTTP 403)."),
              parkedEntry(2L, "The PMS has no such room (HTTP 404).", SyncEntryStatus.FAILED),
            ),
          today = TodayStats(0, 0, 0),
          events = emptyList(),
          languageTag = "en",
        )
      }
    }
    compose.onNodeWithTag("activity_sync_log").assertIsDisplayed()
    compose.onNodeWithText("Last synced: 17 Nov, 09:30").assertIsDisplayed()
    compose.onNodeWithText("3 waiting to sync").assertIsDisplayed()
    compose.onAllNodesWithTag("activity_flagged_entry").assertCountEquals(2)
    compose.onNodeWithText("The PMS refused this entry (HTTP 403).").performScrollTo()
      .assertIsDisplayed()
    compose.onNodeWithText("The PMS has no such room (HTTP 404).").performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `activity sync log shows the clean state honestly`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
          lastSyncedText = null,
          pendingCount = 0,
          flagged = emptyList(),
          today = TodayStats(0, 0, 0),
          events = emptyList(),
          languageTag = "en",
        )
      }
    }
    compose.onNodeWithText("Not synced yet").assertIsDisplayed()
    compose.onNodeWithText("0 waiting to sync").assertIsDisplayed()
    compose.onNodeWithText("Nothing parked — every push went through.").assertIsDisplayed()
  }

  @Test
  fun `activity offline panel lists today stats`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
          lastSyncedText = null,
          pendingCount = 0,
          flagged = emptyList(),
          today = TodayStats(cleaned = 2, tickets = 1, synced = 5),
          events = emptyList(),
          languageTag = "en",
        )
      }
    }
    compose.onNodeWithTag("activity_offline").performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag("activity_today_cleaned").assertIsDisplayed()
    compose.onNodeWithText("2 rooms cleaned").assertIsDisplayed()
    compose.onNodeWithText("1 ticket raised").assertIsDisplayed()
    compose.onNodeWithText("5 changes synced").assertIsDisplayed()
  }

  @Test
  fun `activity offline panel shows the empty line when nothing done`() {
    compose.setContent {
      RoomReadyTheme {
        ActivityScreen(
          lastSyncedText = null,
          pendingCount = 0,
          flagged = emptyList(),
          today = TodayStats(0, 0, 0),
          events = emptyList(),
          languageTag = "en",
        )
      }
    }
    compose.onNodeWithText("Nothing yet today — it will show up here.").performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `profile shows address masked key and version`() {
    compose.setContent {
      RoomReadyTheme {
        ProfileScreen(
          propertyName = "Krishnachura Retreat",
          pmsAddress = "https://pms.demo13.example",
          versionName = "1.0",
          staff = StaffMember("Ayesha", StaffRole.HOUSEKEEPER),
          maskedKey = "••••9f2a",
          lastSyncedText = null,
          syncing = false,
          languageTag = "en",
          onLanguageSelected = {},
          onEditConnection = {},
          onSyncNow = {},
          onDisconnect = {},
        )
      }
    }
    compose.onNodeWithText("Krishnachura Retreat").assertIsDisplayed()
    compose.onNodeWithText("https://pms.demo13.example").assertIsDisplayed()
    compose.onNodeWithText("••••9f2a").assertIsDisplayed()
    compose.onNodeWithText("1.0").assertIsDisplayed()
  }

  @Test
  fun `profile disconnect asks for confirmation and cancel keeps the session`() {
    var disconnected = false
    compose.setContent {
      RoomReadyTheme {
        ProfileScreen(
          propertyName = null,
          pmsAddress = null,
          versionName = null,
          staff = null,
          maskedKey = "",
          lastSyncedText = null,
          syncing = false,
          languageTag = "en",
          onLanguageSelected = {},
          onEditConnection = {},
          onSyncNow = {},
          onDisconnect = { disconnected = true },
        )
      }
    }
    compose.onNodeWithTag("profile_disconnect").performScrollTo().performClick()
    compose.onNodeWithText("Disconnect from the PMS?").assertIsDisplayed()
    assertFalse(disconnected)
    compose.onNodeWithText("Cancel").performClick()
    compose.onNodeWithText("Disconnect from the PMS?").assertDoesNotExist()
    assertFalse(disconnected)
  }

  @Test
  fun `profile disconnect confirm fires exactly once`() {
    var disconnects = 0
    compose.setContent {
      RoomReadyTheme {
        ProfileScreen(
          propertyName = null,
          pmsAddress = null,
          versionName = null,
          staff = null,
          maskedKey = "",
          lastSyncedText = null,
          syncing = false,
          languageTag = "en",
          onLanguageSelected = {},
          onEditConnection = {},
          onSyncNow = {},
          onDisconnect = { disconnects++ },
        )
      }
    }
    compose.onNodeWithTag("profile_disconnect").performScrollTo().performClick()
    compose.onNodeWithTag("profile_disconnect_yes").performClick()
    compose.onNodeWithText("Disconnect from the PMS?").assertDoesNotExist()
    assertTrue(disconnects == 1)
  }

  private fun parkedEntry(id: Long, reason: String, status: SyncEntryStatus = SyncEntryStatus.PARKED) =
    SyncMutationEntity(
      id = id,
      entityType = SyncEntityType.UNIT_STATUS,
      entityId = "u-101",
      op = SyncOp.UPSERT,
      payloadJson = "{}",
      createdAt = 1L,
      dueAt = 1L,
      status = status,
      updatedAt = 1L,
      lastError = reason,
    )
}
