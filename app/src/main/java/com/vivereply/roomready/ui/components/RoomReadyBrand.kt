package com.vivereply.roomready.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

enum class LogoLockupVariant {
  InkOnLight,
  WhiteOnInk,
}

@Composable
fun ReadyDoorMark(
  modifier: Modifier = Modifier,
  size: Dp = 36.dp,
  variant: LogoLockupVariant = LogoLockupVariant.InkOnLight,
) {
  val colors = RoomReadyThemeTokens.colors
  val containerColor =
    when (variant) {
      LogoLockupVariant.InkOnLight -> colors.ink
      LogoLockupVariant.WhiteOnInk -> colors.inkDeep
    }
  val frameStroke =
    when (variant) {
      LogoLockupVariant.InkOnLight -> colors.inkSecondary
      LogoLockupVariant.WhiteOnInk -> colors.mintSolid.copy(alpha = 0.45f)
    }

  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(RoomReadyThemeTokens.radius.sm))
        .background(containerColor)
        .border(width = 1.dp, color = frameStroke, shape = RoundedCornerShape(RoomReadyThemeTokens.radius.sm)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size * 0.86f)) {
      val w = this.size.width
      val h = this.size.height

      // Subtle mint radial glow behind ajar door
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                colors.mintStart.copy(alpha = 0.28f),
                colors.mintEnd.copy(alpha = 0.08f),
                colors.ink.copy(alpha = 0f),
              ),
            center = Offset(w * 0.58f, h * 0.50f),
            radius = w * 0.48f,
          ),
        radius = w * 0.48f,
        center = Offset(w * 0.58f, h * 0.50f),
      )

      // Doorway threshold opening
      drawRoundRect(
        color = colors.inkDeep,
        topLeft = Offset(w * 0.24f, h * 0.14f),
        size = Size(w * 0.48f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = colors.inkSecondary,
        topLeft = Offset(w * 0.24f, h * 0.14f),
        size = Size(w * 0.48f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        style = Stroke(width = w * 0.03f),
      )

      // White door slab slightly ajar
      val doorPath =
        Path().apply {
          moveTo(w * 0.24f, h * 0.16f)
          lineTo(w * 0.57f, h * 0.24f)
          lineTo(w * 0.57f, h * 0.76f)
          lineTo(w * 0.24f, h * 0.84f)
          close()
        }
      drawPath(path = doorPath, color = colors.onInk)
      drawPath(
        path = doorPath,
        color = colors.ink,
        style = Stroke(width = w * 0.045f, join = StrokeJoin.Round),
      )

      // Door handle
      drawCircle(
        color = colors.ink,
        radius = w * 0.032f,
        center = Offset(w * 0.49f, h * 0.50f),
      )

      // Emerging checkmark with ink keyline halo + mint gradient
      val checkPath =
        Path().apply {
          moveTo(w * 0.40f, h * 0.50f)
          lineTo(w * 0.53f, h * 0.63f)
          lineTo(w * 0.80f, h * 0.34f)
        }
      drawPath(
        path = checkPath,
        color = colors.ink,
        style =
          Stroke(
            width = w * 0.145f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
      )
      drawPath(
        path = checkPath,
        brush =
          Brush.linearGradient(
            colors = listOf(colors.mintStart, colors.mintEnd),
            start = Offset(w * 0.40f, h * 0.60f),
            end = Offset(w * 0.80f, h * 0.34f),
          ),
        style =
          Stroke(
            width = w * 0.082f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
      )
    }
  }
}

@Composable
fun RoomReadyLogoLockup(
  variant: LogoLockupVariant,
  modifier: Modifier = Modifier,
  markSize: Dp = 36.dp,
  showTagline: Boolean = false,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val logoCd = stringResource(R.string.brand_lockup_content_description)
  val primaryTextColor =
    when (variant) {
      LogoLockupVariant.InkOnLight -> colors.ink
      LogoLockupVariant.WhiteOnInk -> colors.onInk
    }
  val readyAccentColor =
    when (variant) {
      LogoLockupVariant.InkOnLight -> colors.mintAccessibleInk
      LogoLockupVariant.WhiteOnInk -> colors.mintSolid
    }
  val taglineColor =
    when (variant) {
      LogoLockupVariant.InkOnLight -> colors.inkMuted
      LogoLockupVariant.WhiteOnInk -> colors.mintEnd.copy(alpha = 0.85f)
    }

  val tagName =
    when (variant) {
      LogoLockupVariant.InkOnLight -> "logo_lockup_ink_on_light"
      LogoLockupVariant.WhiteOnInk -> "logo_lockup_white_on_ink"
    }

  Row(
    modifier =
      modifier
        .testTag(tagName)
        .semantics(mergeDescendants = true) { contentDescription = logoCd },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
  ) {
    ReadyDoorMark(size = markSize, variant = variant)
    Spacer(modifier = Modifier.width(spacing.sm))
    Column(verticalArrangement = Arrangement.Center) {
      Text(
        text =
          buildAnnotatedString {
            withStyle(SpanStyle(color = primaryTextColor, fontWeight = FontWeight.ExtraBold)) {
              append("Room")
            }
            withStyle(SpanStyle(color = readyAccentColor, fontWeight = FontWeight.Bold)) {
              append("Ready")
            }
          },
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontSize = 21.sp,
            letterSpacing = (-0.4).sp,
          ),
        maxLines = 1,
      )
      // Mint accent vector keyline underline under the lockup
      Canvas(modifier = Modifier.padding(top = 2.dp).width(68.dp).height(3.dp)) {
        drawRoundRect(
          brush =
            Brush.horizontalGradient(
              colors = listOf(colors.mintStart, colors.mintEnd),
            ),
          cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
        )
      }
      if (showTagline) {
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
          text = stringResource(R.string.brand_tagline),
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
          color = taglineColor,
          maxLines = 1,
        )
      }
    }
  }
}
