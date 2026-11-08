package ir.zagroserp.pda.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.ServerAddress
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.remote.ProbeResult
import ir.zagroserp.pda.data.remote.ServerProbe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerSetupUiState(
    val address: String = "",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val probe: ProbeResult? = null,
    val error: String? = null,
) {
    /** The visitor may only continue once *this* address has answered successfully. */
    val canContinue: Boolean
        get() = probe is ProbeResult.Success && !isTesting && !isSaving

    val serverVersion: String
        get() = (probe as? ProbeResult.Success)?.serverVersion.orEmpty()

    val minAppVersion: String
        get() = (probe as? ProbeResult.Success)?.minAppVersion.orEmpty()

    val isSecure: Boolean get() = ServerAddress.isSecure(address)

    /** Canonical form of whatever is typed, shown as a confirmation under the field. */
    val normalized: String get() = ServerAddress.normalize(address).orEmpty()
}

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val serverProbe: ServerProbe,
    private val serverConfigStore: ServerConfigStore,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerSetupUiState())
    val state: StateFlow<ServerSetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = serverConfigStore.currentBaseUrl()
            _state.update {
                it.copy(
                    address = if (stored == ir.zagroserp.pda.BuildConfig.BASE_URL) "" else stored,
                )
            }
        }
    }

    fun onAddressChange(value: String) {
        // Any edit invalidates a previous successful test.
        _state.update { it.copy(address = value, probe = null, error = null) }
    }

    fun useAddress(value: String) {
        _state.update { it.copy(address = value, probe = null, error = null) }
    }

    fun testConnection() {
        val current = _state.value
        if (current.isTesting) return

        val validationError = ServerAddress.validate(current.address)
        if (validationError != null) {
            _state.update { it.copy(error = validationError, probe = null) }
            return
        }

        _state.update { it.copy(isTesting = true, probe = null, error = null) }
        viewModelScope.launch {
            val result = serverProbe.probe(current.address)
            _state.update { it.copy(isTesting = false, probe = result) }
        }
    }

    /**
     * Persists the address and drops the session when the host actually changed — a token issued
     * by one company's server is meaningless on another.
     */
    fun saveAndContinue(onReady: () -> Unit) {
        val current = _state.value
        if (!current.canContinue) return

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val changed = serverConfigStore.save(current.address)
            if (changed) {
                sessionStore.clear()
            }
            _state.update { it.copy(isSaving = false) }
            onReady()
        }
    }
}
