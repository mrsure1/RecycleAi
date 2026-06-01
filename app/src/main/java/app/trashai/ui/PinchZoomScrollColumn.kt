package app.trashai.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val ZOOM_THRESHOLD = 1.01f
private const val ZOOM_SENSITIVITY = 1.4f

/**
 * Bottom sheet: parent catches two-finger transform; child keeps one-finger scroll at 1x.
 */
@Composable
fun PinchZoomScrollColumn(
    modifier: Modifier = Modifier,
    maxScale: Float = 3f,
    resetKey: Any? = null,
    onScrollState: ((ScrollState) -> Unit)? = null,
    onZoomedChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var contentWidthPx by remember { mutableFloatStateOf(0f) }
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrollState) {
        onScrollState?.invoke(scrollState)
    }

    LaunchedEffect(resetKey) {
        scale = 1f
        offset = Offset.Zero
        scrollState.scrollTo(0)
    }

    val zoomed = scale > ZOOM_THRESHOLD

    LaunchedEffect(zoomed) {
        onZoomedChange?.invoke(zoomed)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        fun clampOffset(raw: Offset, forScale: Float): Offset {
            val scaledW = contentWidthPx * forScale
            val scaledH = contentHeightPx * forScale
            val minX = if (scaledW > viewportWidthPx) -(scaledW - viewportWidthPx) else 0f
            val minY = if (scaledH > viewportHeightPx) -(scaledH - viewportHeightPx) else 0f
            return Offset(
                x = raw.x.coerceIn(minX, 0f),
                y = raw.y.coerceIn(minY, 0f),
            )
        }

        fun adjustedZoom(zoom: Float): Float = when {
            zoom == 1f -> 1f
            zoom > 1f -> 1f + (zoom - 1f) * ZOOM_SENSITIVITY
            else -> 1f - (1f - zoom) * ZOOM_SENSITIVITY
        }

        fun applyScaleAndOffset(newScaleRaw: Float, newOffsetRaw: Offset) {
            val newScale = newScaleRaw.coerceIn(1f, maxScale)
            if (newScale <= ZOOM_THRESHOLD) {
                val restoreScroll = (-newOffsetRaw.y).toInt().coerceIn(0, scrollState.maxValue)
                scale = 1f
                offset = Offset.Zero
                scope.launch { scrollState.scrollTo(restoreScroll) }
            } else {
                scale = newScale
                offset = clampOffset(newOffsetRaw, newScale)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxScale, viewportWidthPx, viewportHeightPx) {
                    detectTransformGestures(
                        panZoomLock = false,
                        onGesture = { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (oldScale * adjustedZoom(zoom)).coerceIn(1f, maxScale)

                            if (abs(newScale - oldScale) <= 0.0001f && oldScale <= ZOOM_THRESHOLD) {
                                return@detectTransformGestures
                            }

                            var newOffset = offset
                            if (oldScale <= ZOOM_THRESHOLD && newScale > ZOOM_THRESHOLD && scrollState.value > 0) {
                                newOffset = Offset(newOffset.x, -scrollState.value.toFloat())
                                scope.launch { scrollState.scrollTo(0) }
                            }

                            if (oldScale > 0f) {
                                newOffset = centroid - (centroid - newOffset) * (newScale / oldScale)
                            }

                            if (oldScale > ZOOM_THRESHOLD || newScale > ZOOM_THRESHOLD) {
                                newOffset += pan
                            }

                            applyScaleAndOffset(newScale, newOffset)
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, enabled = !zoomed)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        contentWidthPx = placeable.width.toFloat()
                        contentHeightPx = placeable.height.toFloat()
                        val layoutHeight = (placeable.height * scale).toInt()
                            .coerceAtLeast(placeable.height)
                        layout(placeable.width, layoutHeight) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = false,
                    ),
                content = content,
            )
        }
    }
}
