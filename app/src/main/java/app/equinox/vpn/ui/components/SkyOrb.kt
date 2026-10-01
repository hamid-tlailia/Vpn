package app.equinox.vpn.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.equinox.vpn.ui.theme.Sky
import kotlin.math.cos
import kotlin.math.sin

/**
 * The connect button. A moon at night ([day] = 0) that becomes a sun ([day] = 1).
 * While [busy], a halo orbits around it.
 */
@Composable
fun SkyOrb(day: Float, busy: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val transition = rememberInfiniteTransition(label = "orb")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "spin")
    val rays by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "rays")
    val breathe by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "breathe",
    )

    Canvas(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { contentDescription = label }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        val c = center
        val r = size.minDimension * 0.30f

        // Atmosphere glow
        val glowColor = lerp(Sky.Moon.copy(alpha = 0.22f), Sky.Gold.copy(alpha = 0.5f), day)
        val glowR = r * (1.75f + 0.12f * breathe)
        drawCircle(Brush.radialGradient(listOf(glowColor, Color.Transparent), c, glowR), glowR, c)

        // Sun rays
        if (day > 0.01f) {
            rotate(rays, c) {
                for (i in 0 until 16) {
                    val a = Math.toRadians(i * 22.5).toFloat()
                    val inner = r * 1.18f
                    val outer = r * (if (i % 2 == 0) 1.42f else 1.30f)
                    drawLine(
                        Sky.Gold.copy(alpha = 0.55f * day),
                        Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                        Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                        strokeWidth = r * 0.035f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Orbiting halo while connecting
        if (busy) {
            rotate(spin, c) {
                val haloR = r * 1.28f
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, Sky.Dawn.copy(alpha = 0.9f)), c),
                    startAngle = 0f, sweepAngle = 300f, useCenter = false,
                    topLeft = Offset(c.x - haloR, c.y - haloR), size = Size(haloR * 2, haloR * 2),
                    style = Stroke(width = r * 0.04f, cap = StrokeCap.Round),
                )
            }
        }

        // Body: silver moon → golden sun
        val light = lerp(Sky.Moon, Sky.Dawn, day)
        val deep = lerp(Sky.MoonShade, Sky.Ember, day)
        drawCircle(
            Brush.radialGradient(listOf(light, deep), Offset(c.x - r * 0.35f, c.y - r * 0.4f), r * 1.7f),
            r, c,
        )

        // Night details: craters + crescent shadow, fading out at sunrise
        val night = 1f - day
        if (night > 0.01f) {
            val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
            clipPath(body) {
                listOf(Triple(-0.38f, -0.22f, 0.16f), Triple(0.12f, 0.42f, 0.11f), Triple(-0.1f, 0.12f, 0.07f), Triple(0.40f, -0.38f, 0.08f))
                    .forEach { (dx, dy, rr) ->
                        drawCircle(Sky.MoonShade.copy(alpha = 0.35f * night), r * rr, Offset(c.x + dx * r, c.y + dy * r))
                    }
                drawCircle(
                    Sky.Midnight.copy(alpha = 0.42f * night),
                    r * 0.98f,
                    Offset(c.x + r * (0.82f + 0.6f * day), c.y - r * 0.14f),
                )
            }
        }

        // Power glyph
        val glyph = lerp(Sky.Indigo.copy(alpha = 0.85f), Color(0xFF7A3B12), day)
        val gr = r * 0.28f
        val stroke = Stroke(width = r * 0.075f, cap = StrokeCap.Round)
        drawArc(glyph, -60f, 300f, false, Offset(c.x - gr, c.y - gr + r * 0.03f), Size(gr * 2, gr * 2), style = stroke)
        drawLine(glyph, Offset(c.x, c.y - gr * 1.25f), Offset(c.x, c.y - gr * 0.1f), r * 0.075f, StrokeCap.Round)
    }
}
