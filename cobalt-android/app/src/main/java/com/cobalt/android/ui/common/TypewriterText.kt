package com.cobalt.android.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Reveals [text] one character at a time, with a blinking caret while it
 * types and no caret once finished. Purely presentational: the caller owns
 * how long the splash stays up, this only decides when the last character
 * lands, so the splash duration is `text.length * msPerChar` plus whatever
 * hold the caller wants.
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    msPerChar: Long = 110L,
    style: TextStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    color: Color = Color.Unspecified,
    caretColor: Color = color,
    onFinished: (() -> Unit)? = null
) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        shown = 0
        while (shown < text.length) {
            delay(msPerChar)
            shown++
        }
        onFinished?.invoke()
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text.take(shown), style = style, color = color)
        if (shown < text.length) {
            Text(text = "|", style = style, color = caretColor)
        }
    }
}
