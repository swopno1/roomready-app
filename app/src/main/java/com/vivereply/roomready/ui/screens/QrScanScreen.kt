package com.vivereply.roomready.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vivereply.roomready.R
import com.vivereply.roomready.data.pms.QrError
import com.vivereply.roomready.data.pms.QrException
import com.vivereply.roomready.data.pms.QrPairing
import com.vivereply.roomready.data.pms.QrPairingParser
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/**
 * QR pairing scan. Decoding runs fully on-device (bundled ML Kit model);
 * a valid code fills address + key on the setup form.
 */
@Composable
fun QrScanScreen(
  onPairing: (QrPairing) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted
    }

  if (!hasPermission) {
    PermissionRationale(
      onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
      onClose = onClose,
      modifier = modifier,
    )
    return
  }
  CameraScanContent(onPairing = onPairing, onClose = onClose, modifier = modifier)
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  Column(
    modifier = modifier.fillMaxSize().background(colors.canvas).padding(spacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(R.string.qr_title),
      style = MaterialTheme.typography.headlineSmall,
      color = colors.ink,
    )
    Spacer(modifier = Modifier.height(spacing.sm))
    Text(
      text = stringResource(R.string.qr_need_permission),
      style = MaterialTheme.typography.bodyMedium,
      color = colors.inkSecondary,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(spacing.md))
    Button(
      onClick = onGrant,
      modifier = Modifier.fillMaxWidth().testTag("qr_grant"),
      colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
    ) {
      Text(stringResource(R.string.qr_grant))
    }
    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.action_close))
    }
  }
}

@Composable
private fun CameraScanContent(
  onPairing: (QrPairing) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  var qrError by remember { mutableStateOf<QrError?>(null) }
  var noCamera by remember { mutableStateOf(false) }
  var handled by remember { mutableStateOf(false) }
  val scanner = remember {
    BarcodeScanning.getClient(
      BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
    )
  }
  DisposableEffect(scanner) {
    onDispose { scanner.close() }
  }

  Box(modifier = modifier.fillMaxSize().background(colors.inkDeep)) {
    if (noCamera) {
      Column(
        modifier = Modifier.fillMaxSize().padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(R.string.qr_no_camera),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onInk,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.md))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.action_close))
        }
      }
      return
    }

    AndroidView(
      factory = { factoryContext ->
        PreviewView(factoryContext).also { previewView ->
          val providerFuture = ProcessCameraProvider.getInstance(factoryContext)
          providerFuture.addListener(
            {
              try {
                val provider = providerFuture.get()
                val preview =
                  androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                  }
                val analysis =
                  ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(factoryContext)) { proxy ->
                  analyzeProxy(proxy, scanner) { raw ->
                    QrPairingParser.parse(raw).fold(
                      onSuccess = { pairing ->
                        if (!handled) {
                          handled = true
                          onPairing(pairing)
                        }
                      },
                      onFailure = { qrError = (it as QrException).error },
                    )
                  }
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                  lifecycleOwner,
                  CameraSelector.DEFAULT_BACK_CAMERA,
                  preview,
                  analysis,
                )
              } catch (e: Exception) {
                noCamera = true
              }
            },
            ContextCompat.getMainExecutor(factoryContext),
          )
        }
      },
      modifier = Modifier.fillMaxSize().testTag("qr_preview"),
    )

    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(spacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      qrError?.let { error ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(radius.md),
          color = colors.statusDirtyContainer,
        ) {
          Text(
            text = stringResource(error.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onStatusDirtyContainer,
            modifier = Modifier.padding(spacing.md).testTag("qr_error"),
            textAlign = TextAlign.Center,
          )
        }
        Spacer(modifier = Modifier.height(spacing.sm))
      }
      Text(
        text = stringResource(R.string.qr_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onInk,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(spacing.sm))
      OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_close), color = colors.onInk)
      }
    }
  }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analyzeProxy(
  proxy: ImageProxy,
  scanner: BarcodeScanner,
  onRawValue: (String) -> Unit,
) {
  val mediaImage = proxy.image
  if (mediaImage == null) {
    proxy.close()
    return
  }
  val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
  scanner.process(input)
    .addOnSuccessListener { barcodes ->
      barcodes.firstOrNull()?.rawValue?.let(onRawValue)
    }.addOnCompleteListener {
      proxy.close()
    }
}
