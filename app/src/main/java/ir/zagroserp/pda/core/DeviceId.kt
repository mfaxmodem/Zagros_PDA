package ir.zagroserp.pda.core

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Stable per-install identifier sent to `auth/login` and `POST /mobile/visitor/devices`.
 * Falls back to a generated UUID stored in SharedPreferences when the ANDROID_ID is unavailable.
 */
object DeviceId {

    private const val PREFS = "zagros_device"
    private const val KEY = "device_id"

    @SuppressLint("HardwareIds")
    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }

        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        val id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId.take(128)
        } else {
            UUID.randomUUID().toString()
        }
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
