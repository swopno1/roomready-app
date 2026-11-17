package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.data.photos.PhotoStore
import com.vivereply.roomready.data.pms.SyncOrchestrator
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.CleaningEvidence
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TurnoverStep
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FinishSuccess(val roomNumber: String, val tookMillis: Long)

class DetailViewModelFactory(
  private val graph: EngineGraph,
  private val unitId: String,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    DetailViewModel(unitId, graph.rooms, graph.orchestrator, graph.photos) as T
}

/**
 * One room's detail: live unit, checklist progress, evidence, and all room
 * actions. Progress and evidence are always re-read from the database, so
 * kill-and-reopen restores them exactly.
 */
class DetailViewModel(
  val unitId: String,
  private val rooms: RoomRepository,
  private val orchestrator: SyncOrchestrator,
  private val photos: PhotoStore,
) : ViewModel() {

  val unit: StateFlow<RoomUnit?> =
    rooms.observeUnits()
      .map { list -> list.firstOrNull { it.id == unitId } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  private val _type = MutableStateFlow<RoomType?>(null)
  val type: StateFlow<RoomType?> = _type.asStateFlow()

  private val tick = MutableStateFlow(0)

  private val _progress = MutableStateFlow<ChecklistProgress?>(null)
  val progress: StateFlow<ChecklistProgress?> = _progress.asStateFlow()

  private val _evidence = MutableStateFlow<CleaningEvidence?>(null)
  val evidence: StateFlow<CleaningEvidence?> = _evidence.asStateFlow()

  private val _pendingPhoto = MutableStateFlow<String?>(photos.pendingRef(unitId))
  val pendingPhoto: StateFlow<String?> = _pendingPhoto.asStateFlow()

  private val _role = MutableStateFlow<StaffRole?>(null)
  val role: StateFlow<StaffRole?> = _role.asStateFlow()

  private val _busy = MutableStateFlow(false)
  val busy: StateFlow<Boolean> = _busy.asStateFlow()

  private val _actionError = MutableStateFlow<Int?>(null)
  val actionError: StateFlow<Int?> = _actionError.asStateFlow()

  private val _finishResult = MutableStateFlow<FinishSuccess?>(null)
  val finishResult: StateFlow<FinishSuccess?> = _finishResult.asStateFlow()

  init {
    refresh()
    viewModelScope.launch {
      tick.collect {
        _progress.value = rooms.progress(unitId)
        _evidence.value = rooms.evidenceFor(unitId)
      }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      val current = unit.first()
      _type.value = current?.let { u -> rooms.roomTypes().firstOrNull { it.id == u.roomTypeId } }
      _role.value = runCatching { orchestrator.staffIdentity()?.role }.getOrNull()
      tick.value += 1
    }
  }

  fun toggleStep(step: TurnoverStep, done: Boolean) {
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _busy.value = true
      _actionError.value = null
      val result = rooms.setStepDone(unitId, step, done, staff)
      _busy.value = false
      result.fold(
        onSuccess = { tick.value += 1 },
        onFailure = { _actionError.value = actionErrorRes(it) },
      )
    }
  }

  fun startCleaning() {
    mutate { staff -> rooms.startCleaning(unitId, staff) }
  }

  fun inspect() {
    mutate { staff -> rooms.inspect(unitId, staff) }
  }

  fun sendBack(reason: String) {
    mutate { staff -> rooms.sendBack(unitId, staff, reason) }
  }

  fun confirmFinish(skipReason: String) {
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      val startedAt = unit.value?.updatedAtEpochMillis ?: return@launch
      _busy.value = true
      _actionError.value = null
      val result = rooms.finishCleaning(unitId, staff, _pendingPhoto.value, skipReason.ifBlank { null })
      _busy.value = false
      result.fold(
        onSuccess = { updated ->
          photos.clearPending(unitId)
          _pendingPhoto.value = null
          val took = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
          _finishResult.value = FinishSuccess(updated.number, took)
          tick.value += 1
        },
        onFailure = { _actionError.value = actionErrorRes(it) },
      )
    }
  }

  fun onPhotoCaptured(tempFile: File) {
    viewModelScope.launch {
      _busy.value = true
      _actionError.value = null
      val ref =
        try {
          withContext(Dispatchers.IO) { photos.compress(tempFile, unitId) }
        } catch (e: Exception) {
          null
        }
      _busy.value = false
      if (ref != null) {
        photos.savePending(unitId, ref)
        _pendingPhoto.value = ref
      } else {
        _actionError.value = R.string.finish_photo_error
      }
    }
  }

  fun clearPendingPhoto() {
    photos.clearPending(unitId)
    _pendingPhoto.value = null
  }

  fun clearFinishResult() {
    _finishResult.value = null
  }

  fun clearActionError() {
    _actionError.value = null
  }

  private fun mutate(call: suspend (StaffMember) -> Result<RoomUnit>) {
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _busy.value = true
      _actionError.value = null
      val result = call(staff)
      _busy.value = false
      result.fold(
        onSuccess = { tick.value += 1 },
        onFailure = { _actionError.value = actionErrorRes(it) },
      )
    }
  }
}
