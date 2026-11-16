@file:OptIn(ExperimentalLayoutApi::class)

package ir.sheibah.sunsetclock.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import java.time.ZoneId

@Composable
fun TimerScreen(vm: MainViewModel) {
    val timer by vm.timer.collectAsStateWithLifecycle()
    val nowMs by vm.now.collectAsStateWithLifecycle()
    val fa = isFarsi()

    var h by rememberSaveable { mutableIntStateOf(0) }
    var m by rememberSaveable { mutableIntStateOf(5) }
    var s by rememberSaveable { mutableIntStateOf(0) }

    val remaining = timer.remaining(nowMs)
    val finished = timer.isRunning && remaining <= 0L
    LaunchedEffect(finished) {
        if (finished) vm.syncTimer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tab_timer),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = timer.isIdle,
            transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.94f)) togetherWith (fadeOut(tween(150)) + scaleOut(targetScale = 0.94f)) },
            label = "timerMode"
        ) { idle ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (idle) {
                    TimeWheels(
                        values = listOf(h, m, s),
                        counts = listOf(24, 60, 60),
                        fa = fa,
                        onChange = { col, v ->
                            when (col) {
                                0 -> h = v
                                1 -> m = v
                                else -> s = v
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        itemHeight = 64.dp,
                        fontSize = 54
                    )

                    Spacer(Modifier.height(12.dp))

                    // پیش‌فرض‌ها (مثل ساعت شیائومی)
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(1, 5, 10, 15, 30, 60).forEach { minutes ->
                            val active = h * 60 + m == minutes && s == 0
                            SoftCard(
                                shape = RoundedCornerShape(20.dp),
                                brush = if (active) SunsetBrushSoft else null,
                                onClick = {
                                    h = minutes / 60
                                    m = minutes % 60
                                    s = 0
                                }
                            ) {
                                Text(
                                    stringResource(R.string.preset_min, Fmt.num(minutes, fa)),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    val totalMs = (h * 3600L + m * 60L + s) * 1000L
                    RoundActionButton(
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.start),
                        onClick = { vm.timerStart(totalMs) },
                        enabled = totalMs > 0L,
                        size = 92.dp
                    )
                } else {
                    val target = if (timer.totalMs > 0L) {
                        (remaining.toFloat() / timer.totalMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val fraction by animateFloatAsState(target, tween(400), label = "ring")
                    val track = MaterialTheme.colorScheme.surfaceContainerHighest

                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(290.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val stroke = 16.dp.toPx()
                            val d = size.minDimension - stroke
                            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                            drawArc(
                                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                topLeft = tl, size = Size(d, d), style = Stroke(stroke)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color(0xFFFFB347), Color(0xFFFF6F61), Color(0xFFFFB347))),
                                startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                                topLeft = tl, size = Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // بالا گرد می‌شود تا آخرین ثانیه صفر نشان داده نشود
                            MonoDigits(
                                Fmt.duration((remaining + 999L) / 1000L, fa),
                                fontSize = 54.sp,
                                weight = FontWeight.ExtraLight
                            )
                            Text(
                                if (timer.isRunning) stringResource(
                                    R.string.timer_ends_at, Fmt.hm(timer.endAt, ZoneId.systemDefault(), fa)
                                ) else stringResource(R.string.paused),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundActionButton(
                            icon = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            onClick = { vm.timerReset() },
                            size = 68.dp,
                            filled = false
                        )
                        RoundActionButton(
                            icon = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (timer.isRunning) R.string.pause else R.string.start),
                            onClick = { if (timer.isRunning) vm.timerPause() else vm.timerResume() },
                            size = 92.dp
                        )
                        FilledTonalButton(onClick = { vm.timerAddMinute() }) {
                            Text(stringResource(R.string.add_minute))
                        }
                    }
                }
            }
        }
    }
}
