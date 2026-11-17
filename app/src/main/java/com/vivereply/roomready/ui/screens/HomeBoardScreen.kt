package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.components.AllCaughtUp
import com.vivereply.roomready.ui.components.RoomReadyLogoLockup
import com.vivereply.roomready.ui.components.LogoLockupVariant
import com.vivereply.roomready.ui.components.SendBackDialog
import com.vivereply.roomready.ui.components.UnitCard
import com.vivereply.roomready.ui.state.BoardFilter
import com.vivereply.roomready.ui.state.BoardViewModel
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.SyncStatusUi
import com.vivereply.roomready.ui.state.UiDates
import com.vivereply.roomready.ui.state.UnitAction
import com.vivereply.roomready.ui.state.countByState
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import java.util.Calendar
import kotlinx.coroutines.delay

data class HomeDashboardUiState(
  val status: SyncStatusUi,
  val staffName: String?,
  val hourOfDay: Int,
  val units: List<RoomUnit>,
  val types: Map<String, RoomType>,
  val role: StaffRole?,
  val busyUnits: Set<String> = emptySet(),
  val actionErrors: Map<String, Int> = emptyMap(),
  val nowEpochMillis: Long = 0L,
)

@Composable
fun HomeRoute(
  connection: ConnectionViewModel,
  board: BoardViewModel,
  languageTag: String,
  onFilterSelected: (BoardFilter) -> Unit,
  onOpenDetail: (String) -> Unit,
  onOpenInspect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val status by connection.status.collectAsStateWithLifecycle()
  val staffName by connection.staffName.collectAsStateWithLifecycle()
  val role by connection.role.collectAsStateWithLifecycle()
  val units by board.units.collectAsStateWithLifecycle()
  val types by board.types.collectAsStateWithLifecycle()
  val busyUnits by board.busyUnits.collectAsStateWithLifecycle()
  val actionErrors by board.actionErrors.collectAsStateWithLifecycle()
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    connection.refresh()
    board.refresh()
    while (true) {
      delay(60_000L)
      now = System.currentTimeMillis()
    }
  }
  val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
  var sendBackTarget by remember { mutableStateOf<RoomUnit?>(null) }
  HomeDashboardScreen(
    state =
      HomeDashboardUiState(
        status = status,
        staffName = staffName,
        hourOfDay = hour,
        units = units,
        types = types,
        role = role,
        busyUnits = busyUnits,
        actionErrors = actionErrors,
        nowEpochMillis = now,
      ),
    languageTag = languageTag,
    onSyncNow = { connection.syncNow() },
    onFilterSelected = onFilterSelected,
    onAction = { action, unit ->
      when (action) {
        UnitAction.FINISH -> onOpenDetail(unit.id)
        UnitAction.INSPECT -> onOpenInspect(unit.id)
        UnitAction.START_CLEANING -> board.runAction(action, unit)
      }
    },
    onSendBack = { sendBackTarget = it },
    onCardClick = { onOpenDetail(it.id) },
    modifier = modifier,
  )
  sendBackTarget?.let { unit ->
    SendBackDialog(
      unit = unit,
      onDismiss = { sendBackTarget = null },
      onConfirm = { reason ->
        board.sendBack(unit, reason)
        sendBackTarget = null
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeDashboardScreen(
  state: HomeDashboardUiState,
  languageTag: String,
  onSyncNow: () -> Unit,
  onFilterSelected: (BoardFilter) -> Unit,
  onAction: (UnitAction, RoomUnit) -> Unit,
  onSendBack: (RoomUnit) -> Unit,
  onCardClick: (RoomUnit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing

  val dirtyFirst =
    remember(state.units) {
      state.units.filter { it.hkState == HousekeepingState.DIRTY }.sortedBy { it.updatedAtEpochMillis }
    }
  val lastSyncedText =
    remember(state.status.lastSyncedAt, languageTag) {
      state.status.lastSyncedAt?.let { UiDates.dateTime(languageTag, it) }
    }

  LazyColumn(
    modifier = modifier.fillMaxSize().background(colors.canvas).testTag("home_list"),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    item(key = "greeting") {
      GreetingHeader(
        staffName = state.staffName,
        hourOfDay = state.hourOfDay,
        modifier = Modifier.padding(horizontal = spacing.md).padding(top = spacing.md),
      )
    }

    item(key = "hero") {
      HeroCard(
        units = state.units,
        onFilterSelected = onFilterSelected,
        modifier = Modifier.padding(horizontal = spacing.md),
      )
    }

    item(key = "needs_title") {
      Text(
        text = stringResource(R.string.home_needs_attention),
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
        modifier = Modifier.padding(horizontal = spacing.md),
      )
    }

    if (state.units.isEmpty()) {
      item(key = "no_rooms") {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md),
          shape = RoundedCornerShape(RoomReadyThemeTokens.radius.lg),
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
      }
    } else if (dirtyFirst.isEmpty()) {
      item(key = "caught_up") {
        AllCaughtUp(
          title = stringResource(R.string.home_all_caught_up_title),
          copy = stringResource(R.string.home_all_caught_up_copy),
        )
      }
    } else {
      items(dirtyFirst, key = { it.id }) { unit ->
        UnitCard(
          unit = unit,
          typeName = typeDisplayName(state.types[unit.roomTypeId], languageTag, unit.roomTypeId),
          nowEpochMillis = state.nowEpochMillis,
          role = state.role,
          busy = unit.id in state.busyUnits,
          errorRes = state.actionErrors[unit.id],
          onAction = onAction,
          onSendBack = onSendBack,
          onCardClick = onCardClick,
          modifier = Modifier.padding(horizontal = spacing.md).animateItem(),
        )
      }
    }

    item(key = "sync") {
      SyncCard(
        status = state.status,
        lastSyncedText = lastSyncedText,
        onSyncNow = onSyncNow,
        modifier = Modifier.padding(horizontal = spacing.md).padding(bottom = spacing.md),
      )
    }
  }
}

@Composable
private fun GreetingHeader(staffName: String?, hourOfDay: Int, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val greeting =
    stringResource(
      when {
        hourOfDay < 12 -> R.string.greeting_morning
        hourOfDay < 17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
      },
    )
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column {
      Text(
        text = greeting,
        style = MaterialTheme.typography.labelLarge,
        color = colors.inkMuted,
      )
      Text(
        text = staffName ?: greeting,
        style = MaterialTheme.typography.headlineSmall,
        color = colors.ink,
        modifier = Modifier.testTag("home_greeting"),
      )
    }
    RoomReadyLogoLockup(variant = LogoLockupVariant.InkOnLight, markSize = 40.dp, showTagline = false)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
  units: List<RoomUnit>,
  onFilterSelected: (BoardFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val ready = units.countByState(HousekeepingState.READY)
  val total = units.size
  val fraction = if (total == 0) 0f else ready.toFloat() / total.toFloat()

  Surface(
    modifier = modifier.fillMaxWidth().testTag("home_hero"),
    shape = RoundedCornerShape(radius.lg),
    color = colors.ink,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
      ) {
        ProgressRing(fraction = fraction, modifier = Modifier.size(104.dp))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
          Text(
            text = pluralStringResource(R.plurals.home_ready_progress, total, ready, total),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onInk,
          )
          Text(
            text = "$ready/$total",
            style = MaterialTheme.typography.labelLarge,
            color = colors.mintSolid,
          )
        }
      }
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
      ) {
        HeroChip(HousekeepingState.DIRTY, units.countByState(HousekeepingState.DIRTY)) {
          onFilterSelected(BoardFilter.DIRTY)
        }
        HeroChip(HousekeepingState.CLEANING, units.countByState(HousekeepingState.CLEANING)) {
          onFilterSelected(BoardFilter.CLEANING)
        }
        HeroChip(HousekeepingState.CLEANED, units.countByState(HousekeepingState.CLEANED)) {
          onFilterSelected(BoardFilter.CLEANED)
        }
        HeroChip(HousekeepingState.READY, ready) { onFilterSelected(BoardFilter.READY) }
      }
    }
  }
}

