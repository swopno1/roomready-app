package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.repository.ActivityRepository
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoardViewModel(private val rooms: RoomRepository) : ViewModel() {
  val units: StateFlow<List<RoomUnit>> =
    rooms.observeUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _typeNames = MutableStateFlow<Map<String, String>>(emptyMap())
  val typeNames: StateFlow<Map<String, String>> = _typeNames.asStateFlow()

  init {
    refreshTypes()
  }

  fun refreshTypes() {
    viewModelScope.launch {
      _typeNames.value = rooms.roomTypes().associate { it.id to it.nameEn }
    }
  }
}

fun List<RoomUnit>.countByState(state: HousekeepingState): Int = count { it.hkState == state }

class ActivityFeedViewModel(activity: ActivityRepository) : ViewModel() {
  val events: StateFlow<List<ActivityEvent>> =
    activity.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class TicketsViewModel(tickets: TicketRepository) : ViewModel() {
  val openTickets: StateFlow<List<Ticket>> =
    tickets.observeOpenTickets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
