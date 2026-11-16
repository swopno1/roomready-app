package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.data.pms.PmsException
import com.vivereply.roomready.data.pms.SyncOrchestrator
import com.vivereply.roomready.domain.model.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sync status shown on Home and Profile. */
data class SyncStatusUi(
  val connected: Boolean,
  val lastSyncedAt: Long?,
  val pendingCount: Int,
  val syncing: Boolean,
  val error: PmsError?,
)

class ConnectionViewModel(private val orchestrator: SyncOrchestrator) : ViewModel() {

  private val _status =
    MutableStateFlow(
      SyncStatusUi(
        connected = orchestrator.isConnected(),
        lastSyncedAt = orchestrator.lastSyncedAt(),
        pendingCount = 0,
        syncing = false,
        error = null,
      ),
    )
  val status: StateFlow<SyncStatusUi> = _status.asStateFlow()

  private val _role = MutableStateFlow<StaffRole?>(null)
  val role: StateFlow<StaffRole?> = _role.asStateFlow()

  private val _staffName = MutableStateFlow<String?>(null)
  val staffName: StateFlow<String?> = _staffName.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _status.value =
        _status.value.copy(
          connected = orchestrator.isConnected(),
          lastSyncedAt = orchestrator.lastSyncedAt(),
          pendingCount = orchestrator.pendingCount(),
        )
      _role.value = runCatching { orchestrator.staffIdentity()?.role }.getOrNull()
      _staffName.value = runCatching { orchestrator.staffIdentity()?.name }.getOrNull()
    }
  }

  fun syncNow() {
    if (_status.value.syncing) return
    viewModelScope.launch {
      _status.value = _status.value.copy(syncing = true, error = null)
      val result = orchestrator.syncNow()
      result.fold(
        onSuccess = {
          _status.value =
            _status.value.copy(
              connected = true,
              lastSyncedAt = orchestrator.lastSyncedAt(),
              pendingCount = it.pendingAfter,
              syncing = false,
              error = null,
            )
        },
        onFailure = { e ->
          _status.value =
            _status.value.copy(
              syncing = false,
              error = (e as? PmsException)?.error,
              pendingCount = orchestrator.pendingCount(),
            )
        },
      )
    }
  }

  fun clearError() {
    _status.value = _status.value.copy(error = null)
  }
}
