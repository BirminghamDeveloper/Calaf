package com.calaf.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small abstract pictures drawn in code, so there is nothing to download and no real person is shown. */
enum class IllustrationKind { Discover, Match, Chat, Privacy, Calm }

@Composable
fun Illustration(kind: IllustrationKind, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val main = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val accent = MaterialTheme.colorScheme.secondary
    Canvas(modifier.size(size)) {
        drawCircle(soft, radius = this.size.minDimension / 2)
        when (kind) {
            IllustrationKind.Discover -> drawCards(main, accent)
            IllustrationKind.Match -> drawRings(main, accent)
            IllustrationKind.Chat -> drawBubbles(main, accent)
            IllustrationKind.Privacy -> drawShield(main, accent)
            IllustrationKind.Calm -> drawMoon(main, accent)
        }
    }
}

private fun DrawScope.drawCards(main: Color, accent: Color) {
    val w = size.width
    drawRoundRect(accent, Offset(w * 0.30f, w * 0.24f), Size(w * 0.34f, w * 0.48f), CornerRadius(w * 0.05f))
    drawRoundRect(main, Offset(w * 0.38f, w * 0.30f), Size(w * 0.34f, w * 0.48f), CornerRadius(w * 0.05f))
    drawCircle(Color.White, w * 0.06f, Offset(w * 0.55f, w * 0.48f))
}

private fun DrawScope.drawRings(main: Color, accent: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.05f)
    drawCircle(main, w * 0.17f, Offset(w * 0.41f, w * 0.5f), style = stroke)
    drawCircle(accent, w * 0.17f, Offset(w * 0.59f, w * 0.5f), style = stroke)
}

private fun DrawScope.drawBubbles(main: Color, accent: Color) {
    val w = size.width
    drawRoundRect(main, Offset(w * 0.22f, w * 0.28f), Size(w * 0.40f, w * 0.20f), CornerRadius(w * 0.08f))
    drawRoundRect(accent, Offset(w * 0.38f, w * 0.54f), Size(w * 0.40f, w * 0.20f), CornerRadius(w * 0.08f))
}

private fun DrawScope.drawShield(main: Color, accent: Color) {
    val w = size.width
    drawRoundRect(main, Offset(w * 0.32f, w * 0.26f), Size(w * 0.36f, w * 0.44f), CornerRadius(w * 0.12f))
    drawCircle(accent, w * 0.07f, Offset(w * 0.5f, w * 0.44f))
    drawRoundRect(accent, Offset(w * 0.47f, w * 0.46f), Size(w * 0.06f, w * 0.12f), CornerRadius(w * 0.02f))
}

private fun DrawScope.drawMoon(main: Color, accent: Color) {
    val w = size.width
    drawCircle(main, w * 0.20f, Offset(w * 0.5f, w * 0.5f))
    drawCircle(accent, w * 0.03f, Offset(w * 0.72f, w * 0.30f))
    drawCircle(accent, w * 0.02f, Offset(w * 0.28f, w * 0.68f))
}
