package ir.sheibah.sunsetclock.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import java.util.Locale

@Composable
fun StopwatchScreen(vm: MainViewModel) {
    val state by vm.stopwatch.collectAsStateWithLifecycle()
    val fa = isFarsi()

    // به‌روزرسانی هم‌گام با فریم‌های صفحه (روان، بدون پرش)
    var frameTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.running) {
        frameTime = SystemClock.elapsedRealtime()
        while (state.running) {
            androidx.compose.runtime.withFrameNanos { frameTime = SystemClock.elapsedRealtime() }
        }
    }
    val elapsed = state.elapsed(frameTime)
    val hasValue = state.running || state.accumulated > 0L

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tab_stopwatch),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(36.dp))

        // mm:ss بزرگ + صدم ثانیه‌ی کوچک‌تر؛ همه با سلول‌های هم‌عرض
        val t = elapsed.coerceAtLeast(0L)
        val main = Fmt.digits(String.format(Locale.ENGLISH, "%02d:%02d", t / 60_000L, t % 60_000L / 1000L), fa)
        val centi = Fmt.digits(String.format(Locale.ENGLISH, ".%02d", t % 1000L / 10L), fa)
        Row(verticalAlignment = Alignment.Bottom) {
            MonoDigits(main, 72.sp, weight = FontWeight.ExtraLight)
            MonoDigits(
                centi, 36.sp, weight = FontWeight.ExtraLight,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundActionButton(
                icon = if (state.running) Icons.Filled.Flag else Icons.Filled.Replay,
                contentDescription = stringResource(if (state.running) R.string.lap else R.string.reset),
                onClick = { if (state.running) vm.stopwatchLap() else vm.stopwatchReset() },
                size = 68.dp,
                filled = false,
                enabled = hasValue
            )
            RoundActionButton(
                icon = if (state.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (state.running) R.string.pause else R.string.start),
                onClick = { vm.stopwatchToggle() },
                size = 92.dp
            )
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            itemsIndexed(state.laps, key = { i, _ -> state.laps.size - i }) { index, total ->
                val lapNumber = state.laps.size - index
                val previous = state.laps.getOrNull(index + 1) ?: 0L
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.lap_fmt, Fmt.num(lapNumber, fa)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(Fmt.stopwatch(total - previous, fa), fontWeight = FontWeight.Medium)
                    Text(Fmt.stopwatch(total, fa), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
