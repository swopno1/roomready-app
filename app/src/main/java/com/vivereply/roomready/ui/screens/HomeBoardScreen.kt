package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.ui.components.StatusChip
import com.vivereply.roomready.ui.components.UnitBoardList
import com.vivereply.roomready.ui.state.BoardViewModel
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.SyncStatusUi
import com.vivereply.roomready.ui.state.UiDates
import com.vivereply.roomready.ui.state.countByState
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun HomeRoute(
  connection: ConnectionViewModel,
  board: BoardViewModel,
  languageTag: String,
  modifier: Modifier = Modifier,
) {
  val status by connection.status.collectAsStateWithLifecycle()
  val units by board.units.collectAsStateWithLifecycle()
  val typeNames by board.typeNames.collectAsStateWithLifecycle()
  LaunchedEffect(Unit) {
    connection.refresh()
    board.refreshTypes()
  }
  HomeBoardScreen(
    status = status,
    units = units,
    typeNames = typeNames,
    languageTag = languageTag,
    onSyncNow = { connection.syncNow() },
    modifier = modifier,
  )
}

@Composable
fun HomeBoardScreen(
  status: SyncStatusUi,
  units: List<RoomUnit>,
  typeNames: Map<String, String>,
  languageTag: String,
  onSyncNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  val lastSyncedText =
    remember(status.lastSyncedAt, languageTag) {
      status.lastSyncedAt?.let { UiDates.dateTime(languageTag, it) }
    }

  Column(
    modifier = modifier.fillMaxSize().background(colors.canvas).padding(spacing.md),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(radius.lg),
      color = colors.ink,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
      ) {
        Text(
          text =
            lastSyncedText?.let { stringResource(R.string.sync_last_synced, it) }
              ?: stringResource(R.string.sync_never),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onInk,
          modifier = Modifier.testTag("home_last_synced"),
        )
        Text(
          text = pluralStringResource(R.plurals.sync_pending, status.pendingCount, status.pendingCount),
          style = MaterialTheme.typography.titleSmall,
          color = colors.mintSolid,
          modifier = Modifier.testTag("home_pending"),
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Button(
          onClick = onSyncNow,
          enabled = !status.syncing,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
              .testTag("home_sync"),
          shape = RoundedCornerShape(radius.md),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = colors.mintSolid,
              contentColor = colors.ink,
            ),
        ) {
          if (status.syncing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.size(spacing.sm))
            Text(stringResource(R.string.sync_working))
          } else {
            Text(stringResource(R.string.sync_now))
          }
        }
        status.error?.let { error ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(radius.md),
            color = colors.statusDirtyContainer,
          ) {
            Text(
              text = stringResource(error.messageRes),
              style = MaterialTheme.typography.bodyMedium,
              color = colors.onStatusDirtyContainer,
              modifier = Modifier.padding(spacing.sm).testTag("home_sync_error"),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    if (units.isEmpty()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius.lg),
        color = colors.surface,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(spacing.lg),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = stringResource(R.string.home_no_rooms_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(spacing.xs))
          Text(
            text = stringResource(R.string.home_no_rooms_copy),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      CountsRow(units = units)
      Text(
        text = stringResource(R.string.home_today),
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
      )
      UnitBoardList(units = units, typeNames = typeNames, modifier = Modifier.weight(1f))
    }
  }
}

@Composable
private fun CountsRow(units: List<RoomUnit>) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val counts =
    listOf(
      HousekeepingState.DIRTY to colors.statusDirty,
      HousekeepingState.CLEANING to colors.statusCleaning,
      HousekeepingState.CLEANED to colors.statusCleaned,
      HousekeepingState.READY to colors.statusReadyAccessible,
    )
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("home_counts"),
    shape = RoundedCornerShape(radius.lg),
    color = colors.surface,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      counts.forEach { (state, dot) ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
          Spacer(modifier = Modifier.height(spacing.xxs))
          Text(
            text = units.countByState(state).toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
          )
          StatusChip(state = state)
        }
      }
    }
  }
}
