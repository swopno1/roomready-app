package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.repository.ActivityRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.Ticket
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ActivityFeedViewModel(activity: ActivityRepository) : ViewModel() {
  val events: StateFlow<List<ActivityEvent>> =
    activity.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class TicketsViewModel(tickets: TicketRepository) : ViewModel() {
  val openTickets: StateFlow<List<Ticket>> =
    tickets.observeOpenTickets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
