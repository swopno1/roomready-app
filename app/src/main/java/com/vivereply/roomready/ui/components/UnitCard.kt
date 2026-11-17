package com.vivereply.roomready.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.state.UnitAction
import com.vivereply.roomready.ui.state.canSendBack
import com.vivereply.roomready.ui.state.primaryActionFor
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.delay

/** Compact relative duration: 45m, 5h, 3d. */
@Composable
fun timeInStateText(sinceEpochMillis: Long, nowEpochMillis: Long): String =
  durationText(nowEpochMillis - sinceEpochMillis)

/** Formats a millisecond span with the same compact units. */
@Composable
fun durationText(totalMillis: Long): String {
  val minutes = (totalMillis / 60_000L).coerceAtLeast(0L)
  return when {
    minutes < 60L -> stringResource(R.string.time_minutes, minutes.toInt())
    minutes < 2_880L -> stringResource(R.string.time_hours, (minutes / 60L).toInt())
    else -> stringResource(R.string.time_days, (minutes / 1_440L).toInt())
  }
}

/**
 * Board unit card: semantic left rail + chip, time-in-state, and the
 * role-gated primary action. Rail and chip cross-fade on state change with
 * a subtle scale pulse; list position never moves, so no jumps.
 */
@Composable
fun UnitCard(
  unit: RoomUnit,
  typeName: String,
  nowEpochMillis: Long,
  role: StaffRole?,
  busy: Boolean,
  errorRes: Int?,
  onAction: (UnitAction, RoomUnit) -> Unit,
  onSendBack: (RoomUnit) -> Unit,
  onCardClick: (RoomUnit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  var firstSeen by remember(unit.id) { mutableStateOf(true) }
  var pulse by remember(unit.id) { mutableStateOf(false) }
  LaunchedEffect(unit.hkState) {
    if (firstSeen) {
      firstSeen = false
    } else {
      pulse = true
      delay(180L)
      pulse = false
    }
  }
  val scale by animateFloatAsState(if (pulse) 1.03f else 1f, label = "cardPulse")

  val statusText = statusLabel(unit.hkState)
  val rowCd = stringResource(R.string.rooms_row_cd, unit.number, statusText)
  val action = primaryActionFor(unit.hkState, role)

  Surface(
    onClick = { onCardClick(unit) },
    modifier =
      modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .semantics { contentDescription = rowCd }
        .testTag("unit_card_${unit.id}"),
    shape = RoundedCornerShape(radius.lg),
    color = colors.surface,
    shadowElevation = RoomReadyThemeTokens.elevation.card,
  ) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      StatusRail(state = unit.hkState)
      Column(
        modifier = Modifier.weight(1f).padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = unit.number,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
          )
          StatusChip(state = unit.hkState)
        }
        Text(
          text = "$typeName • ${occupancyLabel(unit.occupancy)}",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.inkSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.inkMuted,
          )
          Spacer(modifier = Modifier.width(spacing.xxs))
          val duration = timeInStateText(unit.updatedAtEpochMillis, nowEpochMillis)
          Text(
            text =
              if (unit.hkState == HousekeepingState.CLEANING) {
                stringResource(R.string.cleaning_for, duration)
              } else {
                duration
              },
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
          )
        }
        errorRes?.let {
          Text(
            text = stringResource(it),
            style = MaterialTheme.typography.labelMedium,
            color = colors.statusDirty,
            modifier = Modifier.testTag("unit_error_${unit.id}"),
          )
        }
        if (action != null) {
          Spacer(modifier = Modifier.height(spacing.xs))
          Button(
            onClick = { onAction(action, unit) },
            enabled = !busy,
            modifier =
              Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumInteractiveComponentSize()
                .testTag("unit_action_${unit.id}"),
            shape = RoundedCornerShape(radius.md),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = colors.ink,
                contentColor = colors.onInk,
              ),
          ) {
            if (busy) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.mintSolid,
              )
              Spacer(modifier = Modifier.width(spacing.xs))
            }
            Text(text = stringResource(action.labelRes), style = MaterialTheme.typography.labelLarge)
          }
        }
        if (canSendBack(unit.hkState, role)) {
          TextButton(
            onClick = { onSendBack(unit) },
            enabled = !busy,
            modifier =
              Modifier.minimumInteractiveComponentSize().align(Alignment.End)
                .testTag("unit_sendback_${unit.id}"),
          ) {
            Text(
              text = stringResource(R.string.action_send_back),
              style = MaterialTheme.typography.labelLarge,
              color = colors.statusDirty,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun statusLabel(state: HousekeepingState): String =
  stringResource(
    when (state) {
      HousekeepingState.DIRTY -> R.string.status_dirty
      HousekeepingState.CLEANING -> R.string.status_cleaning
      HousekeepingState.CLEANED -> R.string.status_cleaned
      HousekeepingState.READY -> R.string.status_ready
    },
  )

/** Bold semantic rail; colour cross-fades on state change. */
@Composable
private fun StatusRail(state: HousekeepingState, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val radius = RoomReadyThemeTokens.radius
  val target =
    when (state) {
      HousekeepingState.DIRTY -> colors.statusDirty
      HousekeepingState.CLEANING -> colors.statusCleaning
      HousekeepingState.CLEANED -> colors.statusCleaned
      HousekeepingState.READY -> colors.statusReadyAccessible
    }
  val animated by animateColorAsState(target, label = "railColor")
  Box(
    modifier =
      modifier
        .width(8.dp)
        .fillMaxHeight()
        .clip(RoundedCornerShape(topStart = radius.lg, bottomStart = radius.lg))
        .background(animated),
  )
}
