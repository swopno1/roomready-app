package com.vivereply.roomready.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Daily alarm schedule for local notifications: the morning summary at
 * 06:00 for all staff, and a mid-morning inspection nudge at 10:00 that
 * only fires on supervisor sessions. Inexact alarms — no exact-alarm
 * permission needed, gentle on a 2 GB device.
 */
object AlarmScheduler {
  const val MORNING_HOUR: Int = 6
  const val NUDGE_HOUR: Int = 10
  const val MORNING_REQUEST_CODE: Int = 1001
  const val NUDGE_REQUEST_CODE: Int = 1002

  fun scheduleAll(context: Context) {
    schedule(context, MORNING_REQUEST_CODE, MorningSummaryReceiver::class.java, MORNING_HOUR)
    schedule(context, NUDGE_REQUEST_CODE, WaitingInspectionReceiver::class.java, NUDGE_HOUR)
  }

  private fun schedule(
    context: Context,
    requestCode: Int,
    receiver: Class<*>,
    hour: Int,
  ) {
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return
    val pending =
      PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, receiver),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    alarms.cancel(pending)
    alarms.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      nextOccurrence(hour, 0, System.currentTimeMillis()),
      AlarmManager.INTERVAL_DAY,
      pending,
    )
  }

  /**
   * Next [hour]:[minute] strictly after [nowEpochMillis], in device-local
   * time. Pure so the boundary is unit-testable.
   */
  fun nextOccurrence(hour: Int, minute: Int, nowEpochMillis: Long): Long {
    val calendar =
      Calendar.getInstance().apply {
        timeInMillis = nowEpochMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    if (calendar.timeInMillis <= nowEpochMillis) {
      calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return calendar.timeInMillis
  }

  /** True on Android 13+ when the user has granted the runtime permission. */
  fun canPost(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
    return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
      android.content.pm.PackageManager.PERMISSION_GRANTED
  }
}
