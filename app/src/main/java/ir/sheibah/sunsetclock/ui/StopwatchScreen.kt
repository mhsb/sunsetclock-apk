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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R

@Composable
fun StopwatchScreen(vm: MainViewModel) {
    val state by vm.stopwatch.collectAsStateWithLifecycle()
    // فقط برای بازترسیم هر ۲۵۰ میلی‌ثانیه خوانده می‌شود
    val tick by vm.now.collectAsStateWithLifecycle()
    val fa = isFarsi()

    // tick به‌عنوان کلید خوانده می‌شود تا هر ۲۵۰ میلی‌ثانیه بازترسیم شود
    val nowElapsed = remember(tick) { SystemClock.elapsedRealtime() }
    val elapsed = state.elapsed(nowElapsed)
    val hasValue = state.running || state.accumulated > 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tab_stopwatch),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(32.dp))

        Text(
            Fmt.stopwatch(elapsed, fa),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { if (state.running) vm.stopwatchLap() else vm.stopwatchReset() },
                enabled = hasValue,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (state.running) Icons.Filled.Flag else Icons.Filled.Replay,
                    contentDescription = stringResource(if (state.running) R.string.lap else R.string.reset)
                )
            }
            FilledIconButton(
                onClick = { vm.stopwatchToggle() },
                modifier = Modifier.size(84.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    if (state.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (state.running) R.string.pause else R.string.start),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            itemsIndexed(state.laps) { index, total ->
                val lapNumber = state.laps.size - index
                val previous = state.laps.getOrNull(index + 1) ?: 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
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
