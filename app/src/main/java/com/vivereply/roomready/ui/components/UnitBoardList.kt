package com.vivereply.roomready.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** Semantic status chip; colours never change with branding. Cross-fades on change. */
@Composable
fun StatusChip(state: HousekeepingState, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val radius = RoomReadyThemeTokens.radius
  val label =
    stringResource(
      when (state) {
        HousekeepingState.DIRTY -> R.string.status_dirty
        HousekeepingState.CLEANING -> R.string.status_cleaning
        HousekeepingState.CLEANED -> R.string.status_cleaned
        HousekeepingState.READY -> R.string.status_ready
      },
    )
  val (containerTarget, contentTarget) =
    when (state) {
      HousekeepingState.DIRTY -> colors.statusDirtyContainer to colors.onStatusDirtyContainer
      HousekeepingState.CLEANING -> colors.statusCleaningContainer to colors.onStatusCleaningContainer
      HousekeepingState.CLEANED -> colors.statusCleanedContainer to colors.onStatusCleanedContainer
      HousekeepingState.READY -> colors.statusReadyContainer to colors.onStatusReadyContainer
    }
  val container by androidx.compose.animation.animateColorAsState(containerTarget, label = "chipBg")
  val content by androidx.compose.animation.animateColorAsState(contentTarget, label = "chipFg")
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(radius.pill),
    color = container,
    contentColor = content,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = RoomReadyThemeTokens.spacing.sm, vertical = RoomReadyThemeTokens.spacing.xs),
    )
  }
}

@Composable
fun occupancyLabel(occupancy: Occupancy): String =
  stringResource(
    when (occupancy) {
      Occupancy.VACANT -> R.string.occ_vacant
      Occupancy.OCCUPIED -> R.string.occ_occupied
      Occupancy.OUT_OF_ORDER -> R.string.occ_out_of_order
      Occupancy.UNKNOWN -> R.string.occ_unknown
    },
  )

