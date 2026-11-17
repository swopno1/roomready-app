package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.pms.SyncOrchestrator
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BoardFilter {
  ALL,
  DIRTY,
  CLEANING,
  CLEANED,
  READY,
}

fun BoardFilter.matches(state: HousekeepingState): Boolean =
  when (this) {
    BoardFilter.ALL -> true
    BoardFilter.DIRTY -> state == HousekeepingState.DIRTY
    BoardFilter.CLEANING -> state == HousekeepingState.CLEANING
    BoardFilter.CLEANED -> state == HousekeepingState.CLEANED
    BoardFilter.READY -> state == HousekeepingState.READY
  }

class BoardViewModel(
  private val rooms: RoomRepository,
  private val orchestrator: SyncOrchestrator,
) : ViewModel() {

  val units: StateFlow<List<RoomUnit>> =
    rooms.observeUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _types = MutableStateFlow<Map<String, RoomType>>(emptyMap())
  val types: StateFlow<Map<String, RoomType>> = _types.asStateFlow()

  private val _filter = MutableStateFlow(BoardFilter.ALL)
  val filter: StateFlow<BoardFilter> = _filter.asStateFlow()

  private val _loading = MutableStateFlow(true)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _loadError = MutableStateFlow(false)
  val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

  private val _busyUnits = MutableStateFlow<Set<String>>(emptySet())
  val busyUnits: StateFlow<Set<String>> = _busyUnits.asStateFlow()

  private val _actionErrors = MutableStateFlow<Map<String, Int>>(emptyMap())
  val actionErrors: StateFlow<Map<String, Int>> = _actionErrors.asStateFlow()

  init {
    refresh()
  }

  fun setFilter(filter: BoardFilter) {
    _filter.value = filter
  }

  fun refresh() {
    viewModelScope.launch {
      try {
        _types.value = rooms.roomTypes().associateBy { it.id }
        rooms.observeUnits().first()
        _loading.value = false
        _loadError.value = false
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (e: Exception) {
        _loading.value = false
        _loadError.value = true
      }
    }
  }

  fun retry() {
    _loadError.value = false
    _loading.value = true
    refresh()
  }

  fun runAction(action: UnitAction, unit: RoomUnit) {
    // FINISH always routes to the room detail screen (photo/reason sheet);
    // the board executes START and INSPECT immediately.
    if (action == UnitAction.FINISH) return
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _busyUnits.value += unit.id
      _actionErrors.value -= unit.id
      val result =
        when (action) {
          UnitAction.START_CLEANING -> rooms.startCleaning(unit.id, staff)
          else -> rooms.inspect(unit.id, staff)
        }
      _busyUnits.value -= unit.id
      result.fold(
        onSuccess = { _actionErrors.value -= unit.id },
        onFailure = { _actionErrors.value += (unit.id to actionErrorRes(it)) },
      )
    }
  }

  fun sendBack(unit: RoomUnit, reason: String) {
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _busyUnits.value += unit.id
      _actionErrors.value -= unit.id
      val result = rooms.sendBack(unit.id, staff, reason)
      _busyUnits.value -= unit.id
      result.fold(
        onSuccess = { _actionErrors.value -= unit.id },
        onFailure = { _actionErrors.value += (unit.id to actionErrorRes(it)) },
      )
    }
  }

  fun clearActionError(unitId: String) {
    _actionErrors.value -= unitId
  }
}

fun List<RoomUnit>.countByState(state: HousekeepingState): Int = count { it.hkState == state }
