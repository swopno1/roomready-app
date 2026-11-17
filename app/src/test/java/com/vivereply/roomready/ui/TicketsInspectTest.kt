package com.vivereply.roomready.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.CleaningEvidence
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.ui.screens.InspectScreen
import com.vivereply.roomready.ui.screens.InspectUiState
import com.vivereply.roomready.ui.screens.RoomDetailScreen
import com.vivereply.roomready.ui.screens.RoomDetailUiState
import com.vivereply.roomready.ui.screens.RoomPickerDialog
import com.vivereply.roomready.ui.screens.TicketFormScreen
import com.vivereply.roomready.ui.screens.TicketFormUiState
import com.vivereply.roomready.ui.screens.TicketsScreen
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Inspection, tickets, and role gating at the UI layer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TicketsInspectTest {

  @get:Rule val compose = createComposeRule()

  private val cleanedUnit =
    RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.CLEANED, 1L)
  private val dirtyUnit =
    RoomUnit("u-102", "102", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L)

  @Test
  fun `housekeeper never sees inspect actions on detail`() {
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleanedUnit,
              typeName = "Terracotta Villa",
              role = StaffRole.HOUSEKEEPER,
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
    compose.onNodeWithTag("detail_primary").assertDoesNotExist()
    compose.onNodeWithTag("detail_sendback").assertDoesNotExist()
  }

  @Test
  fun `supervisor sees inspect and send-back on a cleaned room`() {
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleanedUnit,
              typeName = "Terracotta Villa",
              role = StaffRole.SUPERVISOR,
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
    compose.onNodeWithTag("detail_primary").assertIsDisplayed()
    compose.onNodeWithTag("detail_sendback").assertIsDisplayed()
  }

  @Test
  fun `sent-back reason shows on the dirty room`() {
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = dirtyUnit,
              sentBackReason = "streaks on mirror",
              role = StaffRole.HOUSEKEEPER,
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
    compose.onNodeWithTag("detail_sentback").assertIsDisplayed()
      .assertTextContains("streaks on mirror", substring = true)
  }

  @Test
  fun `inspect screen shows checklist evidence and pass`() {
    var passed = false
    compose.setContent {
      RoomReadyTheme {
        InspectScreen(
          state =
            InspectUiState(
              unit = cleanedUnit,
              typeName = "Terracotta Villa",
              progress = ChecklistProgress("u-101", TurnoverStep.entries.toSet()),
              evidence =
                CleaningEvidence("u-101", null, "too dark", "Ayesha", 2_000L),
              role = StaffRole.SUPERVISOR,
            ),
          onBack = {},
          onPass = { passed = true },
          onSendBack = {},
        )
      }
    }
    compose.onNodeWithTag("inspect_checklist").assertIsDisplayed()
    compose.onNodeWithText("Change bed linen").assertIsDisplayed()
    compose.onNodeWithTag("inspect_evidence").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("too dark", substring = true).assertIsDisplayed()
    compose.onNodeWithTag("inspect_pass").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(passed)
    compose.onNodeWithTag("inspect_sendback").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `inspect screen hides actions from housekeepers`() {
    compose.setContent {
      RoomReadyTheme {
        InspectScreen(
          state =
            InspectUiState(
              unit = cleanedUnit,
              progress = ChecklistProgress("u-101", TurnoverStep.entries.toSet()),
              role = StaffRole.HOUSEKEEPER,
            ),
          onBack = {},
          onPass = {},
          onSendBack = {},
        )
      }
    }
    compose.onNodeWithTag("inspect_checklist").assertIsDisplayed()
    compose.onNodeWithTag("inspect_pass").assertDoesNotExist()
    compose.onNodeWithTag("inspect_sendback").assertDoesNotExist()
  }

  @Test
  fun `ticket form needs room category and note before raise`() {
    var raised = false
    compose.setContent {
      RoomReadyTheme {
        TicketFormScreen(
          state =
            TicketFormUiState(
              units = listOf(cleanedUnit, dirtyUnit),
              unitId = null,
              roomLocked = false,
              category = null,
              note = "",
              canRaise = false,
            ),
          onUnit = {},
          onPickRoom = {},
          onCategory = {},
          onNote = {},
          onAddPhoto = {},
          onClearPhoto = {},
          onRaise = { raised = true },
          onBack = {},
        )
      }
    }
    TicketCategory.entries.forEach { category ->
      compose.onNodeWithTag("ticket_cat_${category.name.lowercase()}").assertIsDisplayed()
    }
    compose.onNodeWithTag("ticket_raise").assertIsDisplayed().assertIsNotEnabled()
    assertTrue(!raised)
  }

  @Test
  fun `completed ticket form raises`() {
    var raised = false
    compose.setContent {
      RoomReadyTheme {
        TicketFormScreen(
          state =
            TicketFormUiState(
              units = listOf(cleanedUnit),
              unitId = "u-101",
              roomLocked = true,
              category = TicketCategory.AIR_CONDITIONING,
              note = "no cooling",
              canRaise = true,
            ),
          onUnit = {},
          onPickRoom = {},
          onCategory = {},
          onNote = {},
          onAddPhoto = {},
          onClearPhoto = {},
          onRaise = { raised = true },
          onBack = {},
        )
      }
    }
    compose.onNodeWithTag("ticket_raise").assertIsEnabled().performClick()
    assertTrue(raised)
  }

  @Test
  fun `room picker lists rooms and picks one`() {
    var picked: String? = null
    compose.setContent {
      RoomReadyTheme {
        RoomPickerDialog(
          units = listOf(cleanedUnit, dirtyUnit),
          onPick = { picked = it.id },
          onDismiss = {},
        )
      }
    }
    compose.onNodeWithTag("ticket_room_u-102").performClick()
    assertEquals("u-102", picked)
  }

  @Test
  fun `ticket card shows age and marks done with one tap`() {
    var doneId: String? = null
    val ticket =
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
      )
    compose.setContent {
      RoomReadyTheme {
        TicketsScreen(
          tickets = listOf(ticket),
          nowEpochMillis = 7_201_000L,
          doneError = null,
          onDone = { doneId = it },
          onNewTicket = {},
        )
      }
    }
    compose.onNodeWithTag("ticket_card_t-1").assertIsDisplayed()
    compose.onNodeWithText("2h old", substring = true).assertIsDisplayed()
    compose.onNodeWithTag("ticket_done_t-1").performClick()
    assertEquals("t-1", doneId)
  }
}
