package com.vivereply.roomready.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vivereply.roomready.R
import com.vivereply.roomready.RoomReadyApplication
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.StaffRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 10:00 daily: nudge when Cleaned rooms wait for inspection. Supervisor
 * sessions only — housekeepers never see it.
 */
class WaitingInspectionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val pending = goAsync()
    receiverScope.launch {
      try {
        val graph = (context.applicationContext as RoomReadyApplication).graph
        if (!graph.orchestrator.isConnected()) return@launch
        if (graph.credentials.staffIdentity()?.role != StaffRole.SUPERVISOR) return@launch
        val waiting =
          graph.rooms.observeUnits().first().count { it.hkState == HousekeepingState.CLEANED }
        val payload =
          WaitingInspection.build(
            waitingCount = waiting,
            title = context.getString(R.string.notif_inspect_title),
            body = { n -> context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
          ) ?: return@launch
        RoomReadyNotifications.postWaiting(context, payload)
      } catch (_: Exception) {
        // A missed nudge is never worth crashing the receiver.
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}
