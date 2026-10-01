package app.equinox.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** One theme: night sky that warms into sunrise once you're protected. */
object Sky {
    val Midnight = Color(0xFF060A1C)
    val Indigo = Color(0xFF10163F)
    val Twilight = Color(0xFF2B1F5C)
    val Dusk = Color(0xFF5B3A8A)
    val Ember = Color(0xFFF29E5C)
    val Gold = Color(0xFFFFC978)
    val Dawn = Color(0xFFFFE7C2)
    val Moon = Color(0xFFDCE3FF)
    val MoonShade = Color(0xFF8E97C8)

    val TextPrimary = Color(0xFFF7F4FF)
    val TextSecondary = Color(0xFFB9B6D9)
    val TextMuted = Color(0xFF7E7BA6)
    val Glass = Color(0x14FFFFFF)
    val GlassStrong = Color(0x22FFFFFF)
    val GlassBorder = Color(0x24FFFFFF)
    val Danger = Color(0xFFFF8A9A)
    val Sheet = Color(0xFF12153A)
}

private val scheme = darkColorScheme(
    primary = Sky.Gold,
    onPrimary = Sky.Midnight,
    secondary = Sky.Moon,
    onSecondary = Sky.Midnight,
    background = Sky.Midnight,
    onBackground = Sky.TextPrimary,
    surface = Sky.Sheet,
    onSurface = Sky.TextPrimary,
    surfaceVariant = Sky.Twilight,
    onSurfaceVariant = Sky.TextSecondary,
    surfaceContainerHigh = Sky.Sheet,
    surfaceContainerLow = Sky.Sheet,
    surfaceContainer = Sky.Sheet,
    outline = Sky.GlassBorder,
    error = Sky.Danger,
)

private val base = FontFamily.SansSerif

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.22.em),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.08.em),
)

@Composable
fun EquinoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
