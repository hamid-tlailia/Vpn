package app.equinox.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.equinox.vpn.ui.theme.Sky

/** Frosted-glass surface used for every card in the app. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Sky.GlassStrong, Sky.Glass)))
            .border(1.dp, Sky.GlassBorder, shape),
        content = content,
    )
}
