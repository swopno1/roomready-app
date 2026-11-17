package com.vivereply.roomready.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.vivereply.roomready.ui.components.durationText
import com.vivereply.roomready.ui.components.occupancyLabel
import com.vivereply.roomready.ui.components.timeInStateText
import com.vivereply.roomready.ui.state.DetailViewModel
import com.vivereply.roomready.ui.state.FinishSuccess
import com.vivereply.roomready.ui.state.UnitAction
import com.vivereply.roomready.ui.state.canSendBack
import com.vivereply.roomready.ui.state.primaryActionFor
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class RoomDetailUiState(
  val unit: RoomUnit? = null,
  val typeName: String = "",
  val progress: ChecklistProgress? = null,
  val evidence: CleaningEvidence? = null,
  val sentBackReason: String? = null,
  val role: StaffRole? = null,
  val busy: Boolean = false,
  val actionError: Int? = null,
  val pendingPhoto: ImageBitmap? = null,
  val hasPendingPhoto: Boolean = false,
  val finishResult: FinishSuccess? = null,
  val nowEpochMillis: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailRoute(
  viewModel: DetailViewModel,
  graph: EngineGraph,
  languageTag: String,
  onTakePhoto: () -> Unit,
  onOpenInspect: (String) -> Unit,
  onRaiseTicket: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val unit by viewModel.unit.collectAsStateWithLifecycle()
  val type by viewModel.type.collectAsStateWithLifecycle()
  val progress by viewModel.progress.collectAsStateWithLifecycle()
  val evidence by viewModel.evidence.collectAsStateWithLifecycle()
  val sentBackReason by viewModel.sentBackReason.collectAsStateWithLifecycle()
  val role by viewModel.role.collectAsStateWithLifecycle()
  val busy by viewModel.busy.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()
  val pendingRef by viewModel.pendingPhoto.collectAsStateWithLifecycle()
  val finishResult by viewModel.finishResult.collectAsStateWithLifecycle()
  var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  var sheetOpen by rememberSaveable { mutableStateOf(false) }
  var sendBackOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.refresh()
    while (true) {
      delay(60_000L)
      now = System.currentTimeMillis()
    }
  }
  LaunchedEffect(pendingRef) {
    thumbnail =
      if (pendingRef == null) {
        null
      } else {
        withContext(Dispatchers.IO) { graph.photos.loadThumbnail(pendingRef) }
      }
  }

  RoomDetailScreen(
    state =
      RoomDetailUiState(
        unit = unit,
        typeName =
          type?.let { if (languageTag.startsWith("bn")) it.nameBn else it.nameEn } ?: "",
        progress = progress,
        evidence = evidence,
        sentBackReason = sentBackReason,
        role = role,
        busy = busy,
        actionError = actionError,
        pendingPhoto = thumbnail,
        hasPendingPhoto = pendingRef != null,
        finishResult = finishResult,
        nowEpochMillis = now,
      ),
    onBack = onClose,
    onStart = { viewModel.startCleaning() },
    onInspect = { unit?.let { onOpenInspect(it.id) } },
    onSendBack = { sendBackOpen = true },
    onRaiseTicket = { unit?.let { onRaiseTicket(it.id) } },
    onToggleStep = { step, done -> viewModel.toggleStep(step, done) },
    onOpenFinish = { sheetOpen = true },
    onTakePhoto = onTakePhoto,
    onRetakePhoto = {
      viewModel.clearPendingPhoto()
      onTakePhoto()
    },
    onConfirmFinish = { reason -> viewModel.confirmFinish(reason) },
    onSheetDismiss = {
      sheetOpen = false
      viewModel.clearActionError()
    },
    onFinishDone = {
      sheetOpen = false
      viewModel.clearFinishResult()
      onClose()
    },
    modifier = modifier,
  )

  if (sheetOpen) {
    ModalBottomSheet(
      onDismissRequest = {
        sheetOpen = false
        viewModel.clearActionError()
      },
    ) {
      FinishSheetContent(
        hasPhoto = pendingRef != null,
        photo = thumbnail,
        busy = busy,
        errorRes = actionError,
        finishResult = finishResult,
        onTakePhoto = onTakePhoto,
        onRetakePhoto = {
          viewModel.clearPendingPhoto()
          onTakePhoto()
        },
        onConfirm = { reason -> viewModel.confirmFinish(reason) },
        onDone = {
          sheetOpen = false
          viewModel.clearFinishResult()
          onClose()
        },
      )
    }
  }

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
fun RoomDetailScreen(
  state: RoomDetailUiState,
  onBack: () -> Unit,
  onStart: () -> Unit,
  onInspect: () -> Unit,
  onSendBack: () -> Unit,
  onRaiseTicket: () -> Unit,
  onToggleStep: (TurnoverStep, Boolean) -> Unit,
  onOpenFinish: () -> Unit,
  onTakePhoto: () -> Unit,
  onRetakePhoto: () -> Unit,
  onConfirmFinish: (String) -> Unit,
  onSheetDismiss: () -> Unit,
  onFinishDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val unit = state.unit

  Column(modifier = modifier.fillMaxSize().background(colors.canvas)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.md),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.detail_back),
          tint = colors.ink,
        )
      }
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
          .testTag("detail_scroll"),
      verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      DetailHeader(
        unit = unit,
        typeName = state.typeName,
        nowEpochMillis = state.nowEpochMillis,
      )
      state.evidence?.let { evidence ->
        if (unit.hkState == HousekeepingState.CLEANED || unit.hkState == HousekeepingState.READY) {
          EvidenceCard(evidence = evidence)
        }
      }
      state.sentBackReason?.let { reason ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(radius.md),
          color = colors.statusDirtyContainer,
        ) {
          Text(
            text = stringResource(R.string.detail_sent_back, reason),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onStatusDirtyContainer,
            modifier = Modifier.padding(spacing.md).testTag("detail_sentback"),
          )
        }
      }
      DetailActions(
        unit = unit,
        role = state.role,
        progress = state.progress,
        busy = state.busy,
        onStart = onStart,
        onInspect = onInspect,
        onSendBack = onSendBack,
        onOpenFinish = onOpenFinish,
      )
      state.actionError?.let {
        Text(
          text = stringResource(it),
          style = MaterialTheme.typography.labelMedium,
          color = colors.statusDirty,
          modifier = Modifier.testTag("detail_error"),
        )
      }
      if (unit.hkState == HousekeepingState.CLEANING) {
        ChecklistSection(
          progress = state.progress,
          enabled = !state.busy,
          onToggleStep = onToggleStep,
        )
      }
      OutlinedButton(
        onClick = onRaiseTicket,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumInteractiveComponentSize()
            .testTag("detail_raise_ticket"),
        shape = RoundedCornerShape(radius.md),
      ) {
        Text(stringResource(R.string.ticket_new_title), style = MaterialTheme.typography.labelLarge)
      }
      Spacer(modifier = Modifier.height(spacing.md))
    }
  }
}

