package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.ui.state.ActivityFeedViewModel
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.TodayStats
import com.vivereply.roomready.ui.state.UiDates
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun ActivityRoute(
  viewModel: ActivityFeedViewModel,
  connection: ConnectionViewModel,
  languageTag: String,
  modifier: Modifier = Modifier,
) {
  val events by viewModel.events.collectAsStateWithLifecycle()
  val flagged by viewModel.flagged.collectAsStateWithLifecycle()
  val today by viewModel.today.collectAsStateWithLifecycle()
  val status by connection.status.collectAsStateWithLifecycle()
  LaunchedEffect(Unit) { connection.refresh() }
  val lastSyncedText =
    remember(status.lastSyncedAt, languageTag) {
      status.lastSyncedAt?.let { UiDates.dateTime(languageTag, it) }
    }
  ActivityScreen(
    lastSyncedText = lastSyncedText,
    pendingCount = status.pendingCount,
    flagged = flagged,
    today = today,
    events = events,
    languageTag = languageTag,
    modifier = modifier,
  )
}

@Composable
fun ActivityScreen(
  lastSyncedText: String?,
  pendingCount: Int,
  flagged: List<SyncMutationEntity>,
  today: TodayStats,
  events: List<ActivityEvent>,
  languageTag: String,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  LazyColumn(
    modifier =
      modifier.fillMaxSize().background(colors.canvas).padding(spacing.md)
        .testTag("activity_list"),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    item {
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("activity_sync_log"),
        shape = RoundedCornerShape(radius.md),
        color = colors.surface,
        shadowElevation = RoomReadyThemeTokens.elevation.card,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(spacing.md),
          verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
          Text(
            text = stringResource(R.string.activity_sync_log),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
          )
          Text(
            text =
              lastSyncedText?.let { stringResource(R.string.sync_last_synced, it) }
                ?: stringResource(R.string.sync_never),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
          )
          Text(
            text = pluralStringResource(R.plurals.sync_pending, pendingCount, pendingCount),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
          )
          if (flagged.isEmpty()) {
            Text(
              text = stringResource(R.string.activity_no_parked),
              style = MaterialTheme.typography.bodyMedium,
              color = colors.inkSecondary,
            )
          } else {
            flagged.forEach { entry ->
              Column(
                modifier =
                  Modifier.fillMaxWidth().padding(top = spacing.xs)
                    .testTag("activity_flagged_entry"),
              ) {
                Text(
                  text = entry.lastError ?: stringResource(R.string.activity_t_push_parked),
                  style = MaterialTheme.typography.bodyMedium,
                  color = colors.statusDirty,
                )
                Text(
                  text = "${entry.entityType} · ${entry.entityId}",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.inkMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
    item {
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("activity_offline"),
        shape = RoundedCornerShape(radius.md),
        color = colors.surface,
        shadowElevation = RoomReadyThemeTokens.elevation.card,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(spacing.md),
          verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.WifiOff,
              contentDescription = null,
              tint = colors.statusCleaned,
            )
            Text(
              text = stringResource(R.string.activity_offline_title),
              style = MaterialTheme.typography.titleMedium,
              color = colors.ink,
            )
          }
          if (today.isEmpty) {
            Text(
              text = stringResource(R.string.activity_today_empty),
              style = MaterialTheme.typography.bodyMedium,
              color = colors.inkSecondary,
            )
          } else {
            if (today.cleaned > 0) {
              Text(
                text =
                  pluralStringResource(
                    R.plurals.activity_today_cleaned,
                    today.cleaned,
                    today.cleaned,
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
                modifier = Modifier.testTag("activity_today_cleaned"),
              )
            }
            if (today.tickets > 0) {
              Text(
                text =
                  pluralStringResource(
                    R.plurals.activity_today_tickets,
                    today.tickets,
                    today.tickets,
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
                modifier = Modifier.testTag("activity_today_tickets"),
              )
            }
            if (today.synced > 0) {
              Text(
                text =
                  pluralStringResource(
                    R.plurals.activity_today_synced,
                    today.synced,
                    today.synced,
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
                modifier = Modifier.testTag("activity_today_synced"),
              )
            }
          }
        }
      }
    }
    item {
      Text(
        text = stringResource(R.string.activity_recent),
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
      )
    }
    if (events.isEmpty()) {
      item {
        Text(
          text = stringResource(R.string.activity_empty_after_sync),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.inkSecondary,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )
      }
    } else {
      items(events, key = { it.id }) { event ->
        EventCard(event = event, languageTag = languageTag)
      }
    }
  }
}

@Composable
private fun EventCard(event: ActivityEvent, languageTag: String) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(radius.md),
    color = colors.surface,
    shadowElevation = RoomReadyThemeTokens.elevation.card,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = activityTitle(event),
          style = MaterialTheme.typography.titleSmall,
          color = colors.ink,
          modifier = Modifier.weight(1f),
        )
        if (event.type == ActivityType.PUSH_PARKED) {
          Surface(
            shape = RoundedCornerShape(radius.pill),
            color = colors.statusDirtyContainer,
            modifier = Modifier.testTag("activity_parked_flag"),
          ) {
            Text(
              text = stringResource(R.string.activity_parked_flag),
              style = MaterialTheme.typography.labelLarge,
              color = colors.onStatusDirtyContainer,
              modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            )
          }
        }
      }
      val room = event.roomNumber?.let { " • $it" } ?: ""
      Text(
        text = "${event.actorName}$room • ${UiDates.dateTime(languageTag, event.createdAtEpochMillis)}",
        style = MaterialTheme.typography.labelMedium,
        color = colors.inkMuted,
      )
      event.detail?.let { detail ->
        if (event.type != ActivityType.SYNC_COMPLETED) {
          Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun activityTitle(event: ActivityEvent): String =
  when (event.type) {
    ActivityType.STATUS_CHANGED -> stringResource(R.string.activity_t_status_changed)
    ActivityType.CHECKLIST_COMPLETED -> stringResource(R.string.activity_t_checklist_completed)
    ActivityType.TICKET_RAISED -> stringResource(R.string.activity_t_ticket_raised)
    ActivityType.TICKET_DONE -> stringResource(R.string.activity_t_ticket_done)
    ActivityType.SENT_BACK -> stringResource(R.string.activity_t_sent_back)
    ActivityType.BOOTSTRAP_IMPORTED -> stringResource(R.string.activity_t_bootstrap_imported)
    ActivityType.SYNC_COMPLETED ->
      stringResource(R.string.activity_t_sync_completed, event.detail ?: "0")
    ActivityType.PUSH_PARKED -> stringResource(R.string.activity_t_push_parked)
  }
