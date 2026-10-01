package app.equinox.vpn.vpn

import android.content.Context
import app.equinox.vpn.data.Profile
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

enum class Phase { Disconnected, Connecting, Connected, Disconnecting }

data class VpnState(
    val phase: Phase = Phase.Disconnected,
    val profileName: String? = null,
    val connectedAt: Long = 0L,
)

data class Traffic(val rx: Long = 0, val tx: Long = 0)

/** Thin wrapper around the WireGuard Go backend. One tunnel at a time. */
class VpnController(context: Context) {
    private val backend: Backend by lazy { GoBackend(context.applicationContext) }

    private val _state = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private var tunnel: AppTunnel? = null

    private inner class AppTunnel(private val tunnelName: String) : Tunnel {
        override fun getName() = tunnelName
        override fun onStateChange(newState: Tunnel.State) {
            // Called by the backend, e.g. when the system revokes the VPN.
            if (newState == Tunnel.State.DOWN && tunnel === this) {
                _state.value = VpnState()
            }
        }
    }

    suspend fun connect(profile: Profile) = withContext(Dispatchers.IO) {
        _state.value = VpnState(Phase.Connecting, profile.name)
        try {
            val previous = tunnel
            if (previous != null && previous.name != profile.name) {
                backend.setState(previous, Tunnel.State.DOWN, null)
            }
            val next = previous?.takeIf { it.name == profile.name } ?: AppTunnel(profile.name)
            tunnel = next
            backend.setState(next, Tunnel.State.UP, profile.config)
            _state.value = VpnState(Phase.Connected, profile.name, System.currentTimeMillis())
        } catch (e: Exception) {
            tunnel = null
            _state.value = VpnState()
            throw IllegalStateException(describe(e), e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val current = tunnel ?: return@withContext
        _state.update { it.copy(phase = Phase.Disconnecting) }
        try {
            backend.setState(current, Tunnel.State.DOWN, null)
        } finally {
            tunnel = null
            _state.value = VpnState()
        }
    }

    suspend fun traffic(): Traffic = withContext(Dispatchers.IO) {
        val current = tunnel ?: return@withContext Traffic()
        runCatching {
            val stats = backend.getStatistics(current)
            Traffic(stats.totalRx(), stats.totalTx())
        }.getOrDefault(Traffic())
    }

    private fun describe(e: Exception): String = when (e) {
        is BackendException -> when (e.reason) {
            BackendException.Reason.VPN_NOT_AUTHORIZED -> "VPN permission was not granted"
            BackendException.Reason.UNABLE_TO_START_VPN -> "Android could not start the VPN"
            BackendException.Reason.DNS_RESOLUTION_FAILURE -> "Could not resolve the server address"
            BackendException.Reason.TUNNEL_MISSING_CONFIG -> "This profile has no configuration"
            else -> "Connection failed (${e.reason.name.lowercase().replace('_', ' ')})"
        }
        else -> e.message ?: "Connection failed"
    }
}
