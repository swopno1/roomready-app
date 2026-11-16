package com.vivereply.roomready.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/**
 * Loading shimmer whose placeholders mirror the board geometry: one header
 * block plus unit cards with rail, lines, and action. Calm alpha pulse.
 */
@Composable
fun BoardShimmer(modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val pulse by rememberInfiniteTransition(label = "shimmer").animateFloat(
    initialValue = 0.35f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(animation = androidx.compose.animation.core.tween(900), repeatMode = RepeatMode.Reverse),
    label = "shimmerAlpha",
  )
  Column(
    modifier = modifier.fillMaxWidth().alpha(pulse).testTag("board_shimmer"),
    verticalArrangement = Arrangement.spacedBy(spacing.md),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().height(72.dp)
          .clip(RoundedCornerShape(radius.md))
          .backgroundPlaceholder(),
    )
    repeat(3) {
      Row(
        modifier =
          Modifier.fillMaxWidth().height(148.dp)
            .clip(RoundedCornerShape(radius.lg))
            .backgroundPlaceholder(),
      ) {
        Box(modifier = Modifier.width(8.dp).backgroundPlaceholder())
        Column(
          modifier = Modifier.weight(1f).padding(spacing.md),
          verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
          Box(modifier = Modifier.fillMaxWidth(0.5f).height(22.dp).backgroundPlaceholder())
          Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).backgroundPlaceholder())
          Spacer(modifier = Modifier.weight(1f))
          Box(modifier = Modifier.fillMaxWidth().height(48.dp).backgroundPlaceholder())
        }
      }
    }
  }
}

@Composable
private fun Modifier.backgroundPlaceholder(): Modifier =
  this.background(RoomReadyThemeTokens.colors.surfaceMuted)

/** Designed empty state: mint check medallion, title, and inviting copy. */
@Composable
fun AllCaughtUp(title: String, copy: String, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  Column(
    modifier = modifier.fillMaxWidth().padding(spacing.lg).testTag("board_caught_up"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Canvas(modifier = Modifier.size(72.dp)) {
      drawCircle(colors.mintSolid, size.minDimension / 2f)
      val s = size.minDimension / 72f
      drawPath(
        Path().apply {
          moveTo(24f * s, 37f * s)
          lineTo(33f * s, 46f * s)
          lineTo(49f * s, 27f * s)
        },
        colors.ink,
        style = Stroke(width = 7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }
    Spacer(modifier = Modifier.height(spacing.md))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = colors.ink,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(spacing.xs))
    Text(
      text = copy,
      style = MaterialTheme.typography.bodyMedium,
      color = colors.inkSecondary,
      textAlign = TextAlign.Center,
    )
  }
}

/** Board error state with retry. */
@Composable
fun BoardLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  Surface(
    modifier = modifier.fillMaxWidth().testTag("board_error"),
    shape = RoundedCornerShape(radius.lg),
    color = colors.statusDirtyContainer,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(spacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.rooms_load_error),
        style = MaterialTheme.typography.titleLarge,
        color = colors.onStatusDirtyContainer,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(spacing.md))
      Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(radius.md),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = colors.ink,
            contentColor = colors.onInk,
          ),
      ) {
        Text(stringResource(R.string.action_retry))
      }
    }
  }
}
