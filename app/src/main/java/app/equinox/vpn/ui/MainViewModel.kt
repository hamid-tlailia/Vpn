package app.equinox.vpn.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.equinox.vpn.EquinoxApp
import app.equinox.vpn.data.Profile
import app.equinox.vpn.data.WarpRegistrar
import app.equinox.vpn.vpn.Phase
import app.equinox.vpn.vpn.Traffic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as EquinoxApp).profiles
    private val vpn = (app as EquinoxApp).vpn

    val vpnState = vpn.state
    val profiles = store.profiles
    val callsOnly = store.callsOnly

    val selected: StateFlow<Profile?> = combine(store.profiles, store.selected) { list, name ->
        list.firstOrNull { it.name == name } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _traffic = MutableStateFlow(Traffic())
    val traffic: StateFlow<Traffic> = _traffic.asStateFlow()

    private val _warpBusy = MutableStateFlow(false)
    val warpBusy: StateFlow<Boolean> = _warpBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            while (isActive) {
                _traffic.value = if (vpn.state.value.phase == Phase.Connected) vpn.traffic() else Traffic()
                delay(1000)
            }
        }
    }

    /** Called after VPN permission is granted (or already held). */
    fun connect() {
        val profile = selected.value ?: return
        viewModelScope.launch {
            runCatching { vpn.connect(profile, store.callsOnly.value) }.onFailure { _messages.tryEmit(it.message ?: "Connection failed") }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            runCatching { vpn.disconnect() }.onFailure { _messages.tryEmit("Could not disconnect cleanly") }
        }
    }

    fun setCallsOnly(enabled: Boolean) {
        store.setCallsOnly(enabled)
        // Re-apply routing to a live tunnel.
        if (vpnState.value.phase == Phase.Connected) connect()
    }

    fun select(profile: Profile) {
        val wasConnected = vpnState.value.phase == Phase.Connected
        store.select(profile.name)
        if (wasConnected && vpnState.value.profileName != profile.name) connect()
    }

    fun delete(profile: Profile) {
        viewModelScope.launch {
            if (vpnState.value.profileName == profile.name && vpnState.value.phase != Phase.Disconnected) {
                runCatching { vpn.disconnect() }
            }
            store.delete(profile.name)
        }
    }

    fun importText(name: String, text: String): Boolean = try {
        val p = store.add(name, text)
        store.select(p.name)
        _messages.tryEmit("Added ${p.name}")
        true
    } catch (e: Exception) {
        _messages.tryEmit("That isn't a valid WireGuard config")
        false
    }

    /** One tap: create a free Cloudflare WARP account for this device and add it. */
    fun getFreeServer() {
        if (_warpBusy.value) return
        _warpBusy.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { WarpRegistrar.register() } }
            _warpBusy.value = false
            result.onSuccess { importText("WARP", it) }
                .onFailure { _messages.tryEmit("Couldn't reach Cloudflare. Try another network.") }
        }
    }

    fun importUri(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "server"
                    val text = ctx.contentResolver.openInputStream(uri)!!.use { it.reader().readText() }
                    name to text
                }
            }
            result.onSuccess { (name, text) -> importText(name, text) }
                .onFailure { _messages.tryEmit("Couldn't read that file") }
        }
    }
}
