package com.vivereply.roomready.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivereply.roomready.R
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** Motif kinds matched generically from the type name/code keywords. */
enum class TypeMotif {
  VILLA,
  COTTAGE,
  SUITE,
  LOFT,
  LODGE,
  HOUSE,
}

fun motifKindFor(typeName: String, code: String): TypeMotif {
  val haystack = "$typeName $code".lowercase()
  return when {
    "cottage" in haystack -> TypeMotif.COTTAGE
    "villa" in haystack -> TypeMotif.VILLA
    "suite" in haystack -> TypeMotif.SUITE
    "loft" in haystack -> TypeMotif.LOFT
    "lodge" in haystack -> TypeMotif.LODGE
    else -> TypeMotif.HOUSE
  }
}

/** Designed group header: mint gradient band, ink type, vector motif. */
@Composable
fun TypeHeader(
  type: RoomType,
  languageTag: String,
  count: Int,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius
  val displayName = if (languageTag.startsWith("bn")) type.nameBn else type.nameEn
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(radius.md))
        .background(Brush.horizontalGradient(listOf(colors.mintStart, colors.mintEnd)))
        .testTag("type_header_${type.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Text(
          text = displayName,
          style = MaterialTheme.typography.titleLarge,
          color = colors.ink,
        )
        Text(
          text = pluralStringResource(R.plurals.rooms_type_count, count, count),
          style = MaterialTheme.typography.labelLarge,
          color = colors.ink,
        )
      }
      TypeMotifMark(
        kind = motifKindFor(type.nameEn, type.code),
        tint = colors.ink,
        size = 56.dp,
      )
    }
  }
}

@Composable
fun TypeMotifMark(kind: TypeMotif, tint: Color, size: Dp, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(size)) {
    val s = size.toPx() / 64f
    val stroke = Stroke(width = 3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun rect(l: Float, t: Float, r: Float, b: Float) =
      drawRect(tint, Offset(l * s, t * s), Size((r - l) * s, (b - t) * s), style = stroke)
    fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) {
      drawPath(
        Path().apply {
          moveTo(ax * s, ay * s)
          lineTo(bx * s, by * s)
          lineTo(cx * s, cy * s)
          close()
        },
        tint,
        style = stroke,
      )
    }
    when (kind) {
      TypeMotif.VILLA -> {
        rect(10f, 24f, 54f, 28f)
        rect(14f, 28f, 50f, 56f)
        drawArc(
          tint,
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(26f * s, 38f * s),
          size = Size(12f * s, 12f * s),
          style = stroke,
        )
        drawLine(tint, Offset(26f * s, 44f * s), Offset(26f * s, 56f * s), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(38f * s, 44f * s), Offset(38f * s, 56f * s), stroke.width, StrokeCap.Round)
        drawCircle(tint, 2.5f * s, Offset(20f * s, 36f * s), style = stroke)
      }
      TypeMotif.COTTAGE -> {
        tri(8f, 32f, 32f, 8f, 56f, 32f)
        rect(18f, 32f, 46f, 56f)
        rect(28f, 42f, 36f, 56f)
      }
      TypeMotif.SUITE -> {
        rect(18f, 8f, 46f, 32f)
        rect(18f, 34f, 46f, 56f)
        rect(23f, 13f, 28f, 18f)
        rect(36f, 13f, 41f, 18f)
        rect(23f, 41f, 28f, 46f)
        rect(36f, 41f, 41f, 46f)
      }
      TypeMotif.LOFT -> {
        drawPath(
          Path().apply {
            moveTo(10f * s, 26f * s)
            lineTo(54f * s, 16f * s)
            lineTo(54f * s, 23f * s)
            lineTo(10f * s, 33f * s)
            close()
          },
          tint,
          style = stroke,
        )
        rect(16f, 33f, 48f, 56f)
        rect(24f, 38f, 31f, 51f)
      }
      TypeMotif.LODGE -> {
        rect(40f, 12f, 46f, 26f)
        tri(8f, 34f, 32f, 18f, 56f, 34f)
        rect(14f, 34f, 50f, 56f)
        rect(28f, 43f, 36f, 56f)
        drawLine(tint, Offset(14f * s, 42f * s), Offset(28f * s, 42f * s), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(36f * s, 42f * s), Offset(50f * s, 42f * s), stroke.width, StrokeCap.Round)
      }
      TypeMotif.HOUSE -> {
        tri(10f, 30f, 32f, 12f, 54f, 30f)
        rect(18f, 30f, 46f, 56f)
        rect(28f, 42f, 36f, 56f)
      }
    }
  }
}
