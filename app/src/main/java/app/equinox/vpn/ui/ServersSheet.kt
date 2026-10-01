package app.equinox.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.equinox.vpn.data.Profile
import app.equinox.vpn.ui.theme.Sky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersSheet(
    profiles: List<Profile>,
    selected: Profile?,
    activeName: String?,
    onSelect: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
    onPaste: (String, String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var showPaste by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sky.Sheet,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(40.dp, 4.dp)
                    .clip(CircleShape)
                    .background(Sky.GlassBorder),
            )
        },
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Servers", style = MaterialTheme.typography.headlineMedium, color = Sky.TextPrimary)
            Text(
                "Your own WireGuard servers. Nothing else, no one else.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sky.TextSecondary,
            )
            Spacer(Modifier.height(20.dp))

            if (profiles.isEmpty()) {
                Text(
                    "No servers yet. Add the one you set up — see the README for a free server guide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sky.TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profiles, key = { it.name }) { p ->
                        ServerRow(
                            profile = p,
                            isSelected = p.name == selected?.name,
                            isActive = p.name == activeName,
                            onClick = { onSelect(p) },
                            onDelete = { pendingDelete = p.name },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("ADD SERVER", style = MaterialTheme.typography.labelSmall, color = Sky.TextMuted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddTile(Icons.Rounded.QrCodeScanner, "Scan QR", onScanQr, Modifier.weight(1f))
                AddTile(Icons.Rounded.FileOpen, "Import file", onImportFile, Modifier.weight(1f))
                AddTile(Icons.Rounded.ContentPaste, "Paste", { showPaste = true }, Modifier.weight(1f))
            }
        }
    }

    if (showPaste) PasteDialog(onDismiss = { showPaste = false }, onSave = { n, t -> if (onPaste(n, t)) showPaste = false })

    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Sky.Sheet,
            title = { Text("Remove $name?") },
            text = { Text("The configuration will be deleted from this device.", color = Sky.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    profiles.firstOrNull { it.name == name }?.let(onDelete)
                    pendingDelete = null
                }) { Text("Remove", color = Sky.Danger) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = Sky.TextSecondary) } },
        )
    }
}

@Composable
private fun ServerRow(profile: Profile, isSelected: Boolean, isActive: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) Sky.Twilight else Sky.Glass)
            .border(1.dp, if (isSelected) Sky.Gold.copy(alpha = 0.5f) else Sky.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isActive) Sky.Gold else if (isSelected) Sky.Moon else Sky.TextMuted.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium, color = Sky.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (isActive) "Connected · ${profile.endpoint}" else profile.endpoint,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) Sky.Gold else Sky.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, "Remove", tint = Sky.TextMuted)
        }
    }
}

@Composable
private fun AddTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Sky.Glass)
            .border(1.dp, Sky.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Sky.Gold)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Sky.TextPrimary)
    }
}

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Sky.Gold,
        unfocusedBorderColor = Sky.GlassBorder,
        cursorColor = Sky.Gold,
        focusedLabelColor = Sky.Gold,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sky.Sheet,
        title = { Text("Paste configuration") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    text, { text = it },
                    label = { Text("[Interface] … [Peer] …") },
                    colors = colors,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 260.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(name.ifBlank { "server" }, text) }) {
                Text("Save", color = Sky.Gold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Sky.TextSecondary) } },
    )
}
