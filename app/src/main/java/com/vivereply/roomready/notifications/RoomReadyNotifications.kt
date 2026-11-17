package com.vivereply.roomready.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vivereply.roomready.MainActivity
import com.vivereply.roomready.R

/**
 * Single channel + posting for RoomReady's local notifications. Posting
 * silently no-ops when the runtime permission is missing (Android 13+):
 * the alarms stay scheduled, so granting permission later just works.
 */
object RoomReadyNotifications {
  const val CHANNEL_ID: String = "roomready_daily"
  const val MORNING_NOTIFICATION_ID: Int = 2001
  const val NUDGE_NOTIFICATION_ID: Int = 2002

  fun ensureChannel(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notif_channel),
        NotificationManager.IMPORTANCE_DEFAULT,
      ),
    )
  }

  fun postMorning(context: Context, payload: MorningSummary.Payload) {
    post(context, MORNING_NOTIFICATION_ID, payload.title, payload.body)
  }

  fun postWaiting(context: Context, payload: WaitingInspection.Payload) {
    post(context, NUDGE_NOTIFICATION_ID, payload.title, payload.body)
  }

  private fun post(context: Context, id: Int, title: String, body: String) {
    ensureChannel(context)
    if (!AlarmScheduler.canPost(context)) return
    val launch =
      PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(launch)
        .setAutoCancel(true)
        .build()
    try {
      NotificationManagerCompat.from(context).notify(id, notification)
    } catch (_: SecurityException) {
      // Permission revoked between the check and the post: stay silent.
    }
  }
}
