package com.vivereply.roomready.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.R
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
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every tab renders fully under the Bengali locale: a missing string or
 * plural would crash the render, so a passing render is the proof.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BengaliTabsTest {

  @get:Rule val compose = createComposeRule()

  private val units =
    listOf(
      RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
      RoomUnit("u-102", "102", "rt-t", "1", Occupancy.OCCUPIED, HousekeepingState.READY, 1L),
    )
  private val types = mapOf("rt-t" to RoomType("rt-t", "TERRA", "Terracotta Villa", "টেরাকোটা ভিলা", "1 king", 2))

  @Test
  fun `home renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          HomeDashboardScreen(
            state =
              HomeDashboardUiState(
                status =
                  SyncStatusUi(
                    connected = true,
                    lastSyncedAt = null,
                    pendingCount = 1,
                    syncing = false,
                    error = null,
                  ),
                staffName = "আয়েশা",
                hourOfDay = 9,
                units = units,
                types = types,
                role = StaffRole.HOUSEKEEPER,
              ),
            languageTag = "bn",
            onSyncNow = {},
            onFilterSelected = {},
            onAction = { _, _ -> },
            onSendBack = {},
            onCardClick = {},
          )
        }
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.home_needs_attention)).assertCountEquals(1)
    compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_last_synced"))
    compose.onAllNodesWithText(bn.getString(R.string.sync_never)).assertCountEquals(1)
  }

  @Test
  fun `rooms board renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          RoomsBoardScreen(
            state =
              RoomsBoardUiState(
                units = units,
                types = types,
                filter = BoardFilter.ALL,
                role = StaffRole.HOUSEKEEPER,
              ),
            languageTag = "bn",
            onFilter = {},
            onAction = { _, _ -> },
            onSendBack = {},
            onCardClick = {},
            onRetry = {},
          )
        }
      }
    }
    compose.onAllNodesWithText("টেরাকোটা ভিলা").assertCountEquals(1)
  }

  @Test
  fun `tickets render fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          TicketsScreen(
            tickets =
              listOf(
                Ticket(
                  id = "t-1",
                  unitId = "u-101",
                  roomNumber = "101",
                  category = TicketCategory.PLUMBING,
                  note = "কল থেকে পানি পড়ে",
                  photoRef = null,
                  status = TicketStatus.OPEN,
                  createdBy = "আয়েশা",
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
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.tickets_open_title)).assertCountEquals(1)
    compose.onAllNodesWithText("কল থেকে পানি পড়ে").assertCountEquals(1)
  }

  @Test
  fun `activity renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          ActivityScreen(
            lastSyncedText = null,
            pendingCount = 1,
            flagged = emptyList(),
            today = TodayStats(cleaned = 1, tickets = 1, synced = 1),
            events =
              listOf(
                ActivityEvent(
                  id = 1L,
                  type = ActivityType.STATUS_CHANGED,
                  actorName = "আয়েশা",
                  role = null,
                  unitId = "u-101",
                  roomNumber = "101",
                  detail = "DIRTY>CLEANING",
                  createdAtEpochMillis = 1_000L,
                ),
              ),
            languageTag = "bn",
          )
        }
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.activity_sync_log)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.activity_offline_title)).assertCountEquals(1)
    compose.onNodeWithTag("activity_list")
      .performScrollToNode(hasText(bn.getString(R.string.activity_t_status_changed)))
    compose.onAllNodesWithText(bn.getString(R.string.activity_t_status_changed)).assertCountEquals(1)
  }

  @Test
  fun `profile renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          ProfileScreen(
            propertyName = "কৃষ্ণচূড়া রিট্রিট",
            pmsAddress = "https://pms.demo13.example",
            versionName = "1.0",
            staff = StaffMember("আয়েশা", StaffRole.HOUSEKEEPER),
            maskedKey = "••••9f2a",
            lastSyncedText = null,
            syncing = false,
            languageTag = "bn",
            onLanguageSelected = {},
            onEditConnection = {},
            onSyncNow = {},
            onDisconnect = {},
          )
        }
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.profile_disconnect)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.profile_version)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.profile_pms_address)).assertCountEquals(1)
  }

  @Test
  fun `setup renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
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
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.setup_title)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.setup_connect)).assertCountEquals(1)
  }

  @Test
  fun `detail renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          RoomDetailScreen(
            state =
              RoomDetailUiState(
                unit = units[0].copy(hkState = HousekeepingState.CLEANING),
                typeName = "টেরাকোটা ভিলা",
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
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.checklist_step_bed_linen)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.checklist_step_final_walkthrough)).assertCountEquals(1)
  }

  @Test
  fun `inspect renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          InspectScreen(
            state =
              InspectUiState(
                unit = units[0].copy(hkState = HousekeepingState.CLEANED),
                typeName = "টেরাকোটা ভিলা",
                progress = ChecklistProgress("u-101", TurnoverStep.entries.toSet()),
                evidence =
                  CleaningEvidence(
                    unitId = "u-101",
                    photoRef = null,
                    skipReason = "অন্ধকার",
                    finishedBy = "আয়েশা",
                    finishedAtEpochMillis = 1_000L,
                  ),
                role = StaffRole.SUPERVISOR,
              ),
            onBack = {},
            onPass = {},
            onSendBack = {},
          )
        }
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.inspect_checklist)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.inspect_pass)).assertCountEquals(1)
  }

  @Test
  fun `ticket form renders fully in Bengali`() {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
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
      }
    }
    compose.onAllNodesWithText(bn.getString(R.string.ticket_new_title)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(R.string.ticket_category_plumbing)).assertCountEquals(1)
  }

  @Test
  fun `home empty state renders fully in Bengali`() =
    checkEmptyState(RoomReadyDestination.Home)

  @Test
  fun `rooms empty state renders fully in Bengali`() =
    checkEmptyState(RoomReadyDestination.Rooms)

  @Test
  fun `tickets empty state renders fully in Bengali`() =
    checkEmptyState(RoomReadyDestination.Tickets)

  @Test
  fun `activity empty state renders fully in Bengali`() =
    checkEmptyState(RoomReadyDestination.Activity)

  @Test
  fun `profile empty state renders fully in Bengali`() =
    checkEmptyState(RoomReadyDestination.Profile)

  private fun checkEmptyState(destination: RoomReadyDestination) {
    val bn = localizedBn()
    compose.setContent {
      CompositionLocalProvider(
        LocalContext provides bn,
        LocalConfiguration provides bn.resources.configuration,
      ) {
        RoomReadyTheme {
          TabEmptyStateScreen(
            destination = destination,
            currentLanguageTag = "bn",
            onLanguageSelected = {},
            onPrimaryAction = {},
          )
        }
      }
    }
    compose.onAllNodesWithText(bn.getString(destination.emptyTitleRes)).assertCountEquals(1)
    compose.onAllNodesWithText(bn.getString(destination.emptyActionRes)).assertCountEquals(1)
  }

  private fun localizedBn(): Context {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(base.resources.configuration)
    config.setLocale(Locale.forLanguageTag("bn"))
    return base.createConfigurationContext(config)
  }
}
