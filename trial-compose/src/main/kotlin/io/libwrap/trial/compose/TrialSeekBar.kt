package io.libwrap.trial.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.libwrap.trial.TrialEndpoint
import io.libwrap.trial.TrialRange
import io.libwrap.trial.TrialSeekBarStyle

/**
 * 展示试听区间的进度条。
 *
 * 与 View 版语义一致：进度取 `0..1` 比例，控件不持有媒体时长；区间内外可分别配色，
 * 拖动越界通过 [onSeekOutOfRange] 上报，是否钳制由 [clampToRange] 决定。
 *
 * 受控组件：进度由 [progress] 提供，拖动只上报目标值，不回写内部状态。
 *
 * @param style 为 null 时使用按当前密度换算的默认外观
 */
@Composable
public fun TrialSeekBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trialRange: TrialRange? = null,
    style: TrialSeekBarStyle? = null,
    enabled: Boolean = true,
    clampToRange: Boolean = true,
    onProgressChange: (Float) -> Unit = {},
    onProgressChangeFinished: (Float) -> Unit = {},
    onSeekOutOfRange: (Float) -> Unit = {},
) {
    val density = LocalDensity.current
    val resolvedStyle = style ?: defaultStyle(with(density) { 6.dp.toPx() })

    val submit: (Float, Boolean) -> Unit = { requested: Float, finished: Boolean ->
        if (trialRange != null && requested !in trialRange) {
            onSeekOutOfRange(requested)
        }
        val target = when {
            trialRange == null -> requested.coerceIn(0f, 1f)
            clampToRange -> requested.coerceIn(trialRange.start, trialRange.end)
            else -> requested.coerceIn(0f, 1f)
        }
        onProgressChange(target)
        if (finished) {
            onProgressChangeFinished(target)
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(with(density) { (resolvedStyle.trackHeightPx * 4f).toDp() })
            .progressSemantics(progress.coerceIn(0f, 1f))
            .pointerInput(enabled, trialRange, clampToRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    submit(offset.x / size.width.toFloat(), true)
                }
            }
            .pointerInput(enabled, trialRange, clampToRange) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragEnd = { onProgressChangeFinished(progress.coerceIn(0f, 1f)) },
                    onDrag = { change, _ ->
                        change.consume()
                        submit(change.position.x / size.width.toFloat(), false)
                    },
                )
            },
    ) {
        drawTrack(progress.coerceIn(0f, 1f), trialRange, resolvedStyle)
    }
}

private fun defaultStyle(trackHeightPx: Float): TrialSeekBarStyle = TrialSeekBarStyle(
    trackColor = 0x33FFFFFF,
    progressColor = 0xFFFFFFFF.toInt(),
    trialTrackColor = 0x33FFFFFF,
    trialProgressColor = 0xFFFFC107.toInt(),
    trackHeightPx = trackHeightPx,
)

private fun DrawScope.drawTrack(
    progress: Float,
    trialRange: TrialRange?,
    style: TrialSeekBarStyle,
) {
    val trackHeight = style.trackHeightPx
    val top = (size.height - trackHeight) / 2f
    val corner = CornerRadius(style.cornerRadiusPx ?: (trackHeight / 2f))

    drawRoundRect(
        color = androidx.compose.ui.graphics.Color(style.trackColor),
        topLeft = Offset(0f, top),
        size = Size(size.width, trackHeight),
        cornerRadius = corner,
    )

    if (trialRange != null) {
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(style.trialTrackColor),
            topLeft = Offset(trialRange.start * size.width, top),
            size = Size(trialRange.length * size.width, trackHeight),
            cornerRadius = corner,
        )
    }

    val fillStart = trialRange?.start ?: 0f
    if (progress > fillStart) {
        val fillWidth = (progress - fillStart) * size.width
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = top,
                    right = size.width,
                    bottom = top + trackHeight,
                    cornerRadius = corner,
                ),
            )
        }
        clipPath(path) {
            drawRect(
                color = androidx.compose.ui.graphics.Color(
                    if (trialRange != null) style.trialProgressColor else style.progressColor,
                ),
                topLeft = Offset(fillStart * size.width, top),
                size = Size(fillWidth, trackHeight),
            )
        }
    }

    if (trialRange != null) {
        drawEndpoint(style.startEndpoint, trialRange.start * size.width, size.height / 2f, trackHeight)
        drawEndpoint(style.endEndpoint, trialRange.end * size.width, size.height / 2f, trackHeight)
    }
}

private fun DrawScope.drawEndpoint(
    endpoint: TrialEndpoint,
    x: Float,
    centerY: Float,
    trackHeight: Float,
) {
    when (endpoint) {
        TrialEndpoint.None -> Unit
        is TrialEndpoint.Line -> drawRect(
            color = androidx.compose.ui.graphics.Color(endpoint.color),
            topLeft = Offset(x - endpoint.widthPx / 2f, centerY - trackHeight / 2f),
            size = Size(endpoint.widthPx, trackHeight),
        )

        is TrialEndpoint.Circle -> drawCircle(
            color = androidx.compose.ui.graphics.Color(endpoint.color),
            radius = endpoint.radiusPx,
            center = Offset(x, centerY),
        )

        is TrialEndpoint.Custom -> drawIntoCanvas { canvas ->
            endpoint.draw.draw(canvas.nativeCanvas, x, centerY, trackHeight)
        }
    }
}
