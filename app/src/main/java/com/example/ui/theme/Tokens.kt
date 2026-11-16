package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RoomReadyColors(
  val ink: Color,
  val inkDeep: Color,
  val inkSecondary: Color,
  val inkMuted: Color,
  val onInk: Color,
  val mintStart: Color,
  val mintEnd: Color,
  val mintSolid: Color,
  val mintAccessibleInk: Color,
  val mintWash: Color,
  val mintWashStrong: Color,
  val canvas: Color,
  val surface: Color,
  val surfaceSubtle: Color,
  val surfaceMuted: Color,
  val outline: Color,
  val outlineSubtle: Color,
  val doorPanelShadow: Color,
  val statusDirty: Color,
  val statusDirtyContainer: Color,
  val onStatusDirtyContainer: Color,
  val statusCleaning: Color,
  val statusCleaningContainer: Color,
  val onStatusCleaningContainer: Color,
  val statusCleaned: Color,
  val statusCleanedContainer: Color,
  val onStatusCleanedContainer: Color,
  val statusReady: Color,
  val statusReadyAccessible: Color,
  val statusReadyContainer: Color,
  val onStatusReadyContainer: Color,
) {
  val mintGradient: Brush
    get() = Brush.linearGradient(colors = listOf(mintStart, mintEnd))
}

@Immutable
data class RoomReadySpacing(
  val xxs: Dp = 4.dp,
  val xs: Dp = 8.dp,
  val sm: Dp = 12.dp,
  val md: Dp = 16.dp,
  val lg: Dp = 24.dp,
  val xl: Dp = 32.dp,
  val xxl: Dp = 48.dp,
)

@Immutable
data class RoomReadyRadius(
  val xs: Dp = 8.dp,
  val sm: Dp = 12.dp,
  val md: Dp = 16.dp,
  val lg: Dp = 24.dp,
  val pill: Dp = 999.dp,
)

@Immutable
data class RoomReadyElevation(
  val flat: Dp = 0.dp,
  val card: Dp = 1.dp,
  val raised: Dp = 3.dp,
  val nav: Dp = 6.dp,
)

internal val DefaultRoomReadyColors =
  RoomReadyColors(
    ink = ViveReplyInk,
    inkDeep = ViveReplyInkDeep,
    inkSecondary = InkSecondary,
    inkMuted = InkMuted,
    onInk = PureWhite,
    mintStart = MintStart,
    mintEnd = MintEnd,
    mintSolid = MintSolid,
    mintAccessibleInk = MintAccessibleInk,
    mintWash = MintWash,
    mintWashStrong = MintWashStrong,
    canvas = WarmCanvas,
    surface = WarmSurface,
    surfaceSubtle = WarmSurfaceSubtle,
    surfaceMuted = WarmSurfaceMuted,
    outline = WarmOutline,
    outlineSubtle = WarmOutlineSubtle,
    doorPanelShadow = WarmOutlineSubtle,
    statusDirty = StatusDirty,
    statusDirtyContainer = StatusDirtyContainer,
    onStatusDirtyContainer = OnStatusDirtyContainer,
    statusCleaning = StatusCleaning,
    statusCleaningContainer = StatusCleaningContainer,
    onStatusCleaningContainer = OnStatusCleaningContainer,
    statusCleaned = StatusCleaned,
    statusCleanedContainer = StatusCleanedContainer,
    onStatusCleanedContainer = OnStatusCleanedContainer,
    statusReady = StatusReady,
    statusReadyAccessible = StatusReadyAccessible,
    statusReadyContainer = StatusReadyContainer,
    onStatusReadyContainer = OnStatusReadyContainer,
  )

val LocalRoomReadyColors = staticCompositionLocalOf { DefaultRoomReadyColors }
val LocalRoomReadySpacing = staticCompositionLocalOf { RoomReadySpacing() }
val LocalRoomReadyRadius = staticCompositionLocalOf { RoomReadyRadius() }
val LocalRoomReadyElevation = staticCompositionLocalOf { RoomReadyElevation() }

object RoomReadyThemeTokens {
  val colors: RoomReadyColors
    @Composable @ReadOnlyComposable get() = LocalRoomReadyColors.current

  val spacing: RoomReadySpacing
    @Composable @ReadOnlyComposable get() = LocalRoomReadySpacing.current

  val radius: RoomReadyRadius
    @Composable @ReadOnlyComposable get() = LocalRoomReadyRadius.current

  val elevation: RoomReadyElevation
    @Composable @ReadOnlyComposable get() = LocalRoomReadyElevation.current
}
