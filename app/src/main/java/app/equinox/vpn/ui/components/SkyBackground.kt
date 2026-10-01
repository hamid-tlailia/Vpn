package app.equinox.vpn.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.equinox.vpn.ui.theme.Sky
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val size: Float, val phase: Float, val speed: Float)

/**
 * Full-screen sky. [day] goes 0 (night) → 1 (sunrise): the horizon warms up and the stars fade.
 */
@Composable
fun SkyBackground(day: Float, modifier: Modifier = Modifier) {
    val stars = remember {
        val rnd = Random(7)
        List(90) {
            Star(rnd.nextFloat(), rnd.nextFloat() * 0.75f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 2f * PI.toFloat(), 0.6f + rnd.nextFloat())
        }
    }
    val t by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )

    Canvas(modifier) {
        val top = lerp(Sky.Midnight, Sky.Indigo, day)
        val mid = lerp(Sky.Indigo, Sky.Twilight, day)
        val low = lerp(Sky.Twilight, Sky.Dusk, day)
        drawRect(Brush.verticalGradient(0f to top, 0.55f to mid, 1f to low))

        // Horizon glow — cool violet at night, ember at sunrise.
        val glow = lerp(Sky.Dusk.copy(alpha = 0.45f), Sky.Ember.copy(alpha = 0.75f), day)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(glow, glow.copy(alpha = 0f)),
                center = Offset(size.width / 2, size.height * 1.08f),
                radius = size.width * 1.1f,
            ),
            radius = size.width * 1.1f,
            center = Offset(size.width / 2, size.height * 1.08f),
        )

        val starAlpha = 1f - 0.8f * day
        stars.forEach { s ->
            val twinkle = 0.55f + 0.45f * sin(t * s.speed + s.phase)
            drawCircle(
                color = Color.White.copy(alpha = (twinkle * starAlpha * 0.85f).coerceIn(0f, 1f)),
                radius = s.size * density / 2f,
                center = Offset(s.x * size.width, s.y * size.height),
            )
        }
    }
}
