package com.vivereply.roomready.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.components.LogoLockupVariant
import com.vivereply.roomready.ui.components.RoomReadyLogoLockup
import com.vivereply.roomready.ui.navigation.RoomReadyDestination
import com.vivereply.roomready.ui.screens.TabEmptyStateScreen
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomReadyApp(
  currentLanguageTag: String,
  onLanguageSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: RoomReadyDestination.Home.route

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val connectFeedbackMessage = stringResource(R.string.action_connect_prompt_feedback)

  fun navigateToTab(destination: RoomReadyDestination) {
    navController.navigate(destination.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.canvas)) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail) {
        NavigationRail(
          containerColor = colors.surface,
          contentColor = colors.ink,
          modifier = Modifier.fillMaxHeight().testTag("navigation_rail"),
        ) {
          Spacer(modifier = Modifier.weight(1f))
          RoomReadyDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(destination) },
              modifier = Modifier.testTag("nav_item_${destination.route}"),
              icon = {
                Icon(
                  imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = colors.ink,
                  selectedTextColor = colors.ink,
                  indicatorColor = colors.mintSolid,
                  unselectedIconColor = colors.inkMuted,
                  unselectedTextColor = colors.inkMuted,
                ),
            )
          }
          Spacer(modifier = Modifier.weight(1f))
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = colors.canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          Surface(color = colors.canvas) {
            Box(modifier = Modifier.fillMaxWidth()) {
              TopAppBar(
                colors =
                  TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.canvas,
                    titleContentColor = colors.ink,
                  ),
                title = {
                  RoomReadyLogoLockup(
                    variant = LogoLockupVariant.InkOnLight,
                    markSize = 36.dp,
                  )
                },
                actions = {
                  Surface(
                    modifier = Modifier.padding(end = spacing.md),
                    shape = RoundedCornerShape(radius.pill),
                    color = colors.surfaceSubtle,
                    border = BorderStroke(1.dp, colors.outline),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(8.dp).clip(CircleShape).background(colors.inkMuted),
                      )
                      Text(
                        text = stringResource(R.string.status_offline_not_linked),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkSecondary,
                      )
                    }
                  }
                },
              )
              HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = colors.outlineSubtle,
              )
            }
          }
        },
        bottomBar = {
          if (!useRail) {
            Surface(
              color = colors.surface,
              tonalElevation = RoomReadyThemeTokens.elevation.flat,
              shadowElevation = RoomReadyThemeTokens.elevation.nav,
            ) {
              Box {
                HorizontalDivider(thickness = 1.dp, color = colors.outlineSubtle)
                NavigationBar(
                  containerColor = colors.surface,
                  contentColor = colors.ink,
                  tonalElevation = RoomReadyThemeTokens.elevation.flat,
                  modifier = Modifier.testTag("bottom_navigation_bar"),
                ) {
                  RoomReadyDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                      selected = selected,
                      onClick = { navigateToTab(destination) },
                      modifier = Modifier.testTag("nav_item_${destination.route}"),
                      icon = {
                        Icon(
                          imageVector =
                            if (selected) destination.selectedIcon else destination.unselectedIcon,
                          contentDescription = label,
                        )
                      },
                      label = {
                        Text(
                          text = label,
                          style = MaterialTheme.typography.labelMedium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      },
                      alwaysShowLabel = true,
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = colors.ink,
                          selectedTextColor = colors.ink,
                          indicatorColor = colors.mintSolid,
                          unselectedIconColor = colors.inkMuted,
                          unselectedTextColor = colors.inkMuted,
                        ),
                    )
                  }
                }
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = RoomReadyDestination.Home.route,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          RoomReadyDestination.entries.forEach { destination ->
            composable(destination.route) {
              TabEmptyStateScreen(
                destination = destination,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = {
                  if (destination == RoomReadyDestination.Profile) {
                    coroutineScope.launch {
                      snackbarHostState.currentSnackbarData?.dismiss()
                      snackbarHostState.showSnackbar(connectFeedbackMessage)
                    }
                  } else {
                    navigateToTab(RoomReadyDestination.Profile)
                  }
                },
              )
            }
          }
        }
      }
    }
  }
}