@Composable
private fun DetailHeader(unit: RoomUnit, typeName: String, nowEpochMillis: Long) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val (container, content) =
    when (unit.hkState) {
      HousekeepingState.DIRTY -> colors.statusDirtyContainer to colors.onStatusDirtyContainer
      HousekeepingState.CLEANING -> colors.statusCleaningContainer to colors.onStatusCleaningContainer
      HousekeepingState.CLEANED -> colors.statusCleanedContainer to colors.onStatusCleanedContainer
      HousekeepingState.READY -> colors.statusReadyContainer to colors.onStatusReadyContainer
    }
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("detail_header"),
    shape = RoundedCornerShape(radius.lg),
    color = container,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = unit.number,
          style = MaterialTheme.typography.displaySmall,
          color = content,
        )
        StatusChip(state = unit.hkState)
      }
      Text(
        text = "$typeName • ${occupancyLabel(unit.occupancy)}",
        style = MaterialTheme.typography.bodyLarge,
        color = content,
      )
      val duration = timeInStateText(unit.updatedAtEpochMillis, nowEpochMillis)
      Text(
        text =
          if (unit.hkState == HousekeepingState.CLEANING) {
            stringResource(R.string.cleaning_for, duration)
          } else {
            duration
          },
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier.testTag("detail_elapsed"),
      )
    }
  }
}