@Composable
private fun ProgressRing(fraction: Float, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = size.minDimension * 0.11f
      drawArc(
        color = colors.inkSecondary,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      drawArc(
        brush = Brush.sweepGradient(listOf(colors.mintStart, colors.mintEnd, colors.mintStart)),
        startAngle = -90f,
        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
        useCenter = false,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
    }
    Text(
      text = "${(fraction * 100).toInt()}%",
      style = MaterialTheme.typography.titleLarge,
      color = colors.onInk,
    )
  }
}

@Composable
private fun HeroChip(state: HousekeepingState, count: Int, onClick: () -> Unit) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
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
    onClick = onClick,
    shape = RoundedCornerShape(radius.pill),
    color = container,
    contentColor = content,
    modifier = Modifier.minimumInteractiveComponentSize().testTag("home_chip_${state.name.lowercase()}"),
  ) {
    Text(
      text = "$label $count",
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
    )
  }
}

@Composable
private fun SyncCard(
  status: SyncStatusUi,
  lastSyncedText: String?,
  onSyncNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(radius.lg),
    color = colors.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        SyncPill(status = status)
        Text(
          text =
            lastSyncedText?.let { stringResource(R.string.sync_last_synced, it) }
              ?: stringResource(R.string.sync_never),
          style = MaterialTheme.typography.labelMedium,
          color = colors.inkSecondary,
          modifier = Modifier.testTag("home_last_synced"),
        )
      }
      Text(
        text = pluralStringResource(R.plurals.sync_pending, status.pendingCount, status.pendingCount),
        style = MaterialTheme.typography.titleSmall,
        color = colors.ink,
        modifier = Modifier.testTag("home_pending"),
      )
      Button(
        onClick = onSyncNow,
        enabled = !status.syncing,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("home_sync"),
        shape = RoundedCornerShape(radius.md),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = colors.ink,
            contentColor = colors.onInk,
          ),
      ) {
        if (status.syncing) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 3.dp,
            color = colors.mintSolid,
          )
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
}

