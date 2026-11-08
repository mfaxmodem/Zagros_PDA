package ir.zagroserp.pda.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.data.prefs.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PermissionKind(val label: String, val rationale: String, val required: Boolean) {
    LOCATION(
        label = "موقعیت مکانی",
        rationale = "برای ثبت بازدید، اعتبارسنجی محدوده‌ی تور و ایجاد مشتری لازم است.",
        required = true,
    ),
    NOTIFICATIONS(
        label = "اعلان‌ها",
        rationale = "برای نمایش وضعیت ردیابی موقعیت و اعلان‌های مسیر.",
        required = false,
    ),
    BACKGROUND_LOCATION(
        label = "موقعیت در پس‌زمینه",
        rationale = "تا هنگام بسته بودن اپ هم مسیر حرکت شما ثبت شود.",
        required = false,
    ),
    BLUETOOTH(
        label = "بلوتوث",
        rationale = "برای اتصال به چاپگر فاکتور.",
        required = false,
    ),
}

data class PermissionRow(
    val kind: PermissionKind,
    val granted: Boolean,
)

data class PermissionsUiState(
    val rows: List<PermissionRow> = emptyList(),
    val requestedOnce: Boolean = false,
) {
    val requiredGranted: Boolean
        get() = rows.filter { it.kind.required }.all { it.granted }

    val allGranted: Boolean get() = rows.all { it.granted }
}

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsUiState())
    val state: StateFlow<PermissionsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(rows = currentRows()) }
    }

    /**
     * Android 11+ requires background location to be requested on its own, *after* foreground
     * location has been granted. Bluetooth is optional and requested lazily when printing.
     */
    fun permissionsToRequest(): Array<String> {
        val missing = currentRows().filter { !it.granted && it.kind != PermissionKind.BLUETOOTH }
        val foreground = missing.filter { it.kind != PermissionKind.BACKGROUND_LOCATION }
        val background = missing.filter { it.kind == PermissionKind.BACKGROUND_LOCATION }

        val list = mutableListOf<String>()
        if (foreground.isNotEmpty()) {
            foreground.forEach { row ->
                when (row.kind) {
                    PermissionKind.LOCATION -> {
                        list += Manifest.permission.ACCESS_FINE_LOCATION
                        list += Manifest.permission.ACCESS_COARSE_LOCATION
                    }

                    PermissionKind.NOTIFICATIONS -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            list += Manifest.permission.POST_NOTIFICATIONS
                        }
                    }

                    else -> Unit
                }
            }
            return list.toTypedArray()
        }

        if (background.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        return emptyArray()
    }

    fun onRequestFinished() {
        _state.update { it.copy(requestedOnce = true) }
        refresh()
    }

    fun requestBluetooth(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        emptyArray()
    }

    fun markOnboarded(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionStore.setOnboarded(true)
            onDone()
        }
    }

    private fun currentRows(): List<PermissionRow> = PermissionKind.entries.map { kind ->
        PermissionRow(kind = kind, granted = isGranted(kind))
    }

    private fun isGranted(kind: PermissionKind): Boolean = when (kind) {
        PermissionKind.LOCATION -> {
            granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        PermissionKind.NOTIFICATIONS -> {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                granted(Manifest.permission.POST_NOTIFICATIONS)
        }

        PermissionKind.BACKGROUND_LOCATION -> {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        PermissionKind.BLUETOOTH -> {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                granted(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