@Composable
private fun EvidenceCard(evidence: CleaningEvidence) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("detail_evidence"),
    shape = RoundedCornerShape(radius.md),
    color = colors.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
      Text(
        text = stringResource(R.string.detail_finished_by, evidence.finishedBy),
        style = MaterialTheme.typography.titleSmall,
        color = colors.ink,
      )
      if (evidence.photoRef != null) {
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

@Composable
private fun DetailActions(
  unit: RoomUnit,
  role: StaffRole?,
  progress: ChecklistProgress?,
  busy: Boolean,
  onStart: () -> Unit,
  onInspect: () -> Unit,
  onSendBack: () -> Unit,
  onOpenFinish: () -> Unit,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val action = primaryActionFor(unit.hkState, role)
  Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
    if (action == UnitAction.FINISH) {
      val done = progress?.doneCount ?: 0
      val total = progress?.total ?: TurnoverStep.TOTAL
      Text(
        text = pluralStringResource(R.plurals.checklist_progress, done, done, total),
        style = MaterialTheme.typography.labelLarge,
        color = colors.inkSecondary,
      )
      Button(
        onClick = onOpenFinish,
        enabled = !busy && progress?.isComplete == true,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("detail_finish"),
        shape = RoundedCornerShape(radius.md),
        colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
      ) {
        Text(stringResource(R.string.action_finish), style = MaterialTheme.typography.labelLarge)
      }
    } else if (action != null) {
      Button(
        onClick = { if (action == UnitAction.START_CLEANING) onStart() else onInspect() },
        enabled = !busy,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("detail_primary"),
        shape = RoundedCornerShape(radius.md),
        colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
      ) {
        if (busy) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = colors.mintSolid,
          )
          Spacer(modifier = Modifier.width(spacing.xs))
        }
        Text(text = stringResource(action.labelRes), style = MaterialTheme.typography.labelLarge)
      }
    }
    if (canSendBack(unit.hkState, role)) {
      OutlinedButton(
        onClick = onSendBack,
        enabled = !busy,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumInteractiveComponentSize()
            .testTag("detail_sendback"),
        shape = RoundedCornerShape(radius.md),
      ) {
        Text(
          text = stringResource(R.string.action_send_back),
          style = MaterialTheme.typography.labelLarge,
          color = colors.statusDirty,
        )
      }
    }
  }
}

@Composable
private fun ChecklistSection(
  progress: ChecklistProgress?,
  enabled: Boolean,
  onToggleStep: (TurnoverStep, Boolean) -> Unit,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val done = progress?.done ?: emptySet()
  val doneCount = progress?.doneCount ?: 0
  val total = progress?.total ?: TurnoverStep.TOTAL
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("detail_checklist"),
    shape = RoundedCornerShape(radius.lg),
    color = colors.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = pluralStringResource(R.plurals.checklist_progress, doneCount, doneCount, total),
          style = MaterialTheme.typography.titleSmall,
          color = colors.ink,
          modifier = Modifier.testTag("checklist_progress"),
        )
        Text(
          text = "${(doneCount * 100 / total.coerceAtLeast(1))}%",
          style = MaterialTheme.typography.labelLarge,
          color = colors.mintAccessibleInk,
        )
      }
      LinearProgressIndicator(
        progress = { if (total == 0) 0f else doneCount.toFloat() / total.toFloat() },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = colors.mintAccessibleInk,
        trackColor = colors.surfaceMuted,
      )
      TurnoverStep.entries.sortedBy { it.order }.forEach { step ->
        ChecklistRow(
          step = step,
          done = step in done,
          enabled = enabled,
          onToggle = { onToggleStep(step, it) },
        )
      }
    }
  }
}

