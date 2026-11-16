package com.vivereply.roomready.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

/** English/Bangla switch used on Profile (empty and connected states). */
@Composable
fun LanguageSelectorRow(
  currentLanguageTag: String,
  onLanguageSelected: (String) -> Unit,
  dark: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = stringResource(R.string.language_label),
      style = MaterialTheme.typography.titleSmall,
      color = if (dark) colors.onInk else colors.ink,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
      LanguageChoiceChip(
        label = stringResource(R.string.language_english),
        selected = !currentLanguageTag.startsWith("bn"),
        onClick = { onLanguageSelected("en") },
        testTag = "lang_chip_en",
        dark = dark,
      )
      LanguageChoiceChip(
        label = stringResource(R.string.language_bengali),
        selected = currentLanguageTag.startsWith("bn"),
        onClick = { onLanguageSelected("bn") },
        testTag = "lang_chip_bn",
        dark = dark,
      )
    }
  }
}

@Composable
private fun LanguageChoiceChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  dark: Boolean,
) {
  val colors = RoomReadyThemeTokens.colors
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
      )
    },
    colors =
      FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.mintSolid,
        selectedLabelColor = colors.ink,
        containerColor =
          if (dark) colors.inkSecondary.copy(alpha = 0.45f) else colors.surfaceSubtle,
        labelColor = if (dark) colors.onInk else colors.inkSecondary,
      ),
    border =
      FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = if (dark) colors.inkSecondary else colors.outline,
        selectedBorderColor = colors.mintSolid,
      ),
  )
}
