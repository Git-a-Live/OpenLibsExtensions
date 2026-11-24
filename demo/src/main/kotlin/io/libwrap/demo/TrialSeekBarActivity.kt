package io.libwrap.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.libwrap.trial.TrialEndpoint
import io.libwrap.trial.TrialRange
import io.libwrap.trial.TrialSeekBarStyle
import io.libwrap.trial.compose.TrialSeekBar
import kotlinx.coroutines.delay

class TrialSeekBarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TrialDemo()
                }
            }
        }
    }
}

private const val DURATION_MS = 180_000f
private const val RANGE_START_MS = 30_000f
private const val RANGE_END_MS = 60_000f
private const val TICK_MS = 200f

@Composable
private fun TrialDemo() {
    val density = LocalDensity.current
    val range = remember { TrialRange.ofMillis(30_000, 60_000, 180_000)!! }

    var progressMs by remember { mutableFloatStateOf(RANGE_START_MS) }
    var playing by remember { mutableStateOf(false) }
    var endpointMode by remember { mutableIntStateOf(0) }
    var warmPalette by remember { mutableStateOf(true) }
    var hint by remember { mutableStateOf("拖动超出区间时，仅上报回调，不触发任何业务动作。") }

    LaunchedEffect(playing) {
        while (playing) {
            delay(TICK_MS.toLong())
            val next = progressMs + TICK_MS
            if (next >= RANGE_END_MS) {
                progressMs = RANGE_END_MS
                playing = false
            } else {
                progressMs = next
            }
        }
    }

    val markerColor = 0xFFFFFFFF.toInt()
    val endpoint = when (endpointMode) {
        0 -> TrialEndpoint.Circle(color = markerColor, radiusPx = with(density) { 5.dp.toPx() })
        1 -> TrialEndpoint.Line(color = markerColor, widthPx = with(density) { 3.dp.toPx() })
        else -> TrialEndpoint.None
    }

    val style = TrialSeekBarStyle(
        trackColor = 0x33000000,
        progressColor = 0xFF455A64.toInt(),
        trialTrackColor = 0x1F000000,
        trialProgressColor = if (warmPalette) 0xFFF57C00.toInt() else 0xFF00897B.toInt(),
        trackHeightPx = with(density) { 8.dp.toPx() },
        startEndpoint = endpoint,
        endEndpoint = endpoint,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "试听区间进度条", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "当前 ${seconds(progressMs)}　试听 ${seconds(RANGE_START_MS)} ~ ${seconds(RANGE_END_MS)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        TrialSeekBar(
            progress = progressMs / DURATION_MS,
            trialRange = range,
            style = style,
            onProgressChange = {
                progressMs = it * DURATION_MS
                playing = false
                hint = "拖动到 ${seconds(progressMs)}"
            },
            onProgressChangeFinished = {
                hint = "松手位置 ${seconds(it * DURATION_MS)}"
            },
            onSeekOutOfRange = {
                hint = "超出试听区间：请求 ${seconds(it * DURATION_MS)}，已按 clampToRange 处理"
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    progressMs = RANGE_START_MS
                    playing = !playing
                },
            ) {
                Text(if (playing) "暂停" else "播放")
            }
            OutlinedButton(onClick = { endpointMode = (endpointMode + 1) % 3 }) {
                Text(
                    when (endpointMode) {
                        0 -> "端点：圆点"
                        1 -> "端点：短线"
                        else -> "端点：无"
                    },
                )
            }
            OutlinedButton(onClick = { warmPalette = !warmPalette }) {
                Text("换色")
            }
        }

        Text(text = hint, style = MaterialTheme.typography.bodySmall)
    }
}

private fun seconds(millis: Float): String = "${(millis / 1000f * 10).toInt() / 10f}s"
