package com.vivereply.roomready.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** Semantic status chip; colours never change with branding. */
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
  val (container, content) =
    when (state) {
      HousekeepingState.DIRTY -> colors.statusDirtyContainer to colors.onStatusDirtyContainer
      HousekeepingState.CLEANING -> colors.statusCleaningContainer to colors.onStatusCleaningContainer
      HousekeepingState.CLEANED -> colors.statusCleanedContainer to colors.onStatusCleanedContainer
      HousekeepingState.READY -> colors.statusReadyContainer to colors.onStatusReadyContainer
    }
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

/** Board list of room units shared by Home and Rooms. */
@Composable
fun UnitBoardList(
  units: List<RoomUnit>,
  typeNames: Map<String, String>,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(spacing.sm),
  ) {
    items(units, key = { it.id }) { unit ->
      val typeName = typeNames[unit.roomTypeId] ?: unit.roomTypeId
      val statusText =
        when (unit.hkState) {
          HousekeepingState.DIRTY -> stringResource(R.string.status_dirty)
          HousekeepingState.CLEANING -> stringResource(R.string.status_cleaning)
          HousekeepingState.CLEANED -> stringResource(R.string.status_cleaned)
          HousekeepingState.READY -> stringResource(R.string.status_ready)
        }
      val rowCd = stringResource(R.string.rooms_row_cd, unit.number, statusText)
      Surface(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = rowCd },
        shape = RoundedCornerShape(radius.md),
        color = colors.surface,
        shadowElevation = RoomReadyThemeTokens.elevation.card,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(spacing.md),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
              text = unit.number,
              style = MaterialTheme.typography.titleLarge,
              color = colors.ink,
            )
            Text(
              text = "$typeName • ${occupancyLabel(unit.occupancy)}",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.inkSecondary,
            )
          }
          StatusChip(state = unit.hkState)
        }
      }
    }
  }
}
