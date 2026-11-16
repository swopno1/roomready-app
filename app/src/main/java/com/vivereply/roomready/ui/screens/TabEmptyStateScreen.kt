package com.vivereply.roomready.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.components.EmptyStateArtwork
import com.vivereply.roomready.ui.components.LanguageSelectorRow
import com.vivereply.roomready.ui.components.LogoLockupVariant
import com.vivereply.roomready.ui.components.RoomReadyLogoLockup
import com.vivereply.roomready.ui.navigation.RoomReadyDestination
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@Composable
fun TabEmptyStateScreen(
  destination: RoomReadyDestination,
  currentLanguageTag: String,
  onLanguageSelected: (String) -> Unit,
  onPrimaryAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  Box(
    modifier = modifier.fillMaxSize().background(colors.canvas),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = spacing.lg, vertical = spacing.md),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (destination == RoomReadyDestination.Profile) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = spacing.md),
          shape = RoundedCornerShape(radius.lg),
          color = colors.ink,
          tonalElevation = RoomReadyThemeTokens.elevation.flat,
          border = BorderStroke(1.dp, colors.inkSecondary),
        ) {
          Column(modifier = Modifier.padding(spacing.md)) {
            RoomReadyLogoLockup(
              variant = LogoLockupVariant.WhiteOnInk,
              markSize = 40.dp,
              showTagline = true,
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            LanguageSelectorRow(
              currentLanguageTag = currentLanguageTag,
              onLanguageSelected = onLanguageSelected,
              dark = true,
            )
          }
        }
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius.lg),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineSubtle),
        shadowElevation = RoomReadyThemeTokens.elevation.card,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.lg),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          EmptyStateArtwork(
            destination = destination,
            contentDescription = stringResource(destination.emptyArtworkCdRes),
          )

          Spacer(modifier = Modifier.height(spacing.md))

          Text(
            text = stringResource(destination.emptyTitleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("empty_title_${destination.route}"),
          )

          Spacer(modifier = Modifier.height(spacing.xs))

          Text(
            text = stringResource(destination.emptyCopyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("empty_copy_${destination.route}"),
          )

          Spacer(modifier = Modifier.height(spacing.md))

          Button(
            onClick = onPrimaryAction,
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .minimumInteractiveComponentSize()
                .testTag("empty_action_${destination.route}"),
            shape = RoundedCornerShape(radius.md),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = colors.ink,
                contentColor = colors.onInk,
              ),
          ) {
            Box(
              modifier = Modifier.size(10.dp).clip(CircleShape).background(colors.mintSolid),
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Text(
              text = stringResource(destination.emptyActionRes),
              style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = colors.mintSolid,
            )
          }
        }
      }
    }
  }
}

