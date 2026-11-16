@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package ir.sheibah.sunsetclock.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import java.time.ZoneId
import java.util.Locale

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(24.dp))

        if (timer.isIdle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeSpinner(h, 23, stringResource(R.string.unit_hours), fa) { h = it }
                Text(":", fontSize = 44.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TimeSpinner(m, 59, stringResource(R.string.unit_minutes), fa) { m = it }
                Text(":", fontSize = 44.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TimeSpinner(s, 59, stringResource(R.string.unit_seconds), fa) { s = it }
            }

            Spacer(Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10, 15, 30).forEach { minutes ->
                    AssistChip(
                        onClick = {
                            h = 0
                            m = minutes
                            s = 0
                        },
                        label = { Text(stringResource(R.string.preset_min, Fmt.num(minutes, fa))) }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            val totalMs = (h * 3600L + m * 60L + s) * 1000L
            FilledIconButton(
                onClick = { vm.timerStart(totalMs) },
                enabled = totalMs > 0L,
                modifier = Modifier.size(84.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.start),
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            val fraction = if (timer.totalMs > 0L) {
                (remaining.toFloat() / timer.totalMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(260.dp),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // بالا گرد می‌شود تا آخرین ثانیه صفر نشان داده نشود
                    Text(
                        Fmt.duration((remaining + 999L) / 1000L, fa),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light
                    )
                    if (timer.isRunning) {
                        Text(
                            stringResource(
                                R.string.timer_ends_at,
                                Fmt.hm(timer.endAt, ZoneId.systemDefault(), fa)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            stringResource(R.string.paused),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { vm.timerReset() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
                FilledIconButton(
                    onClick = { if (timer.isRunning) vm.timerPause() else vm.timerResume() },
                    modifier = Modifier.size(84.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (timer.isRunning) R.string.pause else R.string.start),
                        modifier = Modifier.size(40.dp)
                    )
                }
                FilledTonalButton(onClick = { vm.timerAddMinute() }) {
                    Text(stringResource(R.string.add_minute))
                }
            }
        }
    }
}

@Composable
private fun TimeSpinner(value: Int, max: Int, label: String, fa: Boolean, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= max) 0 else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
        }
        Text(
            Fmt.digits(String.format(Locale.ENGLISH, "%02d", value), fa),
            fontSize = 52.sp,
            fontWeight = FontWeight.Light
        )
        IconButton(onClick = { onChange(if (value <= 0) max else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
