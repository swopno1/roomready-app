package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.CleaningEvidence
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.ui.components.SendBackDialog
import com.vivereply.roomready.ui.components.StatusChip
import com.vivereply.roomready.ui.state.DetailViewModel
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InspectUiState(
  val unit: RoomUnit? = null,
  val typeName: String = "",
  val progress: ChecklistProgress? = null,
  val evidence: CleaningEvidence? = null,
  val evidencePhoto: ImageBitmap? = null,
  val role: StaffRole? = null,
  val busy: Boolean = false,
  val actionError: Int? = null,
)

@Composable
fun InspectRoute(
  viewModel: DetailViewModel,
  graph: EngineGraph,
  languageTag: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val unit by viewModel.unit.collectAsStateWithLifecycle()
  val type by viewModel.type.collectAsStateWithLifecycle()
  val progress by viewModel.progress.collectAsStateWithLifecycle()
  val evidence by viewModel.evidence.collectAsStateWithLifecycle()
  val role by viewModel.role.collectAsStateWithLifecycle()
  val busy by viewModel.busy.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()
  var photo by remember { mutableStateOf<ImageBitmap?>(null) }
  var sendBackOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) { viewModel.refresh() }
  LaunchedEffect(evidence?.photoRef) {
    photo =
      evidence?.photoRef?.let { withContext(Dispatchers.IO) { graph.photos.loadThumbnail(it) } }
  }
  // A decided room leaves the queue: pop once it is no longer Cleaned.
  LaunchedEffect(unit?.hkState) {
    if (unit != null && unit?.hkState != HousekeepingState.CLEANED) onClose()
  }

  InspectScreen(
    state =
      InspectUiState(
        unit = unit,
        typeName = type?.let { if (languageTag.startsWith("bn")) it.nameBn else it.nameEn } ?: "",
        progress = progress,
        evidence = evidence,
        evidencePhoto = photo,
        role = role,
        busy = busy,
        actionError = actionError,
      ),
    onBack = onClose,
    onPass = { viewModel.inspect() },
    onSendBack = { sendBackOpen = true },
    modifier = modifier,
  )

  if (sendBackOpen && unit != null) {
    SendBackDialog(
      unit = unit!!,
      onDismiss = { sendBackOpen = false },
      onConfirm = { reason ->
        viewModel.sendBack(reason)
        sendBackOpen = false
      },
    )
  }
}

@Composable
fun InspectScreen(
  state: InspectUiState,
  onBack: () -> Unit,
  onPass: () -> Unit,
  onSendBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val unit = state.unit
  val isSupervisor = state.role == StaffRole.SUPERVISOR

  Column(modifier = modifier.fillMaxSize().background(colors.canvas)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.md),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("inspect_back")) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.detail_back),
          tint = colors.ink,
        )
      }
      Text(
        text = unit?.let { stringResource(R.string.inspect_title, it.number) } ?: "",
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink,
      )
    }
    if (unit == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.detail_not_found),
          style = MaterialTheme.typography.bodyLarge,
          color = colors.inkSecondary,
        )
      }
      return@Column
    }
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
          .padding(horizontal = spacing.md)
          .testTag("inspect_scroll"),
      verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      Surface(
        shape = RoundedCornerShape(radius.lg),
        color = colors.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(spacing.md),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(text = unit.number, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(
              text = state.typeName,
              style = MaterialTheme.typography.bodyMedium,
              color = colors.inkSecondary,
            )
          }
          StatusChip(state = unit.hkState)
        }
      }

      Surface(
        shape = RoundedCornerShape(radius.lg),
        color = colors.surface,
        modifier = Modifier.fillMaxWidth().testTag("inspect_checklist"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(spacing.md),
          verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
          Text(
            text = stringResource(R.string.inspect_checklist),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
          )
          TurnoverStep.entries.sortedBy { it.order }.forEach { step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(
                  if (state.progress?.done?.contains(step) == true) colors.mintSolid
                  else colors.surfaceMuted,
                ),
                contentAlignment = Alignment.Center,
              ) {
                if (state.progress?.done?.contains(step) == true) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(spacing.sm))
              Text(
                text = stringResource(step.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
              )
            }
          }
        }
      }

      state.evidence?.let { evidence ->
        Surface(
          shape = RoundedCornerShape(radius.lg),
          color = colors.surface,
          modifier = Modifier.fillMaxWidth().testTag("inspect_evidence"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
          ) {
            Text(
              text = stringResource(R.string.detail_finished_by, evidence.finishedBy),
              style = MaterialTheme.typography.titleSmall,
              color = colors.ink,
            )
            if (state.evidencePhoto != null) {
              Image(
                bitmap = state.evidencePhoto,
                contentDescription = stringResource(R.string.photo_preview_cd),
                contentScale = ContentScale.Crop,
                modifier =
                  Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(radius.md))
                    .testTag("inspect_photo"),
              )
            } else if (evidence.photoRef != null) {
              Text(
                text = stringResource(R.string.detail_evidence_photo),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
              )
            }
            if (evidence.skipReason != null) {
              Text(
                text = stringResource(R.string.detail_evidence_reason, evidence.skipReason),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
              )
            }
          }
        }
      }

      state.actionError?.let {
        Text(
          text = stringResource(it),
          style = MaterialTheme.typography.labelMedium,
          color = colors.statusDirty,
          modifier = Modifier.testTag("inspect_error"),
        )
      }

      if (isSupervisor) {
        Button(
          onClick = onPass,
          enabled = !state.busy,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
              .testTag("inspect_pass"),
          shape = RoundedCornerShape(radius.md),
          colors =
            ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
        ) {
          if (state.busy) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = colors.mintSolid,
            )
            Spacer(modifier = Modifier.width(spacing.xs))
          }
          Text(stringResource(R.string.inspect_pass), style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
          onClick = onSendBack,
          enabled = !state.busy,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumInteractiveComponentSize()
              .testTag("inspect_sendback"),
          shape = RoundedCornerShape(radius.md),
        ) {
          Text(
            text = stringResource(R.string.action_send_back),
            style = MaterialTheme.typography.labelLarge,
            color = colors.statusDirty,
          )
        }
      }
      Spacer(modifier = Modifier.height(spacing.md))
    }
  }
}
