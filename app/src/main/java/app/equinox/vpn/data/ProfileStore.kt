package app.equinox.vpn.data

import android.content.Context
import com.wireguard.config.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.StringReader

/** A saved WireGuard server, stored as a plain wg-quick .conf file in private app storage. */
data class Profile(val name: String, val config: Config) {
    val endpoint: String
        get() = config.peers.firstOrNull()?.endpoint?.orElse(null)?.host ?: "Unknown endpoint"
}

class ProfileStore(context: Context) {
    private val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("equinox", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow(load())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _selected = MutableStateFlow(prefs.getString(KEY_SELECTED, null))
    val selected: StateFlow<String?> = _selected.asStateFlow()

    /** Parses [text] as a WireGuard config and saves it. Returns the saved profile. */
    fun add(rawName: String, text: String): Profile {
        val config = parse(text)
        val name = uniqueName(sanitize(rawName))
        File(dir, "$name.conf").writeText(config.toWgQuickString())
        val profile = Profile(name, config)
        _profiles.value = (_profiles.value + profile).sortedBy { it.name.lowercase() }
        if (_selected.value == null || _profiles.value.size == 1) select(name)
        return profile
    }

    fun delete(name: String) {
        File(dir, "$name.conf").delete()
        _profiles.value = _profiles.value.filterNot { it.name == name }
        if (_selected.value == name) select(_profiles.value.firstOrNull()?.name)
    }

    fun select(name: String?) {
        _selected.value = name
        prefs.edit().putString(KEY_SELECTED, name).apply()
    }

    private fun load(): List<Profile> =
        dir.listFiles { f -> f.extension == "conf" }.orEmpty()
            .mapNotNull { f -> runCatching { Profile(f.nameWithoutExtension, parse(f.readText())) }.getOrNull() }
            .sortedBy { it.name.lowercase() }

    private fun uniqueName(base: String): String {
        val taken = _profiles.value.map { it.name }.toSet()
        if (base !in taken) return base
        var i = 2
        while (true) {
            val suffix = "-$i"
            val candidate = base.take(MAX_NAME - suffix.length) + suffix
            if (candidate !in taken) return candidate
            i++
        }
    }

    companion object {
        private const val KEY_SELECTED = "selected"
        // WireGuard interface names: ^[a-zA-Z0-9_=+.-]{1,15}$
        private const val MAX_NAME = 15

        fun parse(text: String): Config =
            Config.parse(BufferedReader(StringReader(text.trim())))

        fun sanitize(raw: String): String {
            val cleaned = raw.substringBeforeLast(".conf")
                .replace(Regex("[^a-zA-Z0-9_=+.-]"), "-")
                .trim('-', '.')
                .take(MAX_NAME)
            return cleaned.ifEmpty { "server" }
        }
    }
}
