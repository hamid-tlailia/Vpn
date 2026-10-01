package app.equinox.vpn.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.equinox.vpn.data.Profile
import app.equinox.vpn.ui.components.Glass
import app.equinox.vpn.ui.components.SkyBackground
import app.equinox.vpn.ui.components.SkyOrb
import app.equinox.vpn.ui.theme.Sky
import app.equinox.vpn.vpn.Phase
import app.equinox.vpn.vpn.Traffic
import app.equinox.vpn.vpn.VpnState
import kotlinx.coroutines.delay
import java.util.Locale

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onConnect: () -> Unit,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
) {
    val state by vm.vpnState.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val traffic by vm.traffic.collectAsStateWithLifecycle()
    val callsOnly by vm.callsOnly.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    HomeContent(
        state = state,
        profiles = profiles,
        selected = selected,
        traffic = traffic,
        callsOnly = callsOnly,
        snackbar = snackbar,
        onConnect = onConnect,
        onDisconnect = vm::disconnect,
        onSelect = vm::select,
        onDelete = vm::delete,
        onScanQr = onScanQr,
        onImportFile = onImportFile,
        onPaste = vm::importText,
        onCallsOnlyChange = vm::setCallsOnly,
    )
}

@Composable
fun HomeContent(
    state: VpnState,
    profiles: List<Profile>,
    selected: Profile?,
    traffic: Traffic,
    callsOnly: Boolean,
    snackbar: SnackbarHostState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSelect: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
    onPaste: (String, String) -> Boolean,
    onCallsOnlyChange: (Boolean) -> Unit,
    initiallyShowSheet: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    var showSheet by rememberSaveable { mutableStateOf(initiallyShowSheet) }

    val connected = state.phase == Phase.Connected
    val busy = state.phase == Phase.Connecting || state.phase == Phase.Disconnecting
    val day by animateFloatAsState(
        targetValue = when (state.phase) {
            Phase.Connected -> 1f
            Phase.Connecting -> 0.35f
            Phase.Disconnecting -> 0.5f
            Phase.Disconnected -> 0f
        },
        animationSpec = tween(1400),
        label = "day",
    )

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(connected) {
        while (connected) { now = System.currentTimeMillis(); delay(1000) }
    }

    Box(Modifier.fillMaxSize()) {
        SkyBackground(day, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(connected, day)

            Spacer(Modifier.weight(0.6f))

            val headline = when {
                selected == null -> "Add a server"
                state.phase == Phase.Connecting -> "Connecting"
                state.phase == Phase.Disconnecting -> "Disconnecting"
                connected -> "Protected"
                else -> "Not protected"
            }
            Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Sky.TextMuted)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = headline,
                transitionSpec = {
                    (fadeIn(tween(400)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(250)) + slideOutVertically { -it / 3 })
                },
                label = "headline",
            ) { text ->
                Text(text, style = MaterialTheme.typography.headlineMedium, color = Sky.TextPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    connected -> formatDuration(now - state.connectedAt)
                    state.phase == Phase.Connecting -> "Securing your connection…"
                    state.phase == Phase.Disconnecting -> "Closing the tunnel…"
                    selected == null -> "Tap to begin"
                    else -> "Tap to connect"
                },
                style = MaterialTheme.typography.bodyMedium.merge(Tabular),
                color = lerp(Sky.TextSecondary, Sky.Dawn, day),
            )

            SkyOrb(
                day = day,
                busy = busy,
                label = if (connected) "Disconnect" else "Connect",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    when {
                        selected == null -> showSheet = true
                        busy -> Unit
                        connected -> onDisconnect()
                        else -> onConnect()
                    }
                },
                modifier = Modifier.size(300.dp),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Rounded.ArrowDownward, "Download", formatBytes(traffic.rx), day, Modifier.weight(1f))
                StatCard(Icons.Rounded.ArrowUpward, "Upload", formatBytes(traffic.tx), day, Modifier.weight(1f))
            }

            Spacer(Modifier.weight(0.4f))

            CallsOnlyCard(callsOnly, enabled = !busy, onChange = onCallsOnlyChange)
            Spacer(Modifier.height(12.dp))
            ServerCard(selected, onClick = { showSheet = true })
            Spacer(Modifier.height(8.dp))
        }

        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(16.dp),
        ) {
            Snackbar(
                it,
                containerColor = Sky.Sheet,
                contentColor = Sky.TextPrimary,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }

    if (showSheet) {
        ServersSheet(
            profiles = profiles,
            selected = selected,
            activeName = state.profileName.takeIf { state.phase != Phase.Disconnected },
            onSelect = { onSelect(it); showSheet = false },
            onDelete = onDelete,
            onScanQr = { showSheet = false; onScanQr() },
            onImportFile = { showSheet = false; onImportFile() },
            onPaste = onPaste,
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun TopBar(connected: Boolean, day: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("EQUINOX", style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = Sky.TextPrimary)
            Text("Private · No ads · No logs", style = MaterialTheme.typography.bodyMedium, color = Sky.TextMuted)
        }
        val pill = lerp(Sky.Glass, Sky.Gold.copy(alpha = 0.18f), day)
        Row(
            Modifier
                .clip(CircleShape)
                .background(pill)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(lerp(Sky.TextMuted, Sky.Gold, day)),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (connected) "SECURE" else "OFF",
                style = MaterialTheme.typography.labelMedium,
                color = lerp(Sky.TextSecondary, Sky.Dawn, day),
            )
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, day: Float, modifier: Modifier) {
    Glass(modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(lerp(Sky.Moon.copy(alpha = 0.12f), Sky.Gold.copy(alpha = 0.2f), day)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = lerp(Sky.Moon, Sky.Gold, day))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Sky.TextMuted)
                Text(value, style = MaterialTheme.typography.titleMedium.merge(Tabular), color = Sky.TextPrimary)
            }
        }
    }
}

@Composable
private fun CallsOnlyCard(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Glass(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Call, null, tint = if (checked) Sky.Gold else Sky.Moon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Calls only", style = MaterialTheme.typography.titleMedium, color = Sky.TextPrimary)
                Text(
                    if (checked) "Only WhatsApp & Messenger" else "All apps go through the VPN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sky.TextSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Sky.Midnight,
                    checkedTrackColor = Sky.Gold,
                    uncheckedThumbColor = Sky.Moon,
                    uncheckedTrackColor = Sky.Glass,
                    uncheckedBorderColor = Sky.GlassBorder,
                ),
            )
        }
    }
}

@Composable
private fun ServerCard(profile: Profile?, onClick: () -> Unit) {
    Glass(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Sky.Twilight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (profile == null) Icons.Rounded.Shield else Icons.Rounded.Dns, null, tint = Sky.Moon)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.name ?: "Add your server",
                    style = MaterialTheme.typography.titleMedium,
                    color = Sky.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    profile?.endpoint ?: "Scan a QR code or import a .conf file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sky.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Sky.TextMuted)
        }
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

internal fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

