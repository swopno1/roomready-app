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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vivereply.roomready.R
import com.vivereply.roomready.data.EngineGraph
import com.vivereply.roomready.ui.components.LogoLockupVariant
import com.vivereply.roomready.ui.components.RoomReadyLogoLockup
import com.vivereply.roomready.ui.navigation.RoomReadyDestination
import com.vivereply.roomready.ui.navigation.RoomReadyRoute
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.vivereply.roomready.ui.screens.ActivityRoute
import com.vivereply.roomready.ui.screens.DetailRoute
import com.vivereply.roomready.ui.screens.FinishPhotoRoute
import com.vivereply.roomready.ui.screens.HomeRoute
import com.vivereply.roomready.ui.screens.InspectRoute
import com.vivereply.roomready.ui.screens.TicketFormRoute
import com.vivereply.roomready.ui.screens.ProfileRoute
import com.vivereply.roomready.ui.screens.QrScanScreen
import com.vivereply.roomready.ui.screens.RoomsRoute
import com.vivereply.roomready.ui.screens.SetupRoute
import com.vivereply.roomready.ui.screens.TabEmptyStateScreen
import com.vivereply.roomready.ui.screens.TicketsRoute
import com.vivereply.roomready.ui.state.ActivityFeedViewModel
import com.vivereply.roomready.ui.state.BoardViewModel
import com.vivereply.roomready.ui.state.ConnectionViewModel
import com.vivereply.roomready.ui.state.DetailViewModel
import com.vivereply.roomready.ui.state.DetailViewModelFactory
import com.vivereply.roomready.ui.state.GraphViewModelFactory
import com.vivereply.roomready.ui.state.SetupViewModel
import com.vivereply.roomready.ui.state.TicketsViewModel
import com.vivereply.roomready.ui.theme.RoomReadyThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomReadyApp(
  graph: EngineGraph,
  currentLanguageTag: String,
  onLanguageSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = RoomReadyThemeTokens.colors
  val spacing = RoomReadyThemeTokens.spacing
  val radius = RoomReadyThemeTokens.radius

  val factory = remember(graph) { GraphViewModelFactory(graph) }
  val connection: ConnectionViewModel = viewModel(factory = factory)
  val setup: SetupViewModel = viewModel(factory = factory)
  val board: BoardViewModel = viewModel(factory = factory)
  val activityFeed: ActivityFeedViewModel = viewModel(factory = factory)
  val tickets: TicketsViewModel = viewModel(factory = factory)
  val connected by connection.status.collectAsStateWithLifecycle()

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: RoomReadyDestination.Home.route
  val onFullScreenRoute =
    currentRoute == RoomReadyRoute.SETUP || currentRoute == RoomReadyRoute.QR_SCAN ||
      currentRoute.startsWith("room/") || currentRoute.startsWith("ticket/")

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
      if (useRail && !onFullScreenRoute) {
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
                          Modifier.size(8.dp).clip(CircleShape)
                            .background(if (connected.connected) colors.mintSolid else colors.inkMuted),
                      )
                      Text(
                        text =
                          stringResource(
                            if (connected.connected) R.string.profile_connected
                            else R.string.status_offline_not_linked,
                          ),
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
          if (!useRail && !onFullScreenRoute) {
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
          composable(RoomReadyDestination.Home.route) {
            if (connected.connected) {
              HomeRoute(
                connection = connection,
                board = board,
                languageTag = currentLanguageTag,
                onFilterSelected = { filter ->
                  board.setFilter(filter)
                  navigateToTab(RoomReadyDestination.Rooms)
                },
                onOpenDetail = { unitId -> navController.navigate(RoomReadyRoute.room(unitId)) },
                onOpenInspect = { unitId -> navController.navigate(RoomReadyRoute.inspect(unitId)) },
              )
            } else {
              TabEmptyStateScreen(
                destination = RoomReadyDestination.Home,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = { navigateToTab(RoomReadyDestination.Profile) },
              )
            }
          }
          composable(RoomReadyDestination.Rooms.route) {
            if (connected.connected) {
              RoomsRoute(
                board = board,
                connection = connection,
                languageTag = currentLanguageTag,
                onOpenDetail = { unitId -> navController.navigate(RoomReadyRoute.room(unitId)) },
                onOpenInspect = { unitId -> navController.navigate(RoomReadyRoute.inspect(unitId)) },
              )
            } else {
              TabEmptyStateScreen(
                destination = RoomReadyDestination.Rooms,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = { navigateToTab(RoomReadyDestination.Profile) },
              )
            }
          }
          composable(RoomReadyDestination.Tickets.route) {
            if (connected.connected) {
              TicketsRoute(
                viewModel = tickets,
                onNewTicket = { navController.navigate(RoomReadyRoute.ticketForm(null)) },
              )
            } else {
              TabEmptyStateScreen(
                destination = RoomReadyDestination.Tickets,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = { navigateToTab(RoomReadyDestination.Profile) },
              )
            }
          }
          composable(RoomReadyDestination.Activity.route) {
            if (connected.connected) {
              ActivityRoute(
                viewModel = activityFeed,
                connection = connection,
                languageTag = currentLanguageTag,
              )
            } else {
              TabEmptyStateScreen(
                destination = RoomReadyDestination.Activity,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = { navigateToTab(RoomReadyDestination.Profile) },
              )
            }
          }
          composable(RoomReadyDestination.Profile.route) {
            if (connected.connected) {
              ProfileRoute(
                graph = graph,
                connection = connection,
                languageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onEditConnection = { navController.navigate(RoomReadyRoute.SETUP) },
              )
            } else {
              TabEmptyStateScreen(
                destination = RoomReadyDestination.Profile,
                currentLanguageTag = currentLanguageTag,
                onLanguageSelected = onLanguageSelected,
                onPrimaryAction = { navController.navigate(RoomReadyRoute.SETUP) },
              )
            }
          }
          composable(RoomReadyRoute.SETUP) {
            SetupRoute(
              viewModel = setup,
              onConnected = {
                connection.refresh()
                board.refresh()
                navController.popBackStack()
                navigateToTab(RoomReadyDestination.Home)
              },
              onScanQr = { navController.navigate(RoomReadyRoute.QR_SCAN) },
            )
          }
          composable(RoomReadyRoute.QR_SCAN) {
            QrScanScreen(
              onPairing = { pairing ->
                setup.prefill(pairing)
                navController.popBackStack()
              },
              onClose = { navController.popBackStack() },
            )
          }
          composable(RoomReadyRoute.ROOM_PATTERN) { entry ->
            val unitId = entry.arguments?.getString("unitId") ?: return@composable
            val detailVm: DetailViewModel =
              viewModel(
                key = "detail_$unitId",
                factory = remember(graph, unitId) { DetailViewModelFactory(graph, unitId) },
              )
            DetailRoute(
              viewModel = detailVm,
              graph = graph,
              languageTag = currentLanguageTag,
              onTakePhoto = { navController.navigate(RoomReadyRoute.photo(unitId)) },
              onOpenInspect = { id -> navController.navigate(RoomReadyRoute.inspect(id)) },
              onRaiseTicket = { id ->
                navController.navigate(RoomReadyRoute.ticketForm(id))
              },
              onClose = { navController.popBackStack() },
            )
          }
          composable(RoomReadyRoute.INSPECT_PATTERN) { entry ->
            val unitId = entry.arguments?.getString("unitId") ?: return@composable
            val inspectVm: DetailViewModel =
              viewModel(
                key = "inspect_$unitId",
                factory = remember(graph, unitId) { DetailViewModelFactory(graph, unitId) },
              )
            InspectRoute(
              viewModel = inspectVm,
              graph = graph,
              languageTag = currentLanguageTag,
              onClose = { navController.popBackStack() },
            )
          }
          composable(
            route = RoomReadyRoute.TICKET_FORM_PATTERN,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType; nullable = true }),
          ) { entry ->
            TicketFormRoute(
              tickets = tickets,
              board = board,
              graph = graph,
              preselectedUnitId = entry.arguments?.getString("unitId"),
              onTakePhoto = { navController.navigate(RoomReadyRoute.TICKET_PHOTO) },
              onRaised = { navController.popBackStack() },
              onBack = { navController.popBackStack() },
            )
          }
          composable(RoomReadyRoute.TICKET_PHOTO) {
            FinishPhotoRoute(
              graph = graph,
              onPhotoReady = { file ->
                tickets.attachTempPhoto(file)
                navController.popBackStack()
              },
              onClose = { navController.popBackStack() },
              hintRes = R.string.ticket_photo_hint,
            )
          }
          composable(RoomReadyRoute.PHOTO_PATTERN) { entry ->
            val unitId = entry.arguments?.getString("unitId") ?: return@composable
            // Share the detail screen's ViewModel so the captured photo lands in its state.
            val detailEntry =
              remember(entry) { requireNotNull(navController.previousBackStackEntry) }
            val detailVm: DetailViewModel =
              viewModel(
                viewModelStoreOwner = detailEntry,
                key = "detail_$unitId",
                factory = remember(graph, unitId) { DetailViewModelFactory(graph, unitId) },
              )
            FinishPhotoRoute(
              graph = graph,
              onPhotoReady = { file ->
                detailVm.onPhotoCaptured(file)
                navController.popBackStack()
              },
              onClose = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}
