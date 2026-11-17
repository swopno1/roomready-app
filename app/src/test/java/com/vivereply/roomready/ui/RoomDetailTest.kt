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
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.ui.screens.FinishSheetContent
import com.vivereply.roomready.ui.screens.RoomDetailScreen
import com.vivereply.roomready.ui.screens.RoomDetailUiState
import com.vivereply.roomready.ui.state.FinishSuccess
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Room detail: checklist, finish gating, evidence rules, success summary. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDetailTest {

  @get:Rule val compose = createComposeRule()

  private val cleaningUnit =
    RoomUnit("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.CLEANING, 1L)

  @Test
  fun `checklist renders all 8 verbatim steps and toggles`() {
    var toggled: Pair<TurnoverStep, Boolean>? = null
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleaningUnit,
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
          onToggleStep = { step, done -> toggled = step to done },
          onOpenFinish = {},
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirmFinish = {},
          onSheetDismiss = {},
          onFinishDone = {},
        )
      }
    }
    compose.onNodeWithTag("detail_checklist").assertIsDisplayed()
    compose.onNodeWithText("Change bed linen").assertIsDisplayed()
    scrollDetailTo("check_step_final_walkthrough")
    compose.onNodeWithText("Final walkthrough").assertExists()
    compose.onNodeWithTag("checklist_progress").assertTextContains("1 of 8 done", substring = true)

    scrollDetailTo("check_step_vacuum_mop")
    compose.onNodeWithTag("check_step_vacuum_mop").performClick()
    assertEquals(TurnoverStep.VACUUM_MOP to true, toggled)
    scrollDetailTo("check_step_bed_linen")
    compose.onNodeWithTag("check_step_bed_linen").performClick()
    assertEquals(TurnoverStep.BED_LINEN to false, toggled)
  }

  private fun scrollDetailTo(tag: String) {
    compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `finish stays disabled until all 8 steps are done`() {
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleaningUnit,
              progress = ChecklistProgress("u-101", setOf(TurnoverStep.BED_LINEN)),
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
    compose.onNodeWithTag("detail_finish").assertIsDisplayed().assertIsNotEnabled()
  }

  @Test
  fun `finish enables when the checklist is complete`() {
    var opened = false
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleaningUnit,
              progress = ChecklistProgress("u-101", TurnoverStep.entries.toSet()),
              role = StaffRole.HOUSEKEEPER,
            ),
          onBack = {},
          onStart = {},
          onInspect = {},
          onSendBack = {},
          onRaiseTicket = {},
          onToggleStep = { _, _ -> },
          onOpenFinish = { opened = true },
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirmFinish = {},
          onSheetDismiss = {},
          onFinishDone = {},
        )
      }
    }
    compose.onNodeWithTag("detail_finish").assertIsDisplayed().assertIsEnabled().performClick()
    assertTrue(opened)
  }

  @Test
  fun `dirty room shows start and cleaning elapsed on header`() {
    compose.setContent {
      RoomReadyTheme {
        RoomDetailScreen(
          state =
            RoomDetailUiState(
              unit = cleaningUnit,
              typeName = "Terracotta Villa",
              progress = ChecklistProgress("u-101", emptySet()),
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
    compose.onNodeWithTag("detail_header").assertIsDisplayed()
    compose.onNodeWithTag("detail_elapsed").assertTextContains("Cleaning for", substring = true)
  }

  @Test
  fun `finish confirm is impossible without photo and without reason`() {
    var confirmed: String? = "untouched"
    compose.setContent {
      RoomReadyTheme {
        FinishSheetContent(
          hasPhoto = false,
          photo = null,
          busy = false,
          errorRes = null,
          finishResult = null,
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirm = { confirmed = it },
          onDone = {},
        )
      }
    }
    scrollSheetTo("finish_confirm")
    compose.onNodeWithTag("finish_confirm").assertIsNotEnabled()
    assertEquals("untouched", confirmed)
  }

  private fun scrollSheetTo(tag: String) {
    compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `preset reason enables finish confirm`() {
    var confirmed: String? = null
    compose.setContent {
      RoomReadyTheme {
        FinishSheetContent(
          hasPhoto = false,
          photo = null,
          busy = false,
          errorRes = null,
          finishResult = null,
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirm = { confirmed = it },
          onDone = {},
        )
      }
    }
    scrollSheetTo("finish_preset_0")
    compose.onNodeWithTag("finish_preset_0").performClick()
    scrollSheetTo("finish_confirm")
    compose.onNodeWithTag("finish_confirm").assertIsEnabled().performClick()
    assertEquals("Camera not working", confirmed)
  }

  @Test
  fun `success moment shows room and time taken`() {
    var done = false
    compose.setContent {
      RoomReadyTheme {
        FinishSheetContent(
          hasPhoto = true,
          photo = null,
          busy = false,
          errorRes = null,
          finishResult = FinishSuccess("101", 2_700_000L),
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirm = {},
          onDone = { done = true },
        )
      }
    }
    compose.onNodeWithTag("finish_success").assertIsDisplayed()
    compose.onNodeWithText("Room 101 is clean").assertIsDisplayed()
    compose.onNodeWithText("Took 45m", substring = true).assertIsDisplayed()
    compose.onNodeWithTag("finish_done").performClick()
    assertTrue(done)
  }

  @Test
  fun `evidence error renders in the sheet`() {
    compose.setContent {
      RoomReadyTheme {
        FinishSheetContent(
          hasPhoto = false,
          photo = null,
          busy = false,
          errorRes = R.string.action_err_evidence,
          finishResult = null,
          onTakePhoto = {},
          onRetakePhoto = {},
          onConfirm = {},
          onDone = {},
        )
      }
    }
    compose.onNodeWithTag("finish_error").assertTextContains("Add a photo", substring = true)
  }
}