/**
 * Sync honesty pill: syncing, then the last outcome (offline on server-down),
 * then pending work, otherwise synced.
 */
@Composable
private fun SyncPill(status: SyncStatusUi, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val pill =
    when {
      status.syncing ->
        SyncPillUi(
          stringResource(R.string.sync_working),
          colors.mintSolid,
          colors.mintWash,
          colors.onStatusReadyContainer,
        )
      status.error is PmsError.ServerDown ->
        SyncPillUi(
          stringResource(R.string.sync_state_offline),
          colors.statusDirty,
          colors.statusDirtyContainer,
          colors.onStatusDirtyContainer,
        )
      status.pendingCount > 0 ->
        SyncPillUi(
          pluralStringResource(R.plurals.sync_pending, status.pendingCount, status.pendingCount),
          colors.statusCleaning,
          colors.statusCleaningContainer,
          colors.onStatusCleaningContainer,
        )
      else ->
        SyncPillUi(
          stringResource(R.string.sync_state_synced),
          colors.statusReadyAccessible,
          colors.statusReadyContainer,
          colors.onStatusReadyContainer,
        )
    }
  Surface(
    modifier = modifier.testTag("home_sync_pill"),
    shape = RoundedCornerShape(radius.pill),
    color = pill.container,
    contentColor = pill.content,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pill.dot))
      Text(text = pill.text, style = MaterialTheme.typography.labelLarge)
    }
  }
}

private data class SyncPillUi(
  val text: String,
  val dot: androidx.compose.ui.graphics.Color,
  val container: androidx.compose.ui.graphics.Color,
  val content: androidx.compose.ui.graphics.Color,
)

private fun typeDisplayName(type: RoomType?, languageTag: String, fallback: String): String =
  type?.let { if (languageTag.startsWith("bn")) it.nameBn else it.nameEn } ?: fallback
