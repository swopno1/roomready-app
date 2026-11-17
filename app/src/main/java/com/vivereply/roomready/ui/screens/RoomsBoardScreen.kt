package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.ui.components.AllCaughtUp
import com.vivereply.roomready.ui.components.BoardLoadError
import com.vivereply.roomready.ui.components.BoardShimmer
import com.vivereply.roomready.ui.components.SendBackDialog
import com.vivereply.roomready.ui.components.TypeHeader
import com.vivereply.roomready.ui.components.UnitCard
import com.vivereply.roomready.ui.state.BoardFilter
import com.vivereply.roomready.ui.state.BoardViewModel
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.UnitAction
import com.vivereply.roomready.ui.state.countByState
import com.vivereply.roomready.ui.state.matches
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.delay

data class RoomsBoardUiState(
  val units: List<RoomUnit> = emptyList(),
  val types: Map<String, RoomType> = emptyMap(),
  val filter: BoardFilter = BoardFilter.ALL,
  val loading: Boolean = false,
  val loadError: Boolean = false,
  val role: StaffRole? = null,
  val busyUnits: Set<String> = emptySet(),
  val actionErrors: Map<String, Int> = emptyMap(),
  val nowEpochMillis: Long = 0L,
)

@Composable
fun RoomsRoute(
  board: BoardViewModel,
  connection: ConnectionViewModel,
  languageTag: String,
  onOpenDetail: (String) -> Unit,
  onOpenInspect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val units by board.units.collectAsStateWithLifecycle()
  val types by board.types.collectAsStateWithLifecycle()
  val filter by board.filter.collectAsStateWithLifecycle()
  val loading by board.loading.collectAsStateWithLifecycle()
  val loadError by board.loadError.collectAsStateWithLifecycle()
  val busyUnits by board.busyUnits.collectAsStateWithLifecycle()
  val actionErrors by board.actionErrors.collectAsStateWithLifecycle()
  val role by connection.role.collectAsStateWithLifecycle()
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(60_000L)
      now = System.currentTimeMillis()
    }
  }
  var sendBackTarget by remember { mutableStateOf<RoomUnit?>(null) }

  RoomsBoardScreen(
    state =
      RoomsBoardUiState(
        units = units,
        types = types,
        filter = filter,
        loading = loading,
        loadError = loadError,
        role = role,
        busyUnits = busyUnits,
        actionErrors = actionErrors,
        nowEpochMillis = now,
      ),
    languageTag = languageTag,
    onFilter = { board.setFilter(it) },
    onAction = { action, unit ->
      when (action) {
        UnitAction.FINISH -> onOpenDetail(unit.id)
        UnitAction.INSPECT -> onOpenInspect(unit.id)
        UnitAction.START_CLEANING -> board.runAction(action, unit)
      }
    },
    onSendBack = { sendBackTarget = it },
    onCardClick = { onOpenDetail(it.id) },
    onRetry = { board.retry() },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoomsBoardScreen(
  state: RoomsBoardUiState,
  languageTag: String,
  onFilter: (BoardFilter) -> Unit,
  onAction: (UnitAction, RoomUnit) -> Unit,
  onSendBack: (RoomUnit) -> Unit,
  onCardClick: (RoomUnit) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing

  val counts =
    remember(state.units) {
      mapOf(
        BoardFilter.ALL to state.units.size,
        BoardFilter.DIRTY to state.units.countByState(HousekeepingState.DIRTY),
        BoardFilter.CLEANING to state.units.countByState(HousekeepingState.CLEANING),
        BoardFilter.CLEANED to state.units.countByState(HousekeepingState.CLEANED),
        BoardFilter.READY to state.units.countByState(HousekeepingState.READY),
      )
    }
  val visible = remember(state.units, state.filter) { state.units.filter { state.filter.matches(it.hkState) } }
  val groups = remember(visible, state.types) { groupUnits(visible, state.types, languageTag) }

  Column(
    modifier = modifier.fillMaxSize().background(colors.canvas).padding(horizontal = spacing.md),
  ) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm).testTag("board_filters"),
    ) {
      BoardFilter.entries.forEachIndexed { index, filter ->
        SegmentedButton(
          selected = state.filter == filter,
          onClick = { onFilter(filter) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = BoardFilter.entries.size),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("filter_${filter.name.lowercase()}"),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = filterLabel(filter),
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
            )
            Text(
              text = (counts[filter] ?: 0).toString(),
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
            )
          }
        }
      }
    }

    when {
      state.loading -> BoardShimmer(modifier = Modifier.padding(bottom = spacing.md))
      state.loadError -> BoardLoadError(onRetry = onRetry)
      state.units.isEmpty() ->
        AllCaughtUp(
          title = stringResource(R.string.rooms_all_caught_up_title),
          copy = stringResource(R.string.home_no_rooms_copy),
        )
      visible.isEmpty() ->
        AllCaughtUp(
          title = stringResource(R.string.rooms_all_caught_up_title),
          copy = stringResource(R.string.rooms_all_caught_up_copy),
        )
      else ->
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("board_list"),
          verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
          groups.forEach { (type, units) ->
            stickyHeader(key = "header_${type.id}") {
              TypeHeader(
                type = type,
                languageTag = languageTag,
                count = units.size,
                modifier = Modifier.padding(bottom = spacing.xs),
              )
            }
            items(units, key = { it.id }) { unit ->
              val displayType = state.types[unit.roomTypeId]
              val typeName =
                displayType?.let { if (languageTag.startsWith("bn")) it.nameBn else it.nameEn }
                  ?: unit.roomTypeId
              UnitCard(
                unit = unit,
                typeName = typeName,
                nowEpochMillis = state.nowEpochMillis,
                role = state.role,
                busy = unit.id in state.busyUnits,
                errorRes = state.actionErrors[unit.id],
                onAction = onAction,
                onSendBack = onSendBack,
                onCardClick = onCardClick,
                modifier = Modifier.animateItem(),
              )
            }
          }
        }
    }
  }
}

@Composable
private fun filterLabel(filter: BoardFilter): String =
  stringResource(
    when (filter) {
      BoardFilter.ALL -> R.string.filter_all
      BoardFilter.DIRTY -> R.string.status_dirty
      BoardFilter.CLEANING -> R.string.status_cleaning
      BoardFilter.CLEANED -> R.string.status_cleaned
      BoardFilter.READY -> R.string.status_ready
    },
  )

/** Groups units by type, ordered by localized display name; unknown types stay visible. */
fun groupUnits(
  units: List<RoomUnit>,
  types: Map<String, RoomType>,
  languageTag: String,
): List<Pair<RoomType, List<RoomUnit>>> {
  val displayName: (RoomType) -> String =
    { if (languageTag.startsWith("bn")) it.nameBn else it.nameEn }
  return units
    .groupBy { it.roomTypeId }
    .map { (typeId, members) ->
      val type = types[typeId] ?: RoomType(typeId, typeId, typeId, typeId, "", 0)
      type to members.sortedBy { it.number }
    }
    .sortedBy { displayName(it.first) }
}


