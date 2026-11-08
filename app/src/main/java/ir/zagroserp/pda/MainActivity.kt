package ir.zagroserp.pda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.prefs.ThemeMode
import ir.zagroserp.pda.location.LocationProvider
import ir.zagroserp.pda.location.LocationTrackingService
import ir.zagroserp.pda.push.PushTokenManager
import ir.zagroserp.pda.ui.navigation.AppNavHost
import ir.zagroserp.pda.ui.theme.ZagrosPDATheme
import ir.zagroserp.pda.ui.util.LocalServerOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Set when the activity is launched from a push notification. */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingRoute.value = intent?.getStringExtra(EXTRA_OPEN_ROUTE)

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val serverOrigin by appViewModel.serverOrigin.collectAsState()
            val openRoute by pendingRoute.collectAsState()

            ZagrosPDATheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalServerOrigin provides serverOrigin) {
                    AppNavHost(
                        openRoute = openRoute,
                        onRouteConsumed = { pendingRoute.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Tapping a second notification while the app is alive must still navigate.
        pendingRoute.value = intent.getStringExtra(EXTRA_OPEN_ROUTE)
    }

    companion object {
        const val EXTRA_OPEN_ROUTE = "open_route"
        const val ROUTE_MESSAGES = "messages"
    }
}

/**
 * Root-level app state: theme, the runtime server origin used to resolve asset URLs, and the
 * session-driven background services.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    sessionStore: SessionStore,
    serverConfigStore: ServerConfigStore,
    private val locationProvider: LocationProvider,
    private val pushTokenManager: PushTokenManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = sessionStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM,
    )

    val serverOrigin: StateFlow<String> = serverConfigStore.origin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "",
    )

    init {
        // Background work follows the session: location tracking and push registration both start
        // once the visitor is logged in and stop on logout.
        viewModelScope.launch {
            sessionStore.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    if (locationProvider.hasPermission()) {
                        LocationTrackingService.start(context)
                    }
                    pushTokenManager.syncToken()
                } else {
                    LocationTrackingService.stop(context)
                    pushTokenManager.unregister()
                }
            }
        }
    }
}
