package com.vivereply.roomready.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val RoomReadyLightColorScheme =
  lightColorScheme(
    primary = ViveReplyInk,
    onPrimary = PureWhite,
    primaryContainer = MintWash,
    onPrimaryContainer = ViveReplyInk,
    secondary = MintAccessibleInk,
    onSecondary = PureWhite,
    secondaryContainer = WarmSurfaceSubtle,
    onSecondaryContainer = ViveReplyInk,
    tertiary = StatusCleaned,
    onTertiary = PureWhite,
    tertiaryContainer = StatusCleanedContainer,
    onTertiaryContainer = OnStatusCleanedContainer,
    background = WarmCanvas,
    onBackground = ViveReplyInk,
    surface = WarmSurface,
    onSurface = ViveReplyInk,
    surfaceVariant = WarmSurfaceSubtle,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = WarmSurface,
    surfaceContainerLow = WarmCanvas,
    surfaceContainer = WarmSurfaceSubtle,
    surfaceContainerHigh = WarmSurfaceMuted,
    surfaceContainerHighest = WarmOutlineSubtle,
    outline = WarmOutline,
    outlineVariant = WarmOutlineSubtle,
    error = StatusDirty,
    onError = PureWhite,
    errorContainer = StatusDirtyContainer,
    onErrorContainer = OnStatusDirtyContainer,
  )

private val RoomReadyShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8),
    small = RoundedCornerShape(12),
    medium = RoundedCornerShape(16),
    large = RoundedCornerShape(24),
    extraLarge = RoundedCornerShape(28),
  )

@Composable
fun RoomReadyTheme(content: @Composable () -> Unit) {
  val spacing = RoomReadySpacing()
  val radius = RoomReadyRadius()
  val elevation = RoomReadyElevation()

  CompositionLocalProvider(
    LocalRoomReadyColors provides DefaultRoomReadyColors,
    LocalRoomReadySpacing provides spacing,
    LocalRoomReadyRadius provides radius,
    LocalRoomReadyElevation provides elevation,
  ) {
    MaterialTheme(
      colorScheme = RoomReadyLightColorScheme,
      typography = RoomReadyTypography,
      shapes = RoomReadyShapes,
      content = content,
    )
  }
}
