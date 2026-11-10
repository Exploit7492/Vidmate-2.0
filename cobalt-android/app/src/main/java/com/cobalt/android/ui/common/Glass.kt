package com.cobalt.android.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cobalt.android.ui.theme.CobaltTheme

/**
 * Glassmorphism surface treatment: a translucent fill, a hairline border and
 * a diagonal sheen. Compose can't sample and blur what is drawn behind a
 * composable without a manual backdrop capture, so this reproduces the look
 * with translucency and light rather than a real backdrop blur — it reads as
 * frosted glass over the app's own background and costs nothing.
 */
@Composable
fun Modifier.cobaltGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    strong: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier {
    val glass = CobaltTheme.glass
    val fill = if (strong) glass.fillStrong else glass.fill
    return this
        .clip(shape)
        .background(fill)
        .background(
            Brush.linearGradient(
                colors = listOf(glass.sheen, androidx.compose.ui.graphics.Color.Transparent),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        )
        .border(BorderStroke(borderWidth, glass.border), shape)
}
