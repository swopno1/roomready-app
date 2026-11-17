package com.vivereply.roomready.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules the daily alarms after a reboot; alarms don't survive one. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    AlarmScheduler.scheduleAll(context)
  }
}
