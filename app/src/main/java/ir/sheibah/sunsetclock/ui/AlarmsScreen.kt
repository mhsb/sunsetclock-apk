package ir.sheibah.sunsetclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.core.AlarmCalc
import ir.sheibah.sunsetclock.core.AlarmItem
import ir.sheibah.sunsetclock.core.AlarmKind
import java.time.ZoneId

@Composable
fun AlarmsScreen(vm: MainViewModel) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val nowMs by vm.now.collectAsStateWithLifecycle()
    val fa = isFarsi()
    val place = settings.place()
    val method = settings.method
    val minute = nowMs / 60_000L

    var editing by remember { mutableStateOf<AlarmItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.tab_alarms),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            )

            if (alarms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.no_alarms),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alarms, key = { it.id }) { item ->
                        // زمان بعدی هر دقیقه یک‌بار دوباره حساب می‌شود
                        val trigger = remember(item, minute, place, method) {
                            if (item.enabled) AlarmCalc.nextTrigger(item, minute * 60_000L, place, method) else null
                        }
                        AlarmCard(
                            item = item,
                            trigger = trigger,
                            nowMs = minute * 60_000L,
                            zone = place.zone,
                            fa = fa,
                            onClick = { editing = item },
                            onToggle = { vm.setAlarmEnabled(item, it) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = AlarmItem(id = 0) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_alarm))
        }
    }

    val current = editing
    if (current != null) {
        AlarmEditorSheet(
            initial = current,
            place = place,
            method = method,
            onDismiss = { editing = null },
            onSave = {
                vm.saveAlarm(it)
                editing = null
            },
            onDelete = if (current.id != 0) {
                {
                    vm.deleteAlarm(current.id)
                    editing = null
                }
            } else null
        )
    }
}

@Composable
private fun AlarmCard(
    item: AlarmItem,
    trigger: Long?,
    nowMs: Long,
    zone: ZoneId,
    fa: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val big = when (item.kind) {
        AlarmKind.CLOCK -> Fmt.hm(item.hour, item.minute, fa)
        AlarmKind.PRAYER -> if (trigger != null) Fmt.hm(trigger, zone, fa) else "--:--"
    }

    val kindLine = when (item.kind) {
        AlarmKind.CLOCK -> item.label
        AlarmKind.PRAYER -> {
            val ev = stringResource(item.anchor.labelRes())
            val m = Fmt.num(kotlin.math.abs(item.offsetMin), fa)
            when {
                item.offsetMin == 0 -> stringResource(R.string.offset_same, ev)
                item.offsetMin < 0 -> stringResource(R.string.offset_before, m, ev)
                else -> stringResource(R.string.offset_after, m, ev)
            }
        }
    }
    val extraLabel = if (item.kind == AlarmKind.PRAYER) item.label else ""

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    big,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light,
                    color = if (item.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (kindLine.isNotBlank()) {
                    Text(kindLine, style = MaterialTheme.typography.titleSmall)
                }
                if (extraLabel.isNotBlank()) {
                    Text(
                        extraLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    daysSummary(item.daysMask, fa),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.enabled && trigger != null) {
                    Text(
                        remainingText(trigger - nowMs, fa),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(checked = item.enabled, onCheckedChange = onToggle)
        }
    }
}
