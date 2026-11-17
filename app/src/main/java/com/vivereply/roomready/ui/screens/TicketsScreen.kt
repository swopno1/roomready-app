package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.ui.components.AllCaughtUp
import com.vivereply.roomready.ui.components.durationText
import com.vivereply.roomready.ui.state.BoardViewModel
import com.vivereply.roomready.ui.state.TicketDraft
import com.vivereply.roomready.ui.state.TicketsViewModel
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TicketsRoute(
  viewModel: TicketsViewModel,
  onNewTicket: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tickets by viewModel.openTickets.collectAsStateWithLifecycle()
  val doneError by viewModel.doneError.collectAsStateWithLifecycle()
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(60_000L)
      now = System.currentTimeMillis()
    }
  }
  TicketsScreen(
    tickets = tickets,
    nowEpochMillis = now,
    doneError = doneError,
    onDone = { viewModel.markDone(it) },
    onNewTicket = onNewTicket,
    modifier = modifier,
  )
}

@Composable
fun TicketsScreen(
  tickets: List<Ticket>,
  nowEpochMillis: Long,
  doneError: Int?,
  onDone: (String) -> Unit,
  onNewTicket: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = colors.canvas,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNewTicket,
        modifier = Modifier.testTag("ticket_new"),
        containerColor = colors.ink,
        contentColor = colors.onInk,
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = stringResource(R.string.ticket_new_title),
        )
      }
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(spacing.md),
    ) {
      Text(
        text = stringResource(R.string.tickets_open_title),
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
      )
      Spacer(modifier = Modifier.height(spacing.sm))
      doneError?.let {
        Surface(
          shape = RoundedCornerShape(RoomReadyThemeTokens.radius.md),
          color = colors.statusDirtyContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onStatusDirtyContainer,
            modifier = Modifier.padding(spacing.sm),
          )
        }
        Spacer(modifier = Modifier.height(spacing.sm))
      }
      if (tickets.isEmpty()) {
        AllCaughtUp(
          title = stringResource(R.string.home_all_caught_up_title),
          copy = stringResource(R.string.tickets_none_after_sync),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("ticket_list"),
          verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
          items(tickets, key = { it.id }) { ticket ->
            TicketCard(
              ticket = ticket,
              nowEpochMillis = nowEpochMillis,
              onDone = { onDone(ticket.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TicketCard(ticket: Ticket, nowEpochMillis: Long, onDone: () -> Unit) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val category = stringResource(ticket.category.labelRes)
  val rowCd = stringResource(R.string.tickets_row_cd, ticket.roomNumber, category)
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("ticket_card_${ticket.id}"),
    shape = RoundedCornerShape(radius.md),
    color = colors.surface,
    shadowElevation = RoomReadyThemeTokens.elevation.card,
  ) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      Box(
        modifier =
          Modifier.width(8.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = radius.md, bottomStart = radius.md))
            .background(colors.statusCleaning),
      )
      Column(
        modifier =
          Modifier.weight(1f).padding(spacing.md)
            .semantics { contentDescription = rowCd },
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
          Surface(shape = RoundedCornerShape(radius.pill), color = colors.statusCleaningContainer) {
            Text(
              text = category,
              style = MaterialTheme.typography.labelLarge,
              color = colors.onStatusCleaningContainer,
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              stringResource(
                R.string.ticket_age,
                durationText(nowEpochMillis - ticket.createdAtEpochMillis),
              ),
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
          )
          Text(
            text = ticket.createdBy,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
          )
        }
        Button(
          onClick = onDone,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumInteractiveComponentSize()
              .testTag("ticket_done_${ticket.id}"),
          shape = RoundedCornerShape(radius.md),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = colors.ink,
              contentColor = colors.onInk,
            ),
        ) {
          Text(stringResource(R.string.ticket_mark_done), style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

data class TicketFormUiState(
  val units: List<RoomUnit> = emptyList(),
  val unitId: String? = null,
  val roomLocked: Boolean = false,
  val category: TicketCategory? = null,
  val note: String = "",
  val photo: ImageBitmap? = null,
  val hasPhoto: Boolean = false,
  val raising: Boolean = false,
  val errorRes: Int? = null,
  val canRaise: Boolean = false,
)

@Composable
fun TicketFormRoute(
  tickets: TicketsViewModel,
  board: BoardViewModel,
  graph: EngineGraph,
  preselectedUnitId: String?,
  onTakePhoto: () -> Unit,
  onRaised: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val draft by tickets.draft.collectAsStateWithLifecycle()
  val units by board.units.collectAsStateWithLifecycle()
  var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
  var pickerOpen by remember { mutableStateOf(false) }

  LaunchedEffect(preselectedUnitId) { tickets.startDraft(preselectedUnitId) }
  LaunchedEffect(draft.photoFile) {
    thumbnail =
      draft.photoFile?.let { withContext(Dispatchers.IO) { graph.photos.loadTempThumbnail(it) } }
  }
  LaunchedEffect(draft.raised) {
    if (draft.raised) {
      tickets.consumeRaised()
      onRaised()
    }
  }

  TicketFormScreen(
    state =
      TicketFormUiState(
        units = units,
        unitId = draft.unitId,
        roomLocked = preselectedUnitId != null,
        category = draft.category,
        note = draft.note,
        photo = thumbnail,
        hasPhoto = draft.photoFile != null,
        raising = draft.raising,
        errorRes = draft.errorRes,
        canRaise = draft.canRaise,
      ),
    onUnit = { tickets.setUnit(it) },
    onPickRoom = { pickerOpen = true },
    onCategory = { tickets.setCategory(it) },
    onNote = { tickets.setNote(it) },
    onAddPhoto = onTakePhoto,
    onClearPhoto = { tickets.clearPhoto() },
    onRaise = { tickets.raise() },
    onBack = onBack,
    modifier = modifier,
  )

  if (pickerOpen) {
    RoomPickerDialog(
      units = units,
      onPick = {
        tickets.setUnit(it.id)
        pickerOpen = false
      },
      onDismiss = { pickerOpen = false },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TicketFormScreen(
  state: TicketFormUiState,
  onUnit: (String) -> Unit,
  onPickRoom: () -> Unit,
  onCategory: (TicketCategory) -> Unit,
  onNote: (String) -> Unit,
  onAddPhoto: () -> Unit,
  onClearPhoto: () -> Unit,
  onRaise: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val selectedRoom = state.units.firstOrNull { it.id == state.unitId }

  Column(modifier = modifier.fillMaxSize().background(colors.canvas)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.md),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("ticket_form_back")) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.detail_back),
          tint = colors.ink,
        )
      }
      Text(
        text = stringResource(R.string.ticket_new_title),
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
      )
    }
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
          text = stringResource(R.string.ticket_room),
          style = MaterialTheme.typography.titleSmall,
          color = colors.ink,
        )
        if (state.roomLocked && selectedRoom != null) {
          Surface(shape = RoundedCornerShape(radius.md), color = colors.surface) {
            Text(
              text = selectedRoom.number,
              style = MaterialTheme.typography.headlineSmall,
              color = colors.ink,
              modifier = Modifier.padding(spacing.md).testTag("ticket_room_fixed"),
            )
          }
        } else {
          OutlinedButton(
            onClick = onPickRoom,
            modifier = Modifier.fillMaxWidth().testTag("ticket_pick_room"),
            shape = RoundedCornerShape(radius.md),
          ) {
            Text(
              text = selectedRoom?.number ?: stringResource(R.string.ticket_pick_room),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(spacing.xs),
          verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
          TicketCategory.entries.forEach { category ->
            FilterChip(
              selected = state.category == category,
              onClick = { onCategory(category) },
              label = { Text(stringResource(category.labelRes)) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("ticket_cat_${category.name.lowercase()}"),
            )
          }
        }
      }

      OutlinedTextField(
        value = state.note,
        onValueChange = onNote,
        modifier = Modifier.fillMaxWidth().testTag("ticket_note"),
        label = { Text(stringResource(R.string.ticket_note_label)) },
        placeholder = { Text(stringResource(R.string.ticket_note_hint)) },
        singleLine = false,
        maxLines = 4,
      )

      if (state.hasPhoto && state.photo != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            bitmap = state.photo,
            contentDescription = stringResource(R.string.photo_preview_cd),
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(radius.md)),
          )
          Spacer(modifier = Modifier.width(spacing.sm))
          TextButton(
            onClick = onClearPhoto,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("ticket_photo_clear"),
          ) {
            Text(stringResource(R.string.finish_retake))
          }
        }
      } else {
        OutlinedButton(
          onClick = onAddPhoto,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
              .testTag("ticket_add_photo"),
          shape = RoundedCornerShape(radius.md),
        ) {
          Icon(
            imageVector = Icons.Rounded.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(spacing.xs))
          Text(stringResource(R.string.ticket_add_photo))
        }
      }

      state.errorRes?.let {
        Text(
          text = stringResource(it),
          style = MaterialTheme.typography.labelMedium,
          color = colors.statusDirty,
          modifier = Modifier.testTag("ticket_error"),
        )
      }

      Button(
        onClick = onRaise,
        enabled = state.canRaise,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("ticket_raise"),
        shape = RoundedCornerShape(radius.md),
        colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
      ) {
        if (state.raising) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = colors.mintSolid,
          )
          Spacer(modifier = Modifier.width(spacing.xs))
        }
        Text(stringResource(R.string.ticket_raise), style = MaterialTheme.typography.labelLarge)
      }
      Spacer(modifier = Modifier.height(spacing.md))
    }
  }
}

@Composable
fun RoomPickerDialog(units: List<RoomUnit>, onPick: (RoomUnit) -> Unit, onDismiss: () -> Unit) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.ticket_pick_room)) },
    text = {
      LazyColumn(modifier = Modifier.fillMaxWidth().testTag("ticket_room_list")) {
        items(units, key = { it.id }) { unit ->
          TextButton(
            onClick = { onPick(unit) },
            modifier =
              Modifier.minimumInteractiveComponentSize().fillMaxWidth()
                .testTag("ticket_room_${unit.id}"),
          ) {
            Text(
              text = unit.number,
              style = MaterialTheme.typography.bodyLarge,
              color = colors.ink,
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Start,
            )
          }
          if (unit != units.lastOrNull()) {
            Spacer(modifier = Modifier.height(spacing.xxs))
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_close))
      }
    },
  )
}
