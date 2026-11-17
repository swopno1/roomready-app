package com.vivereply.roomready.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** Supervisor send-back: reason is required by the domain, so confirm stays disabled until set. */
@Composable
fun SendBackDialog(unit: RoomUnit, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var reason by remember(unit.id) { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sendback_title)) },
    text = {
      Column {
        Text(
          text = unit.number,
          style = MaterialTheme.typography.titleLarge,
          color = RoomReadyThemeTokens.colors.ink,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          modifier = Modifier.fillMaxWidth().testTag("sendback_reason"),
          placeholder = { Text(stringResource(R.string.sendback_hint)) },
          singleLine = false,
          maxLines = 3,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(reason.trim()) },
        enabled = reason.isNotBlank(),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("sendback_confirm"),
      ) {
        Text(stringResource(R.string.sendback_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_close))
      }
    },
  )
}
