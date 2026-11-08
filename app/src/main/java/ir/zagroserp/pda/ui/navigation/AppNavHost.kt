package ir.zagroserp.pda.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.zagroserp.pda.ui.screens.catalog.CartScreen
import ir.zagroserp.pda.ui.screens.catalog.CatalogScreen
import ir.zagroserp.pda.ui.screens.customers.CustomerDetailScreen
import ir.zagroserp.pda.ui.screens.customers.CustomerEditScreen
import ir.zagroserp.pda.ui.screens.customers.CustomerFormScreen
import ir.zagroserp.pda.ui.screens.customers.CustomerListScreen
import ir.zagroserp.pda.ui.screens.dashboard.DashboardScreen
import ir.zagroserp.pda.ui.screens.invoices.InvoicesScreen
import ir.zagroserp.pda.ui.screens.login.LoginScreen
import ir.zagroserp.pda.ui.screens.messages.MessagesScreen
import ir.zagroserp.pda.ui.screens.orders.OrderDetailScreen
import ir.zagroserp.pda.ui.screens.orders.OrdersScreen
import ir.zagroserp.pda.ui.screens.permissions.PermissionsScreen
import ir.zagroserp.pda.ui.screens.permissions.ZonePermissionsScreen
import ir.zagroserp.pda.ui.screens.reports.ReportsScreen
import ir.zagroserp.pda.ui.screens.route.TodayRouteScreen
import ir.zagroserp.pda.ui.screens.server.ServerSetupScreen
import ir.zagroserp.pda.ui.screens.settings.SettingsScreen
import ir.zagroserp.pda.ui.screens.splash.SplashScreen

/** Opening a screen: quick, so the app stays responsive. */
private const val NAV_PUSH_MS = 240

/** Closing a screen with back: slower and fully visible, so it reads as "one window closing". */
private const val NAV_POP_MS = 320

private val NavEasing = androidx.compose.animation.core.FastOutSlowInEasing

