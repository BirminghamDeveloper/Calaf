package com.calaf.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** How many cards are drawn at once. The rest wait in the list, so a long list costs nothing. */
const val SWIPE_STACK_VISIBLE_CARDS = 3

internal data class SwipeRequest(val id: Int, val direction: SwipeDirection)

internal data class UndoRequest(val key: Any, val from: SwipeDirection)

/**
 * Lets buttons and TalkBack do exactly what a drag does ([swipe]), and gives an undo animation hook ([animateUndo]).
 */
class SwipeStackController {
    private var counter = 0
    internal var request by mutableStateOf<SwipeRequest?>(null)
    internal var undo by mutableStateOf<UndoRequest?>(null)

    /** Sends the top card, with the same animation and callback as a drag. */
    fun swipe(direction: SwipeDirection) {
        request = SwipeRequest(++counter, direction)
    }

    /**
     * The undo animation hook. Put the card with [itemKey] back at the front of the list, then call this: it flies
     * back in from the side it left. The stack does not remember swiped cards, so what can be undone is up to the
     * caller.
     */
    fun animateUndo(itemKey: Any, from: SwipeDirection) {
        undo = UndoRequest(itemKey, from)
    }
}

/**
 * A stack of swipeable cards. The first item is on top. Dragging sends the card (see [SwipePhysics]), the cards
 * behind it peek out, and at most [SWIPE_STACK_VISIBLE_CARDS] are drawn. [onSwiped] reports a sent card and the caller
 * then removes it from [items]. [enabled] false stops drags and buttons, for example while a request is running.
 *
 * The top card also offers LIKE and PASS as TalkBack actions, labelled [likeLabel] and [passLabel].
 */
@Composable
fun <T : Any> SwipeCardStack(
    items: List<T>,
    itemKey: (T) -> Any,
    onSwiped: (T, SwipeDirection) -> Unit,
    controller: SwipeStackController,
    likeLabel: String,
    passLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    card: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val visible = items.take(SWIPE_STACK_VISIBLE_CARDS)
        // Draw from the back to the front so the top card is last and sits above the others.
        visible.asReversed().forEachIndexed { reversed, item ->
            val depth = visible.size - 1 - reversed
            key(itemKey(item)) {
                if (depth == 0) {
                    TopCard(item, itemKey(item), widthPx, controller, enabled, likeLabel, passLabel, onSwiped, card)
                } else {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            val scale = 1f - DEPTH_SCALE * depth
                            scaleX = scale
                            scaleY = scale
                            translationY = depth * DEPTH_SHIFT_DP.dp.toPx()
                        }.clearAndSetSemantics {},
                    ) { card(item) }
                }
            }
        }
    }
}

@Composable
private fun <T : Any> TopCard(
    item: T,
    itemKey: Any,
    widthPx: Float,
    controller: SwipeStackController,
    enabled: Boolean,
    likeLabel: String,
    passLabel: String,
    onSwiped: (T, SwipeDirection) -> Unit,
    card: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var sent by remember { mutableStateOf(false) }

    suspend fun send(direction: SwipeDirection) {
        if (sent) return
        sent = true
        offsetX.animateTo(SwipePhysics.exitOffset(direction, widthPx), tween(SEND_MILLIS))
        onSwiped(item, direction)
    }

    LaunchedEffect(controller.request, enabled) {
        val request = controller.request ?: return@LaunchedEffect
        if (!enabled || sent) return@LaunchedEffect
        controller.request = null
        send(request.direction)
    }
    LaunchedEffect(controller.undo) {
        val undo = controller.undo ?: return@LaunchedEffect
        if (undo.key != itemKey) return@LaunchedEffect
        controller.undo = null
        sent = false
        offsetX.snapTo(SwipePhysics.exitOffset(undo.from, widthPx))
        offsetX.animateTo(0f, spring(Spring.DampingRatioLowBouncy))
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY * DRAG_Y_FOLLOW
                rotationZ = SwipePhysics.rotation(offsetX.value, widthPx)
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(likeLabel) {
                        controller.swipe(SwipeDirection.LIKE)
                        true
                    },
                    CustomAccessibilityAction(passLabel) {
                        controller.swipe(SwipeDirection.PASS)
                        true
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val tracker = VelocityTracker()
                var travelled = 0f
                detectDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                        travelled = offsetX.value
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        travelled += drag.x
                        offsetY += drag.y
                        tracker.addPosition(change.uptimeMillis, Offset(travelled, 0f))
                        scope.launch { offsetX.snapTo(travelled) }
                    },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity().x
                        offsetY = 0f
                        scope.launch {
                            val direction = SwipePhysics.decide(offsetX.value, velocity, widthPx)
                            if (direction != null) send(direction) else offsetX.animateTo(0f, spring())
                        }
                    },
                    onDragCancel = {
                        offsetY = 0f
                        scope.launch { offsetX.animateTo(0f, spring()) }
                    },
                )
            },
    ) {
        card(item)
        val stamp = SwipePhysics.stampDirection(offsetX.value)
        if (stamp != null) {
            Text(
                text = (if (stamp == SwipeDirection.LIKE) likeLabel else passLabel).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (stamp == SwipeDirection.LIKE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.align(if (stamp == SwipeDirection.LIKE) Alignment.TopStart else Alignment.TopEnd)
                    .padding(24.dp)
                    .graphicsLayer { alpha = SwipePhysics.stampAlpha(offsetX.value, widthPx) },
            )
        }
    }
}

private const val DEPTH_SCALE = 0.05f
private const val DEPTH_SHIFT_DP = 14
private const val DRAG_Y_FOLLOW = 0.3f
private const val SEND_MILLIS = 220
