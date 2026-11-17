package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.vivereply.roomready.R
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.ui.components.LanguageSelectorRow
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.UiDates
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun ProfileRoute(
  graph: EngineGraph,
  connection: ConnectionViewModel,
  languageTag: String,
  onLanguageSelected: (String) -> Unit,
  onEditConnection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val status by connection.status.collectAsStateWithLifecycle()
  var staff by remember { mutableStateOf<StaffMember?>(null) }
  var maskedKey by remember { mutableStateOf("") }
  var property by remember { mutableStateOf<String?>(null) }
  var pmsAddress by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val versionName =
    remember {
      runCatching {
        val info =
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
              context.packageName,
              android.content.pm.PackageManager.PackageInfoFlags.of(0),
            )
          } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
          }
        info.versionName
      }.getOrNull()
    }
  LaunchedEffect(Unit) {
    connection.refresh()
    staff = graph.orchestrator.staffIdentity()
    maskedKey = graph.credentials.maskedApiKey()
    property = graph.orchestrator.propertyName()
    pmsAddress = graph.orchestrator.pmsAddress()
  }
  val lastSyncedText =
    remember(status.lastSyncedAt, languageTag) {
      status.lastSyncedAt?.let {
        UiDates.dateTime(languageTag, it)
      }
    }
  ProfileScreen(
    propertyName = property,
    pmsAddress = pmsAddress,
    versionName = versionName,
    staff = staff,
    maskedKey = maskedKey,
    lastSyncedText = lastSyncedText,
    syncing = status.syncing,
    languageTag = languageTag,
    onLanguageSelected = onLanguageSelected,
    onEditConnection = onEditConnection,
    onSyncNow = { connection.syncNow() },
    onDisconnect = {
      scope.launch {
        graph.disconnect()
        connection.refresh()
      }
    },
    modifier = modifier,
  )
}

@Composable
fun ProfileScreen(
  propertyName: String?,
  pmsAddress: String?,
  versionName: String?,
  staff: StaffMember?,
  maskedKey: String,
  lastSyncedText: String?,
  syncing: Boolean,
  languageTag: String,
  onLanguageSelected: (String) -> Unit,
  onEditConnection: () -> Unit,
  onSyncNow: () -> Unit,
  onDisconnect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  var confirmDisconnect by remember { mutableStateOf(false) }

  Column(
    modifier =
      modifier.fillMaxSize().background(colors.canvas).verticalScroll(rememberScrollState())
        .padding(spacing.md),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(radius.lg),
      color = colors.ink,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.profile_connected),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onInk,
            modifier = Modifier.testTag("profile_connected"),
          )
          Surface(shape = RoundedCornerShape(radius.pill), color = colors.mintSolid) {
            Text(
              text = staff?.let { stringResource(it.role.labelRes) } ?: "",
              style = MaterialTheme.typography.labelLarge,
              color = colors.ink,
              modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            )
          }
        }
        ProfileRow(label = stringResource(R.string.profile_property), value = propertyName ?: "—")
        ProfileRow(
          label = stringResource(R.string.profile_pms_address),
          value = pmsAddress ?: "—",
        )
        ProfileRow(label = stringResource(R.string.profile_staff), value = staff?.name ?: "—")
        ProfileRow(label = stringResource(R.string.profile_key), value = maskedKey.ifEmpty { "—" })
        ProfileRow(
          label = stringResource(R.string.sync_last_synced_label),
          value = lastSyncedText ?: stringResource(R.string.sync_never),
        )
        ProfileRow(
          label = stringResource(R.string.profile_version),
          value = versionName ?: "—",
        )
      }
    }

    Button(
      onClick = onSyncNow,
      enabled = !syncing,
      modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize(),
      shape = RoundedCornerShape(radius.md),
      colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
    ) {
      if (syncing) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 3.dp, color = colors.mintSolid)
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(stringResource(R.string.sync_working))
      } else {
        Text(stringResource(R.string.sync_now))
      }
    }

    OutlinedButton(
      onClick = onEditConnection,
      modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize(),
      shape = RoundedCornerShape(radius.md),
    ) {
      Text(stringResource(R.string.profile_edit))
    }

    OutlinedButton(
      onClick = { confirmDisconnect = true },
      modifier =
        Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
          .testTag("profile_disconnect"),
      shape = RoundedCornerShape(radius.md),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.statusDirty),
    ) {
      Text(stringResource(R.string.profile_disconnect))
    }

    if (confirmDisconnect) {
      AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text(stringResource(R.string.disconnect_title)) },
        text = { Text(stringResource(R.string.disconnect_copy)) },
        confirmButton = {
          TextButton(
            onClick = {
              confirmDisconnect = false
              onDisconnect()
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_disconnect_yes"),
          ) {
            Text(
              text = stringResource(R.string.profile_disconnect),
              color = colors.statusDirty,
            )
          }
        },
        dismissButton = {
          TextButton(
            onClick = { confirmDisconnect = false },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text(stringResource(R.string.action_cancel))
          }
        },
      )
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(radius.lg),
      color = colors.surface,
    ) {
      LanguageSelectorRow(
        currentLanguageTag = languageTag,
        onLanguageSelected = onLanguageSelected,
        dark = false,
        modifier = Modifier.padding(spacing.md),
      )
    }
  }
}

@Composable
private fun ProfileRow(label: String, value: String) {
  val colors = RoomReadyThemeTokens.colors
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.onInk.copy(alpha = 0.7f))
    Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.onInk)
  }
}
