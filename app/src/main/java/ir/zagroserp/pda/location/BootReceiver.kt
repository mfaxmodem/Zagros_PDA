package ir.zagroserp.pda.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import ir.zagroserp.pda.data.prefs.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resumes location tracking after a reboot or an app update.
 *
 * Without this the service only started when the visitor opened the app, so a device that
 * rebooted mid-shift silently stopped reporting positions until someone tapped the icon — the
 * backend would just see a gap with no explanation.
 *
 * `location` is one of the foreground-service types Android still allows to start from
 * BOOT_COMPLETED; the call is wrapped defensively anyway because OEM builds vary.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var locationProvider: LocationProvider

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loggedIn = sessionStore.isLoggedIn.first()
                if (loggedIn && locationProvider.hasPermission()) {
                    LocationTrackingService.start(context)
                }
            } catch (t: Throwable) {
                // Some OEM ROMs refuse a foreground-service start from boot. Tracking resumes
                // the next time the app is opened, which is the pre-existing behaviour.
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }
}
