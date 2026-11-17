package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivereply.roomready.R
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.data.pms.QrError
import com.vivereply.roomready.ui.components.RoomReadyIcons
import com.vivereply.roomready.ui.state.SetupViewModel
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** Setup form state for the stateless screen (also reusable in tests). */
data class SetupFormState(
  val address: String = "",
  val apiKey: String = "",
  val staffName: String = "",
  val connecting: Boolean = false,
  val error: PmsError? = null,
)

@Composable
fun SetupRoute(
  viewModel: SetupViewModel,
  onConnected: () -> Unit,
  onScanQr: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val connected by viewModel.connected.collectAsStateWithLifecycle()
  LaunchedEffect(connected) {
    if (connected) onConnected()
  }
  var pasteOpen by remember { mutableStateOf(false) }
  SetupScreen(
    state =
      SetupFormState(
        address = viewModel.address,
        apiKey = viewModel.apiKey,
        staffName = viewModel.staffName,
        connecting = viewModel.connecting,
        error = viewModel.error,
      ),
    onAddressChange = { viewModel.address = it },
    onApiKeyChange = { viewModel.apiKey = it },
    onStaffNameChange = { viewModel.staffName = it },
    onConnect = { viewModel.connect() },
    onScanQr = onScanQr,
    onPasteCode = { pasteOpen = true },
    modifier = modifier,
  )
  if (pasteOpen) {
    PasteCodeDialog(
      pasteError = viewModel.pasteError,
      onDismiss = {
        pasteOpen = false
        viewModel.clearPasteError()
      },
      onApply = { raw ->
        if (viewModel.applyPastedCode(raw)) {
          pasteOpen = false
          viewModel.clearPasteError()
        }
      },
    )
  }
}

@Composable
fun SetupScreen(
  state: SetupFormState,
  onAddressChange: (String) -> Unit,
  onApiKeyChange: (String) -> Unit,
  onStaffNameChange: (String) -> Unit,
  onConnect: () -> Unit,
  onScanQr: () -> Unit,
  onPasteCode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(spacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
      verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      Text(
        text = stringResource(R.string.setup_title),
        style = MaterialTheme.typography.headlineSmall,
        color = colors.ink,
      )
      Text(
        text = stringResource(R.string.setup_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.inkSecondary,
      )

      OutlinedTextField(
        value = state.address,
        onValueChange = onAddressChange,
        modifier = Modifier.fillMaxWidth().testTag("setup_address"),
        label = { Text(stringResource(R.string.setup_address_label)) },
        placeholder = { Text(stringResource(R.string.setup_address_hint)) },
        singleLine = true,
        enabled = !state.connecting,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
      )
      // The key field is always visually masked: secrets are never shown on screen.
      OutlinedTextField(
        value = state.apiKey,
        onValueChange = onApiKeyChange,
        modifier = Modifier.fillMaxWidth().testTag("setup_key"),
        label = { Text(stringResource(R.string.setup_key_label)) },
        placeholder = { Text(stringResource(R.string.setup_key_hint)) },
        singleLine = true,
        enabled = !state.connecting,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
      )
      OutlinedTextField(
        value = state.staffName,
        onValueChange = onStaffNameChange,
        modifier = Modifier.fillMaxWidth().testTag("setup_name"),
        label = { Text(stringResource(R.string.setup_name_label)) },
        placeholder = { Text(stringResource(R.string.setup_name_hint)) },
        singleLine = true,
        enabled = !state.connecting,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConnect() }),
      )

      state.error?.let { error ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(radius.md),
          color = colors.statusDirtyContainer,
        ) {
          Text(
            text = stringResource(error.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onStatusDirtyContainer,
            modifier = Modifier.padding(spacing.md).testTag("setup_error"),
          )
        }
      }

      Button(
        onClick = onConnect,
        enabled = !state.connecting,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("setup_connect"),
        shape = RoundedCornerShape(radius.md),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = colors.ink,
            contentColor = colors.onInk,
          ),
      ) {
        if (state.connecting) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = colors.mintSolid,
            strokeWidth = 3.dp,
          )
          Spacer(modifier = Modifier.size(spacing.sm))
          Text(stringResource(R.string.setup_working), style = MaterialTheme.typography.labelLarge)
        } else {
          Text(stringResource(R.string.setup_connect), style = MaterialTheme.typography.labelLarge)
        }
      }

      OutlinedButton(
        onClick = onScanQr,
        enabled = !state.connecting,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumInteractiveComponentSize()
            .testTag("setup_scan"),
        shape = RoundedCornerShape(radius.md),
      ) {
        Icon(
          imageVector = RoomReadyIcons.QrCodeScanner,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(stringResource(R.string.setup_scan_qr), style = MaterialTheme.typography.labelLarge)
      }

      TextButton(
        onClick = onPasteCode,
        enabled = !state.connecting,
        modifier =
          Modifier.minimumInteractiveComponentSize().align(Alignment.CenterHorizontally)
            .testTag("setup_paste"),
      ) {
        Text(stringResource(R.string.setup_paste_code), style = MaterialTheme.typography.labelLarge)
      }

      Spacer(modifier = Modifier.height(spacing.md))
    }
  }
}

@Composable
private fun PasteCodeDialog(
  pasteError: QrError?,
  onDismiss: () -> Unit,
  onApply: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.setup_paste_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(RoomReadyThemeTokens.spacing.sm)) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth().testTag("paste_field"),
          placeholder = { Text(stringResource(R.string.setup_paste_hint)) },
          singleLine = false,
          maxLines = 4,
        )
        pasteError?.let {
          Text(
            text = stringResource(it.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = RoomReadyThemeTokens.colors.statusDirty,
            modifier = Modifier.testTag("paste_error"),
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onApply(text) },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("paste_apply"),
      ) {
        Text(stringResource(R.string.setup_paste_apply))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_close))
      }
    },
  )
}