/** Routes that only make sense once the visitor is authenticated. */
private val authenticatedRoutes = setOf(
    Routes.DASHBOARD,
    Routes.TODAY_ROUTE,
    Routes.CUSTOMERS,
    Routes.CATALOG,
    Routes.MESSAGES,
    Routes.ORDERS,
    Routes.INVOICES,
    Routes.REPORTS,
    Routes.ZONE_PERMISSIONS,
    Routes.SETTINGS,
    Routes.CART,
    Routes.CUSTOMER_NEW,
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    openRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // A notification tap requests a destination. It is applied only once the visitor is actually
    // inside the authenticated part of the graph, so a cold start still runs splash -> login.
    LaunchedEffect(openRoute, currentRoute) {
        if (openRoute == null) return@LaunchedEffect
        if (currentRoute !in authenticatedRoutes) return@LaunchedEffect
        navController.navigate(openRoute) { launchSingleTop = true }
        onRouteConsumed()
    }
    val showBottomBar = bottomNavItems.any { item ->
        backStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Screens own their own insets; the host only reserves space for the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ZagrosBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateToTab(route) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            val goHome: () -> Unit = {
                navController.popBackStack(Routes.DASHBOARD, inclusive = false)
            }

            CompositionLocalProvider(LocalGoHome provides goHome) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize(),
                // Direction-aware push/pop transitions.
                //
                // Push: the incoming screen slides in from the *end* edge while the outgoing one
                // drifts a quarter of the way toward the *start* edge (parallax), so the stack
                // reads as a stack.
                // Pop: exactly mirrored and given a slightly longer duration, so pressing back
                // visibly peels one screen off at a time instead of snapping.
                //
                // `full` is the screen width in pixels. In RTL the end edge is the left side, so
                // every offset is negated.
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(NAV_PUSH_MS, easing = NavEasing)) { full ->
                        if (isRtl) -full else full
                    } + fadeIn(animationSpec = tween(NAV_PUSH_MS, easing = NavEasing))
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(NAV_PUSH_MS, easing = NavEasing)) { full ->
                        if (isRtl) full / 4 else -full / 4
                    } + fadeOut(animationSpec = tween(NAV_PUSH_MS, easing = NavEasing))
                },
                popEnterTransition = {
                    slideInHorizontally(animationSpec = tween(NAV_POP_MS, easing = NavEasing)) { full ->
                        if (isRtl) full / 4 else -full / 4
                    } + fadeIn(animationSpec = tween(NAV_POP_MS, easing = NavEasing))
                },
                popExitTransition = {
                    slideOutHorizontally(animationSpec = tween(NAV_POP_MS, easing = NavEasing)) { full ->
                        if (isRtl) -full else full
                    } + fadeOut(animationSpec = tween(NAV_POP_MS, easing = NavEasing))
                },
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onLoggedIn = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                        onNeedsLogin = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                        onNeedsServerSetup = {
                            navController.navigate(Routes.SERVER_SETUP) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                        onNeedsPermissions = {
                            navController.navigate(Routes.PERMISSIONS) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.PERMISSIONS) {
                    PermissionsScreen(
                        onDone = {
                            navController.navigate(Routes.SERVER_SETUP) {
                                popUpTo(Routes.PERMISSIONS) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.SERVER_SETUP) {
                    ServerSetupScreen(
                        onReady = {
                            // The address may have changed, in which case the session was dropped
                            // by the view model – so clear the whole stack back to login.
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }

                composable(Routes.TODAY_ROUTE) {
                    TodayRouteScreen(
                        onCustomerClick = { id -> navController.navigate(Routes.customerDetail(id)) },
                        onCreateOrder = { id ->
                            navController.navigate(Routes.catalogForCustomer(id))
                        },
                    )
                }

                composable(Routes.CUSTOMERS) {
                    CustomerListScreen(
                        onCustomerClick = { id -> navController.navigate(Routes.customerDetail(id)) },
                        onAddCustomer = { navController.navigate(Routes.CUSTOMER_NEW) },
                    )
                }

                composable(
                    route = Routes.CUSTOMER_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.StringType }),
                ) { entry ->
                    val customerId = entry.arguments?.getString(Routes.ARG_CUSTOMER_ID).orEmpty()
                    CustomerDetailScreen(
                        customerId = customerId,
                        onBack = { navController.popBackStack() },
                        onCreateOrder = {
                            navController.navigate(Routes.catalogForCustomer(customerId))
                        },
                        onEdit = { navController.navigate(Routes.customerEdit(customerId)) },
                        onViewOrders = { navController.navigate(Routes.ORDERS) },
                        onViewPermissions = { navController.navigate(Routes.ZONE_PERMISSIONS) },
                    )
                }

                composable(
                    route = Routes.CUSTOMER_EDIT,
                    arguments = listOf(
                        navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.StringType },
                    ),
                ) {
                    CustomerEditScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.CUSTOMER_NEW) {
                    CustomerFormScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = { id ->
                            navController.popBackStack()
                            if (id.isNotBlank()) navController.navigate(Routes.customerDetail(id))
                        },
                    )
                }

                composable(Routes.CATALOG) {
                    CatalogScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCart = { navController.navigate(Routes.CART) },
                    )
                }

                composable(
                    route = Routes.CATALOG_FOR_CUSTOMER,
                    arguments = listOf(
                        navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.StringType },
                    ),
                ) {
                    CatalogScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCart = { navController.navigate(Routes.CART) },
                    )
                }

                composable(Routes.CART) {
                    CartScreen(
                        onBack = { navController.popBackStack() },
                        onOrderPlaced = {
                            navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                        },
                    )
                }

                composable(Routes.ORDERS) {
                    OrdersScreen(
                        onBack = { navController.popBackStack() },
                        onOrderClick = { id -> navController.navigate(Routes.orderDetail(id)) },
                    )
                }

                composable(
                    route = Routes.ORDER_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_ORDER_ID) { type = NavType.StringType }),
                ) { entry ->
                    val orderId = entry.arguments?.getString(Routes.ARG_ORDER_ID).orEmpty()
                    OrderDetailScreen(orderId = orderId, onBack = { navController.popBackStack() })
                }

                composable(Routes.INVOICES) {
                    InvoicesScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.REPORTS) {
                    ReportsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.MESSAGES) {
                    MessagesScreen(
                        onOpenOrders = { navController.navigate(Routes.ORDERS) },
                    )
                }

                composable(Routes.ZONE_PERMISSIONS) {
                    ZonePermissionsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onChangeServer = {
                            navController.navigate(Routes.SERVER_SETUP) {
                                popUpTo(Routes.SERVER_SETUP) { inclusive = true }
                            }
                        },
                        onLoggedOut = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun ZagrosBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            // NavigationBar applies NavigationBarDefaults.windowInsets (navigation bars) itself.
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val isCenter = item.route == bottomNavCenterRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(item.route) },
                    icon = {
                        if (isCenter) {
                            // The dashboard is the hub – give it a filled, raised badge so it
                            // reads as the centre of the bar.
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = item.emoji, style = MaterialTheme.typography.titleSmall)
                            }
                        } else {
                            Text(
                                text = item.emoji,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected || isCenter) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/**
 * Switches bottom-bar tabs.
 *
 * `popUpTo` targets the **dashboard**, not `graph.findStartDestination()`. The graph starts at
 * the splash, but the splash is popped with `inclusive = true` once boot finishes, so it is no
 * longer on the back stack — and `popUpTo` on an id that is absent pops *the whole stack*. That
 * was silently wiping every pushed screen whenever a tab was tapped, which is why back no longer
 * closed screens one at a time.
 *
 * The dashboard is guaranteed to be the stack root after login, so this pops exactly the screens
 * above it and keeps the rest of the stack intact.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.DASHBOARD) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
