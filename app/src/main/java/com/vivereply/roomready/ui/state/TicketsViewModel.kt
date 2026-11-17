package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.R
import com.vivereply.roomready.data.photos.PhotoStore
import com.vivereply.roomready.data.pms.SyncOrchestrator
import com.vivereply.roomready.data.repository.TicketActionError
import com.vivereply.roomready.data.repository.TicketActionException
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Draft for the raise-ticket form; photo stays a temp file until raise succeeds. */
data class TicketDraft(
  val unitId: String? = null,
  val category: TicketCategory? = null,
  val note: String = "",
  val photoFile: File? = null,
  val raising: Boolean = false,
  val errorRes: Int? = null,
  val raised: Boolean = false,
) {
  val canRaise: Boolean = unitId != null && category != null && note.isNotBlank() && !raising
}

class TicketsViewModel(
  private val tickets: TicketRepository,
  private val orchestrator: SyncOrchestrator,
  private val photos: PhotoStore,
) : ViewModel() {

  val openTickets: StateFlow<List<Ticket>> =
    tickets.observeOpenTickets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _draft = MutableStateFlow(TicketDraft())
  val draft: StateFlow<TicketDraft> = _draft.asStateFlow()

  private val _doneError = MutableStateFlow<Int?>(null)
  val doneError: StateFlow<Int?> = _doneError.asStateFlow()

  fun startDraft(unitId: String?) {
    _draft.value = TicketDraft(unitId = unitId)
  }

  fun setUnit(unitId: String) {
    _draft.value = _draft.value.copy(unitId = unitId, errorRes = null)
  }

  fun setCategory(category: TicketCategory) {
    _draft.value = _draft.value.copy(category = category, errorRes = null)
  }

  fun setNote(note: String) {
    _draft.value = _draft.value.copy(note = note, errorRes = null)
  }

  fun attachTempPhoto(file: File) {
    _draft.value = _draft.value.copy(photoFile = file, errorRes = null)
  }

  fun clearPhoto() {
    _draft.value.photoFile?.delete()
    _draft.value = _draft.value.copy(photoFile = null)
  }

  fun raise() {
    val current = _draft.value
    if (!current.canRaise) return
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _draft.value = current.copy(raising = true, errorRes = null)
      val photoRef =
        current.photoFile?.let { file ->
          try {
            withContext(Dispatchers.IO) {
              photos.compressTicket(file, "t${System.currentTimeMillis()}")
            }
          } catch (e: Exception) {
            null
          }
        }
      if (current.photoFile != null && photoRef == null) {
        _draft.value = current.copy(raising = false, errorRes = R.string.ticket_photo_error)
        return@launch
      }
      val result =
        tickets.raiseTicket(current.unitId!!, current.category!!, current.note, photoRef, staff)
      result.fold(
        onSuccess = { _draft.value = TicketDraft(raised = true) },
        onFailure = { _draft.value = current.copy(raising = false, errorRes = ticketErrorRes(it)) },
      )
    }
  }

  fun consumeRaised() {
    _draft.value = _draft.value.copy(raised = false)
  }

  fun markDone(ticketId: String) {
    viewModelScope.launch {
      val staff = orchestrator.staffIdentity() ?: return@launch
      _doneError.value = null
      val result = tickets.markDone(ticketId, staff)
      if (result.isFailure) _doneError.value = R.string.action_err_failed
    }
  }

  fun clearDoneError() {
    _doneError.value = null
  }

  private fun ticketErrorRes(error: Throwable): Int =
    when ((error as? TicketActionException)?.error) {
      is TicketActionError.BlankNote,
      is TicketActionError.UnknownUnit,
      is TicketActionError.UnknownTicket,
      -> R.string.action_err_failed
      else -> R.string.action_err_failed
    }
}
