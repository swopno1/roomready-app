package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.repository.ActivityRepository
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What was done on this device today, for the offline-proof panel. */
data class TodayStats(val cleaned: Int, val tickets: Int, val synced: Int) {
  val isEmpty: Boolean get() = cleaned == 0 && tickets == 0 && synced == 0
}

class ActivityFeedViewModel(
  activity: ActivityRepository,
  queue: SyncQueueDao,
) : ViewModel() {
  val events: StateFlow<List<ActivityEvent>> =
    activity.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val flagged: StateFlow<List<SyncMutationEntity>> =
    queue.observeFlagged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val today: StateFlow<TodayStats> =
    events.map { summarizeToday(it, System.currentTimeMillis()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayStats(0, 0, 0))
}

/** Counts today's local work; SYNC_COMPLETED details carry delivered counts. */
fun summarizeToday(events: List<ActivityEvent>, nowEpochMillis: Long): TodayStats {
  val start = startOfLocalDay(nowEpochMillis)
  val today = events.filter { it.createdAtEpochMillis >= start }
  return TodayStats(
    cleaned = today.count { it.type == ActivityType.CHECKLIST_COMPLETED },
    tickets = today.count { it.type == ActivityType.TICKET_RAISED },
    synced =
      today.filter { it.type == ActivityType.SYNC_COMPLETED }
        .sumOf { it.detail?.toLongOrNull() ?: 0L }
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt(),
  )
}

fun startOfLocalDay(nowEpochMillis: Long): Long {
  val calendar = Calendar.getInstance()
  calendar.timeInMillis = nowEpochMillis
  calendar.set(Calendar.HOUR_OF_DAY, 0)
  calendar.set(Calendar.MINUTE, 0)
  calendar.set(Calendar.SECOND, 0)
  calendar.set(Calendar.MILLISECOND, 0)
  return calendar.timeInMillis
}
