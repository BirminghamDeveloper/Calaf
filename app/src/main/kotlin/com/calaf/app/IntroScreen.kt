package com.calaf.app

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.calaf.core.designsystem.CalafSpacing
import kotlinx.coroutines.delay

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val entrance = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        if (!reducedMotion) entrance.animateTo(1f, tween(700))
        delay(if (reducedMotion) 1200 else 1500)
        finish()
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(CalafSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val gold = MaterialTheme.colorScheme.secondary
            val emerald = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(112.dp).graphicsLayer {
                alpha = entrance.value
                scaleX = .8f + .2f * entrance.value
                scaleY = scaleX
                rotationZ = -15f * (1f - entrance.value)
            }) {
                drawCircle(emerald, radius = size.minDimension * .29f, center = Offset(size.width * .38f, size.height * .45f), style = Stroke(5.dp.toPx()))
                drawCircle(gold, radius = size.minDimension * .29f, center = Offset(size.width * .62f, size.height * .55f), style = Stroke(5.dp.toPx()))
            }
            Spacer(Modifier.height(CalafSpacing.large))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.intro_tagline), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(CalafSpacing.section))
            Text(stringResource(R.string.credit_idea), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.credit_developer), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(CalafSpacing.large))
            TextButton(onClick = onFinished) { Text(stringResource(R.string.continue_action)) }
        }
    }
}
