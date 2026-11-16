package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.ui.state.ActivityFeedViewModel
import com.vivereply.roomready.ui.state.UiDates
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun ActivityRoute(
  viewModel: ActivityFeedViewModel,
  languageTag: String,
  modifier: Modifier = Modifier,
) {
  val events by viewModel.events.collectAsStateWithLifecycle()
  ActivityScreen(events = events, languageTag = languageTag, modifier = modifier)
}

@Composable
fun ActivityScreen(events: List<ActivityEvent>, languageTag: String, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Column(
    modifier =
      modifier.fillMaxSize().background(colors.canvas).padding(spacing.md)
        .testTag("activity_list"),
  ) {
    Text(
      text = stringResource(R.string.activity_recent),
      style = MaterialTheme.typography.titleLarge,
      color = colors.ink,
    )
    Spacer(modifier = Modifier.height(spacing.sm))
    if (events.isEmpty()) {
      Text(
        text = stringResource(R.string.activity_empty_after_sync),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.inkSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
      ) {
        items(events, key = { it.id }) { event ->
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
