package com.vivereply.roomready.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivereply.roomready.data.pms.PmsError
import com.vivereply.roomready.data.pms.PmsException
import com.vivereply.roomready.data.pms.QrError
import com.vivereply.roomready.data.pms.QrException
import com.vivereply.roomready.data.pms.QrPairing
import com.vivereply.roomready.data.pms.QrPairingParser
import com.vivereply.roomready.data.pms.SyncOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetupViewModel(private val orchestrator: SyncOrchestrator) : ViewModel() {

  var address: String by mutableStateOf("")
  var apiKey: String by mutableStateOf("")
  var staffName: String by mutableStateOf("")
  var connecting: Boolean by mutableStateOf(false)
    private set
  var error: PmsError? by mutableStateOf(null)
    private set
  var pasteError: QrError? by mutableStateOf(null)
    private set

  private val _connected = MutableStateFlow(false)
  val connected: StateFlow<Boolean> = _connected.asStateFlow()

  fun prefill(pairing: QrPairing) {
    address = pairing.baseUrl.origin
    apiKey = pairing.apiKey
    pasteError = null
    error = null
  }

  /** Returns true when the pasted code filled the form. */
  fun applyPastedCode(raw: String): Boolean {
    val result = QrPairingParser.parse(raw)
    result.fold(
      onSuccess = { prefill(it) },
      onFailure = { pasteError = (it as QrException).error },
    )
    return result.isSuccess
  }

  fun clearPasteError() {
    pasteError = null
  }

  fun connect() {
    if (connecting) return
    viewModelScope.launch {
      connecting = true
      error = null
      val result = orchestrator.validateAndConnect(address, apiKey, staffName)
      connecting = false
      result.fold(
        onSuccess = {
          apiKey = ""
          _connected.value = true
        },
        onFailure = { error = (it as? PmsException)?.error },
      )
    }
  }
}