@Composable
private fun ChecklistRow(step: TurnoverStep, done: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val circle by animateColorAsState(
    if (done) colors.mintSolid else colors.surface,
    label = "checkBg",
  )
  val checkScale by animateFloatAsState(
    if (done) 1f else 0.4f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "checkPop",
  )
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 64.dp)
        .clip(RoundedCornerShape(RoomReadyThemeTokens.radius.md))
        .clickable(enabled = enabled) { onToggle(!done) }
        .padding(horizontal = spacing.xs, vertical = spacing.xs)
        .testTag("check_step_${step.key}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(36.dp).clip(CircleShape)
          .background(circle)
          .then(if (done) Modifier else Modifier.border(2.dp, colors.outline, CircleShape)),
      contentAlignment = Alignment.Center,
    ) {
      if (done) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = colors.ink,
          modifier = Modifier.size(22.dp).graphicsLayer {
            scaleX = checkScale
            scaleY = checkScale
          },
        )
      }
    }
    Spacer(modifier = Modifier.width(spacing.sm))
    Text(
      text = stringResource(step.labelRes),
      style = MaterialTheme.typography.bodyLarge,
      color = if (done) colors.inkMuted else colors.ink,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
fun FinishSheetContent(
  hasPhoto: Boolean,
  photo: ImageBitmap?,
  busy: Boolean,
  errorRes: Int?,
  finishResult: FinishSuccess?,
  onTakePhoto: () -> Unit,
  onRetakePhoto: () -> Unit,
  onConfirm: (String) -> Unit,
  onDone: () -> Unit,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  if (finishResult != null) {
    SuccessContent(result = finishResult, onDone = onDone)
    return
  }
  var reason by rememberSaveable { mutableStateOf("") }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(spacing.lg)
        .testTag("finish_scroll"),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    Text(
      text = stringResource(R.string.finish_title),
      style = MaterialTheme.typography.headlineSmall,
      color = colors.ink,
    )
    Text(
      text = stringResource(R.string.finish_photo),
      style = MaterialTheme.typography.titleSmall,
      color = colors.ink,
    )
    if (hasPhoto && photo != null) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          bitmap = photo,
          contentDescription = stringResource(R.string.photo_preview_cd),
          modifier = Modifier.size(120.dp).clip(RoundedCornerShape(radius.md)),
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        TextButton(
          onClick = onRetakePhoto,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("finish_retake"),
        ) {
          Text(stringResource(R.string.finish_retake))
        }
      }
    } else {
      OutlinedButton(
        onClick = onTakePhoto,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("finish_take_photo"),
        shape = RoundedCornerShape(radius.md),
      ) {
        Icon(
          imageVector = Icons.Rounded.PhotoCamera,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(stringResource(R.string.finish_take_photo))
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
      Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.outlineSubtle))
      Text(
        text = stringResource(R.string.finish_or),
        style = MaterialTheme.typography.labelMedium,
        color = colors.inkMuted,
      )
      Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.outlineSubtle))
    }
    Text(
      text = stringResource(R.string.finish_skip),
      style = MaterialTheme.typography.titleSmall,
      color = colors.ink,
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
      listOf(
        R.string.skip_photo_camera,
        R.string.skip_photo_light,
        R.string.skip_photo_privacy,
      ).forEachIndexed { index, res ->
        val label = stringResource(res)
        FilterChip(
          selected = reason == label,
          onClick = { reason = if (reason == label) "" else label },
          label = { Text(label) },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("finish_preset_$index"),
        )
      }
    }
    OutlinedTextField(
      value = reason,
      onValueChange = { reason = it },
      modifier = Modifier.fillMaxWidth().testTag("finish_reason"),
      placeholder = { Text(stringResource(R.string.finish_reason_hint)) },
      singleLine = false,
      maxLines = 3,
    )
    errorRes?.let {
      Text(
        text = stringResource(it),
        style = MaterialTheme.typography.labelMedium,
        color = colors.statusDirty,
        modifier = Modifier.testTag("finish_error"),
      )
    }
    Button(
      onClick = { onConfirm(reason.trim()) },
      enabled = !busy && (hasPhoto || reason.isNotBlank()),
      modifier =
        Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
          .testTag("finish_confirm"),
      shape = RoundedCornerShape(radius.md),
      colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
    ) {
      if (busy) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = colors.mintSolid,
        )
        Spacer(modifier = Modifier.width(spacing.xs))
      }
      Text(stringResource(R.string.finish_confirm), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(modifier = Modifier.height(spacing.sm))
  }
}

@Composable
private fun SuccessContent(result: FinishSuccess, onDone: () -> Unit) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  val scale by animateFloatAsState(
    if (shown) 1f else 0.3f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "successPop",
  )
  val durationText = durationText(result.tookMillis)
  Column(
    modifier = Modifier.fillMaxWidth().padding(spacing.lg).testTag("finish_success"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Canvas(
      modifier =
        Modifier.size(96.dp).graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    ) {
      drawCircle(colors.mintSolid, size.minDimension / 2f)
      val s = size.minDimension / 96f
      drawPath(
        Path().apply {
          moveTo(32f * s, 49f * s)
          lineTo(44f * s, 61f * s)
          lineTo(65f * s, 36f * s)
        },
        colors.ink,
        style = Stroke(width = 9f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }
    Spacer(modifier = Modifier.height(spacing.md))
    Text(
      text = stringResource(R.string.finish_success_title, result.roomNumber),
      style = MaterialTheme.typography.headlineSmall,
      color = colors.ink,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(spacing.xs))
    Text(
      text = stringResource(R.string.finish_took, durationText),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.inkSecondary,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(spacing.md))
    Button(
      onClick = onDone,
      modifier =
        Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
          .testTag("finish_done"),
      shape = RoundedCornerShape(radius.md),
      colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
    ) {
      Text(stringResource(R.string.finish_done), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(modifier = Modifier.height(spacing.sm))
  }
}
