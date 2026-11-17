package com.vivereply.roomready.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vivereply.roomready.R
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Finish-photo capture: preview, capture, review, use. Compression and
 * storage happen in the ViewModel via [com.vivereply.roomready.data.photos.PhotoStore].
 */
@Composable
fun FinishPhotoRoute(
  graph: EngineGraph,
  onPhotoReady: (File) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  @StringRes hintRes: Int = R.string.finish_photo,
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
    PhotoPermissionRationale(
      onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
      onClose = onClose,
      modifier = modifier,
    )
    return
  }
  FinishPhotoScreen(
    onCaptureFile = { graph.photos.newTempFile() },
    onLoadPreview = { file -> withContext(Dispatchers.IO) { graph.photos.loadTempThumbnail(file) } },
    onPhotoReady = onPhotoReady,
    onClose = onClose,
    hintRes = hintRes,
    modifier = modifier,
  )
}

@Composable
private fun PhotoPermissionRationale(onGrant: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
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
      modifier = Modifier.fillMaxWidth(),
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
private fun FinishPhotoScreen(
  onCaptureFile: () -> File,
  onLoadPreview: suspend (File) -> ImageBitmap?,
  onPhotoReady: (File) -> Unit,
  onClose: () -> Unit,
  @StringRes hintRes: Int,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val scope = rememberCoroutineScope()

  var captured by remember { mutableStateOf<File?>(null) }
  var preview by remember { mutableStateOf<ImageBitmap?>(null) }
  var capturing by remember { mutableStateOf(false) }
  var failed by remember { mutableStateOf(false) }
  var noCamera by remember { mutableStateOf(false) }
  val imageCapture = remember {
    ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
  }

  fun takePhoto() {
    if (capturing) return
    capturing = true
    failed = false
    val output = onCaptureFile()
    val options = ImageCapture.OutputFileOptions.Builder(output).build()
    imageCapture.takePicture(
      options,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(results: ImageCapture.OutputFileResults) {
          scope.launch {
            val bitmap = withContext(Dispatchers.IO) { onLoadPreview(output) }
            capturing = false
            if (bitmap != null) {
              captured = output
              preview = bitmap
            } else {
              failed = true
              output.delete()
            }
          }
        }

        override fun onError(exception: ImageCaptureException) {
          capturing = false
          failed = true
          output.delete()
        }
      },
    )
  }

  Box(modifier = modifier.fillMaxSize().background(colors.inkDeep)) {
    if (captured != null && preview != null) {
      Column(
        modifier = Modifier.fillMaxSize().padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Image(
          bitmap = preview!!,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(280.dp).clip(RoundedCornerShape(radius.lg)).testTag("photo_preview"),
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
          OutlinedButton(
            onClick = {
              captured?.delete()
              captured = null
              preview = null
            },
            modifier = Modifier.weight(1f),
          ) {
            Text(stringResource(R.string.finish_retake), color = colors.onInk)
          }
          Button(
            onClick = { captured?.let(onPhotoReady) },
            modifier = Modifier.weight(1f).testTag("photo_use"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = colors.mintSolid,
                contentColor = colors.ink,
              ),
          ) {
            Text(stringResource(R.string.finish_use_photo))
          }
        }
      }
      return@Box
    }

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
      return@Box
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
                provider.unbindAll()
                provider.bindToLifecycle(
                  lifecycleOwner,
                  CameraSelector.DEFAULT_BACK_CAMERA,
                  preview,
                  imageCapture,
                )
              } catch (e: Exception) {
                noCamera = true
              }
            },
            ContextCompat.getMainExecutor(factoryContext),
          )
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(spacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (failed) {
        Surface(
          shape = RoundedCornerShape(radius.md),
          color = colors.statusDirtyContainer,
        ) {
          Text(
            text = stringResource(R.string.finish_photo_error),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onStatusDirtyContainer,
            modifier = Modifier.padding(spacing.md),
            textAlign = TextAlign.Center,
          )
        }
        Spacer(modifier = Modifier.height(spacing.sm))
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        OutlinedButton(onClick = onClose, modifier = Modifier.testTag("photo_close")) {
          Text(stringResource(R.string.action_close), color = colors.onInk)
        }
        Button(
          onClick = ::takePhoto,
          enabled = !capturing,
          shape = CircleShape,
          contentPadding = PaddingValues(20.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = colors.mintSolid,
              contentColor = colors.ink,
            ),
          modifier =
            Modifier.size(84.dp).minimumInteractiveComponentSize().testTag("photo_capture"),
        ) {
          if (capturing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
          } else {
            Box(
              modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.ink),
            )
          }
        }
        Spacer(modifier = Modifier.width(96.dp))
      }
      Spacer(modifier = Modifier.height(spacing.sm))
      Text(
        text = stringResource(hintRes),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onInk,
        textAlign = TextAlign.Center,
      )
    }
  }
}
