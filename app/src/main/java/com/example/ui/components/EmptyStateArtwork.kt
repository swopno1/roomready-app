package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.RoomReadyDestination
import com.example.ui.theme.RoomReadyThemeTokens

@Composable
fun EmptyStateArtwork(
  destination: RoomReadyDestination,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors

  Canvas(
    modifier =
      modifier
        .size(width = 188.dp, height = 136.dp)
        .testTag("empty_artwork_${destination.route}")
        .semantics { this.contentDescription = contentDescription },
  ) {
    val w = size.width
    val h = size.height

    // Warm organic backdrop arch shared across all room/door motifs
    drawRoundRect(
      color = colors.surfaceSubtle,
      topLeft = Offset(w * 0.10f, h * 0.08f),
      size = Size(w * 0.80f, h * 0.82f),
      cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
    )

    // Subtle floor threshold line
    drawLine(
      color = colors.outline,
      start = Offset(w * 0.16f, h * 0.82f),
      end = Offset(w * 0.84f, h * 0.82f),
      strokeWidth = 3.dp.toPx(),
      cap = StrokeCap.Round,
    )

    when (destination) {
      RoomReadyDestination.Home -> {
        // Central sanctuary doorway + 4-stage turnover status chips motif
        drawRoundRect(
          color = colors.ink,
          topLeft = Offset(w * 0.34f, h * 0.18f),
          size = Size(w * 0.32f, h * 0.64f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        val door =
          Path().apply {
            moveTo(w * 0.35f, h * 0.20f)
            lineTo(w * 0.56f, h * 0.24f)
            lineTo(w * 0.56f, h * 0.78f)
            lineTo(w * 0.35f, h * 0.81f)
            close()
          }
        drawPath(door, color = colors.surface)
        drawPath(
          door,
          color = colors.ink,
          style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round),
        )
        drawCircle(
          color = colors.ink,
          radius = 2.8.dp.toPx(),
          center = Offset(w * 0.51f, h * 0.51f),
        )

        // Emerging mint glow arc inside open doorway
        drawLine(
          brush = Brush.verticalGradient(listOf(colors.mintStart, colors.mintEnd)),
          start = Offset(w * 0.61f, h * 0.27f),
          end = Offset(w * 0.61f, h * 0.73f),
          strokeWidth = 4.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Four semantic status badges floating neatly on the right board card
        drawRoundRect(
          color = colors.surface,
          topLeft = Offset(w * 0.68f, h * 0.26f),
          size = Size(w * 0.18f, h * 0.42f),
          cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        )
        drawRoundRect(
          color = colors.outline,
          topLeft = Offset(w * 0.68f, h * 0.26f),
          size = Size(w * 0.18f, h * 0.42f),
          cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          style = Stroke(width = 1.5.dp.toPx()),
        )
        val statusDots =
          listOf(
            colors.statusDirty,
            colors.statusCleaning,
            colors.statusCleaned,
            colors.statusReady,
          )
        statusDots.forEachIndexed { index, dotColor ->
          drawCircle(
            color = dotColor,
            radius = 4.2.dp.toPx(),
            center = Offset(w * 0.77f, h * (0.33f + index * 0.09f)),
          )
        }
      }

      RoomReadyDestination.Rooms -> {
        // Three resort villa doors in a clean corridor rhythm
        val doorXs = listOf(0.22f, 0.41f, 0.60f)
        doorXs.forEachIndexed { idx, xFrac ->
          val isCenter = idx == 1
          val doorTop = if (isCenter) 0.18f else 0.24f
          val doorHeight = 0.82f - doorTop
          drawRoundRect(
            color = if (isCenter) colors.ink else colors.surface,
            topLeft = Offset(w * xFrac, h * doorTop),
            size = Size(w * 0.17f, h * doorHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
          )
          drawRoundRect(
            color = colors.ink,
            topLeft = Offset(w * xFrac, h * doorTop),
            size = Size(w * 0.17f, h * doorHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
          )
          if (isCenter) {
            val ajar =
              Path().apply {
                moveTo(w * (xFrac + 0.01f), h * (doorTop + 0.02f))
                lineTo(w * (xFrac + 0.13f), h * (doorTop + 0.06f))
                lineTo(w * (xFrac + 0.13f), h * 0.78f)
                lineTo(w * (xFrac + 0.01f), h * 0.81f)
                close()
              }
            drawPath(ajar, color = colors.surface)
            val check =
              Path().apply {
                moveTo(w * 0.45f, h * 0.50f)
                lineTo(w * 0.50f, h * 0.56f)
                lineTo(w * 0.59f, h * 0.42f)
              }
            drawPath(
              check,
              brush = Brush.linearGradient(listOf(colors.mintStart, colors.mintEnd)),
              style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          } else {
            drawCircle(
              color = colors.inkMuted,
              radius = 2.2.dp.toPx(),
              center = Offset(w * (xFrac + 0.13f), h * 0.54f),
            )
          }
        }
      }

      RoomReadyDestination.Tickets -> {
        // Doorway with a hanging service ticket tag
        drawRoundRect(
          color = colors.surface,
          topLeft = Offset(w * 0.30f, h * 0.18f),
          size = Size(w * 0.34f, h * 0.64f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawRoundRect(
          color = colors.ink,
          topLeft = Offset(w * 0.30f, h * 0.18f),
          size = Size(w * 0.34f, h * 0.64f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          style = Stroke(width = 2.4.dp.toPx()),
        )
        // Door handle hook
        drawCircle(
          color = colors.ink,
          radius = 3.5.dp.toPx(),
          center = Offset(w * 0.56f, h * 0.47f),
        )
        // Service ticket tag hanging beside the door
        drawRoundRect(
          color = colors.statusCleaningContainer,
          topLeft = Offset(w * 0.53f, h * 0.49f),
          size = Size(w * 0.22f, h * 0.25f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawRoundRect(
          color = colors.statusCleaning,
          topLeft = Offset(w * 0.53f, h * 0.49f),
          size = Size(w * 0.22f, h * 0.25f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          style = Stroke(width = 2.dp.toPx()),
        )
        // Tag lines
        drawLine(
          color = colors.ink,
          start = Offset(w * 0.58f, h * 0.57f),
          end = Offset(w * 0.70f, h * 0.57f),
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = colors.inkSecondary,
          start = Offset(w * 0.58f, h * 0.64f),
          end = Offset(w * 0.67f, h * 0.64f),
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      RoomReadyDestination.Activity -> {
        // Doorway paired with a crisp turnover timeline rail
        drawRoundRect(
          color = colors.surface,
          topLeft = Offset(w * 0.24f, h * 0.20f),
          size = Size(w * 0.28f, h * 0.62f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawRoundRect(
          color = colors.ink,
          topLeft = Offset(w * 0.24f, h * 0.20f),
          size = Size(w * 0.28f, h * 0.62f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          style = Stroke(width = 2.4.dp.toPx()),
        )
        drawCircle(
          color = colors.ink,
          radius = 2.6.dp.toPx(),
          center = Offset(w * 0.46f, h * 0.52f),
        )

        // Vertical timeline rail + 3 nodes
        drawLine(
          color = colors.outline,
          start = Offset(w * 0.64f, h * 0.26f),
          end = Offset(w * 0.64f, h * 0.74f),
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        val nodeYs = listOf(0.30f, 0.50f, 0.70f)
        nodeYs.forEachIndexed { idx, yFrac ->
          val nodeColor =
            when (idx) {
              0 -> colors.mintSolid
              1 -> colors.statusCleaned
              else -> colors.surface
            }
          drawCircle(
            color = nodeColor,
            radius = 5.5.dp.toPx(),
            center = Offset(w * 0.64f, h * yFrac),
          )
          drawCircle(
            color = colors.ink,
            radius = 5.5.dp.toPx(),
            center = Offset(w * 0.64f, h * yFrac),
            style = Stroke(width = 2.dp.toPx()),
          )
          drawLine(
            color = colors.inkSecondary,
            start = Offset(w * 0.70f, h * yFrac),
            end = Offset(w * 0.80f, h * yFrac),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }

      RoomReadyDestination.Profile -> {
        // Staff badge overlapping an ink Ready Door emblem
        drawRoundRect(
          color = colors.ink,
          topLeft = Offset(w * 0.24f, h * 0.20f),
          size = Size(w * 0.30f, h * 0.62f),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        val door =
          Path().apply {
            moveTo(w * 0.26f, h * 0.22f)
            lineTo(w * 0.46f, h * 0.26f)
            lineTo(w * 0.46f, h * 0.76f)
            lineTo(w * 0.26f, h * 0.80f)
            close()
          }
        drawPath(door, color = colors.surface)

        // Staff ID card overlapping right edge
        drawRoundRect(
          color = colors.surface,
          topLeft = Offset(w * 0.44f, h * 0.32f),
          size = Size(w * 0.34f, h * 0.44f),
          cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        )
        drawRoundRect(
          color = colors.ink,
          topLeft = Offset(w * 0.44f, h * 0.32f),
          size = Size(w * 0.34f, h * 0.44f),
          cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          style = Stroke(width = 2.2.dp.toPx()),
        )
        // Avatar circle + mint badge bar
        drawCircle(
          color = colors.mintWash,
          radius = 8.dp.toPx(),
          center = Offset(w * 0.61f, h * 0.46f),
        )
        drawCircle(
          color = colors.ink,
          radius = 8.dp.toPx(),
          center = Offset(w * 0.61f, h * 0.46f),
          style = Stroke(width = 1.8.dp.toPx()),
        )
        drawLine(
          brush = Brush.horizontalGradient(listOf(colors.mintStart, colors.mintEnd)),
          start = Offset(w * 0.52f, h * 0.62f),
          end = Offset(w * 0.70f, h * 0.62f),
          strokeWidth = 4.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
  }
}
