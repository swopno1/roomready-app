package com.vivereply.roomready.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.CleaningEvidence
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.ui.navigation.RoomReadyDestination
import com.vivereply.roomready.ui.screens.ActivityScreen
import com.vivereply.roomready.ui.screens.HomeDashboardScreen
import com.vivereply.roomready.ui.screens.HomeDashboardUiState
import com.vivereply.roomready.ui.screens.InspectScreen
import com.vivereply.roomready.ui.screens.InspectUiState
import com.vivereply.roomready.ui.screens.ProfileScreen
import com.vivereply.roomready.ui.screens.RoomsBoardScreen
import com.vivereply.roomready.ui.screens.RoomsBoardUiState
import com.vivereply.roomready.ui.screens.RoomDetailScreen
import com.vivereply.roomready.ui.screens.RoomDetailUiState
import com.vivereply.roomready.ui.screens.SetupFormState
import com.vivereply.roomready.ui.screens.SetupScreen
import com.vivereply.roomready.ui.screens.TabEmptyStateScreen
import com.vivereply.roomready.ui.screens.TicketFormScreen
import com.vivereply.roomready.ui.screens.TicketFormUiState
import com.vivereply.roomready.ui.screens.TicketsScreen
import com.vivereply.roomready.ui.state.BoardFilter
import com.vivereply.roomready.ui.state.SyncStatusUi
import com.vivereply.roomready.ui.state.TodayStats
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every tab plus room detail composes cleanly at 1.3x and 1.5x font
 * scale: no crashed layout, no missing content.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FontScaleTest {

  @get:Rule val compose = createComposeRule()

  private val units =
    listOf(
      RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
      RoomUnit("u-102", "102", "rt-t", "1", Occupancy.OCCUPIED, HousekeepingState.READY, 1L),
    )
  private val types = mapOf("rt-t" to RoomType("rt-t", "TERRA", "Terracotta Villa", "টেরাকোটা ভিলা", "1 king", 2))

  @Test fun `home survives 1_3x`() = checkHome(1.3f)

  @Test fun `home survives 1_5x`() = checkHome(1.5f)

  @Test fun `rooms survive 1_3x`() = checkRooms(1.3f)

  @Test fun `rooms survive 1_5x`() = checkRooms(1.5f)

  @Test fun `tickets survive 1_3x`() = checkTickets(1.3f)

  @Test fun `tickets survive 1_5x`() = checkTickets(1.5f)

  @Test fun `activity survives 1_3x`() = checkActivity(1.3f)

  @Test fun `activity survives 1_5x`() = checkActivity(1.5f)

  @Test fun `profile survives 1_3x`() = checkProfile(1.3f)

  @Test fun `profile survives 1_5x`() = checkProfile(1.5f)

  @Test fun `detail survives 1_3x`() = checkDetail(1.3f)

  @Test fun `detail survives 1_5x`() = checkDetail(1.5f)

  @Test fun `setup survives 1_3x`() = checkSetup(1.3f)

  @Test fun `setup survives 1_5x`() = checkSetup(1.5f)

  @Test fun `inspect survives 1_3x`() = checkInspect(1.3f)

  @Test fun `inspect survives 1_5x`() = checkInspect(1.5f)

  @Test fun `ticket form survives 1_3x`() = checkTicketForm(1.3f)

  @Test fun `ticket form survives 1_5x`() = checkTicketForm(1.5f)

  @Test fun `empty state survives 1_3x`() = checkEmpty(1.3f)

  @Test fun `empty state survives 1_5x`() = checkEmpty(1.5f)

  private fun checkHome(scale: Float) {
    renderAt(scale) {
      HomeDashboardScreen(
        state =
          HomeDashboardUiState(
            status = SyncStatusUi(true, null, 1, false, null),
            staffName = "Ayesha",
            hourOfDay = 9,
            units = units,
            types = types,
            role = StaffRole.HOUSEKEEPER,
          ),
        languageTag = "en",
        onSyncNow = {},
        onFilterSelected = {},
        onAction = { _, _ -> },
        onSendBack = {},
        onCardClick = {},
      )
    }
    compose.onNodeWithTag("home_hero").assertExists()
    compose.onNodeWithText("Needs attention").assertExists()
  }

  private fun checkRooms(scale: Float) {
    renderAt(scale) {
      RoomsBoardScreen(
        state =
          RoomsBoardUiState(
            units = units,
            types = types,
            filter = BoardFilter.ALL,
            role = StaffRole.HOUSEKEEPER,
          ),
        languageTag = "en",
        onFilter = {},
        onAction = { _, _ -> },
        onSendBack = {},
        onCardClick = {},
        onRetry = {},
      )
    }
    compose.onNodeWithTag("board_filters").assertExists()
    compose.onNodeWithText("Terracotta Villa").assertExists()
  }

  private fun checkTickets(scale: Float) {
    renderAt(scale) {
      TicketsScreen(
        tickets =
          listOf(
            Ticket(
              id = "t-1",
              unitId = "u-101",
              roomNumber = "101",
              category = TicketCategory.PLUMBING,
              note = "Tap leaks",
              photoRef = null,
              status = TicketStatus.OPEN,
              createdBy = "Ayesha",
              createdAtEpochMillis = 1_000L,
              updatedAtEpochMillis = 1_000L,
            ),
          ),
        nowEpochMillis = 7_201_000L,
        doneError = null,
        onDone = {},
        onNewTicket = {},
      )
    }
    compose.onNodeWithTag("ticket_list").assertExists()
    compose.onNodeWithText("Tap leaks").assertExists()
  }

  private fun checkActivity(scale: Float) {
    renderAt(scale) {
      ActivityScreen(
        lastSyncedText = "17 Nov, 09:30",
        pendingCount = 2,
        flagged = emptyList(),
        today = TodayStats(cleaned = 1, tickets = 1, synced = 1),
        events =
          listOf(
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
    compose.onNodeWithTag("activity_sync_log").assertExists()
    compose.onNodeWithTag("activity_offline").assertExists()
  }

  private fun checkProfile(scale: Float) {
    renderAt(scale) {
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
    compose.onNodeWithTag("profile_connected").assertExists()
    compose.onNodeWithTag("profile_disconnect").assertExists()
  }

  private fun checkDetail(scale: Float) {
    renderAt(scale) {
      RoomDetailScreen(
        state =
          RoomDetailUiState(
            unit = units[0].copy(hkState = HousekeepingState.CLEANING),
            typeName = "Terracotta Villa",
            progress = ChecklistProgress("u-101", setOf(TurnoverStep.BED_LINEN)),
            role = StaffRole.HOUSEKEEPER,
            nowEpochMillis = 3_661_000L,
          ),
        onBack = {},
        onStart = {},
        onInspect = {},
        onSendBack = {},
        onRaiseTicket = {},
        onToggleStep = { _, _ -> },
        onOpenFinish = {},
        onTakePhoto = {},
        onRetakePhoto = {},
        onConfirmFinish = {},
        onSheetDismiss = {},
        onFinishDone = {},
      )
    }
    compose.onNodeWithTag("detail_checklist").assertExists()
    compose.onNodeWithText("Change bed linen").assertExists()
  }

  private fun checkSetup(scale: Float) {
    renderAt(scale) {
      SetupScreen(
        state = SetupFormState(),
        onAddressChange = {},
        onApiKeyChange = {},
        onStaffNameChange = {},
        onConnect = {},
        onScanQr = {},
        onPasteCode = {},
      )
    }
    compose.onNodeWithText("Connect to your PMS").assertExists()
    compose.onNodeWithTag("setup_connect").assertExists()
  }

  private fun checkInspect(scale: Float) {
    renderAt(scale) {
      InspectScreen(
        state =
          InspectUiState(
            unit = units[0].copy(hkState = HousekeepingState.CLEANED),
            typeName = "Terracotta Villa",
            progress = ChecklistProgress("u-101", TurnoverStep.entries.toSet()),
            evidence =
              CleaningEvidence(
                unitId = "u-101",
                photoRef = null,
                skipReason = "Too dark",
                finishedBy = "Ayesha",
                finishedAtEpochMillis = 1_000L,
              ),
            role = StaffRole.SUPERVISOR,
          ),
        onBack = {},
        onPass = {},
        onSendBack = {},
      )
    }
    compose.onNodeWithTag("inspect_evidence").assertExists()
    compose.onNodeWithText("Pass").assertExists()
  }

  private fun checkTicketForm(scale: Float) {
    renderAt(scale) {
      TicketFormScreen(
        state = TicketFormUiState(units = units, unitId = "u-101"),
        onUnit = {},
        onPickRoom = {},
        onCategory = {},
        onNote = {},
        onAddPhoto = {},
        onClearPhoto = {},
        onRaise = {},
        onBack = {},
      )
    }
    compose.onNodeWithText("New ticket").assertExists()
    compose.onNodeWithTag("ticket_cat_plumbing").assertExists()
  }

  private fun checkEmpty(scale: Float) {
    renderAt(scale) {
      TabEmptyStateScreen(
        destination = RoomReadyDestination.Profile,
        currentLanguageTag = "en",
        onLanguageSelected = {},
        onPrimaryAction = {},
      )
    }
    compose.onNodeWithText("Set up your housekeeper profile").assertExists()
    compose.onNodeWithTag("lang_chip_bn").assertExists()
  }

  private fun renderAt(scale: Float, content: @Composable () -> Unit) {
    compose.setContent {
      RoomReadyTheme {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
          content()
        }
      }
    }
  }
}
