package com.vivereply.roomready.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vivereply.roomready.R
import com.vivereply.roomready.RoomReadyApplication
import com.vivereply.roomready.domain.model.HousekeepingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 06:00 daily: how many rooms are dirty at day start. All staff. */
class MorningSummaryReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val pending = goAsync()
    receiverScope.launch {
      try {
        val graph = (context.applicationContext as RoomReadyApplication).graph
        if (!graph.orchestrator.isConnected()) return@launch
        val units = graph.rooms.observeUnits().first()
        val dirty = units.count { it.hkState == HousekeepingState.DIRTY }
        val waiting = units.count { it.hkState == HousekeepingState.CLEANED }
        val payload =
          MorningSummary.build(
            dirtyCount = dirty,
            waitingCount = waiting,
            title = context.getString(R.string.notif_morning_title),
            dirtyLine = { n -> context.resources.getQuantityString(R.plurals.notif_morning_body, n, n) },
            waitingLine = { n ->
              context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n)
            },
          ) ?: return@launch
        RoomReadyNotifications.postMorning(context, payload)
      } catch (_: Exception) {
        // A missed summary is never worth crashing the receiver.
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}
