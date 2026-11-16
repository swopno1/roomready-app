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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.ui.state.TicketsViewModel
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun TicketsRoute(viewModel: TicketsViewModel, modifier: Modifier = Modifier) {
  val tickets by viewModel.openTickets.collectAsStateWithLifecycle()
  TicketsScreen(tickets = tickets, modifier = modifier)
}

@Composable
fun TicketsScreen(tickets: List<Ticket>, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Column(modifier = modifier.fillMaxSize().background(colors.canvas).padding(spacing.md)) {
    Text(
      text = stringResource(R.string.tickets_open_title),
      style = MaterialTheme.typography.titleLarge,
      color = colors.ink,
    )
    Spacer(modifier = Modifier.height(spacing.sm))
    if (tickets.isEmpty()) {
      Text(
        text = stringResource(R.string.tickets_none_after_sync),
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
        items(tickets, key = { it.id }) { ticket ->
          val category = stringResource(ticket.category.labelRes)
          val rowCd = stringResource(R.string.tickets_row_cd, ticket.roomNumber, category)
          Surface(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = rowCd },
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
                  text = ticket.roomNumber,
                  style = MaterialTheme.typography.titleLarge,
                  color = colors.ink,
                )
                Surface(shape = RoundedCornerShape(radius.pill), color = colors.surfaceSubtle) {
                  Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                  )
                }
              }
              Text(
                text = ticket.note,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
                maxLines = 3,
              )
              Text(
                text = ticket.createdBy,
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
              )
            }
          }
        }
      }
    }
  }
}
