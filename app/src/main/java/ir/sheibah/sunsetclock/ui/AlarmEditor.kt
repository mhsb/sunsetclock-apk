@file:OptIn(ExperimentalMaterial3Api::class)

package ir.sheibah.sunsetclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.astro.PrayerEvent
import ir.sheibah.sunsetclock.core.AlarmCalc
import ir.sheibah.sunsetclock.core.AlarmItem
import ir.sheibah.sunsetclock.core.AlarmKind
import ir.sheibah.sunsetclock.core.Place
import kotlin.math.abs

private const val MAX_OFFSET_MIN = 720

@Composable
fun AlarmEditorSheet(
    initial: AlarmItem,
    place: Place,
    method: CalcMethod,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fa = isFarsi()

    var kind by remember { mutableStateOf(initial.kind) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minuteOfHour by remember { mutableIntStateOf(initial.minute) }
    var anchor by remember { mutableStateOf(initial.anchor) }
    var before by remember { mutableStateOf(initial.offsetMin <= 0) }
    var minutes by remember { mutableIntStateOf(abs(initial.offsetMin).coerceAtMost(MAX_OFFSET_MIN)) }
    var days by remember { mutableIntStateOf(initial.daysMask) }
    var label by remember { mutableStateOf(initial.label) }

    val draft = AlarmItem(
        id = initial.id,
        label = label.trim(),
        enabled = true,
        kind = kind,
        hour = hour,
        minute = minuteOfHour,
        anchor = anchor,
        offsetMin = if (before) -minutes else minutes,
        daysMask = days
    )
    val nowMs = remember(draft) { System.currentTimeMillis() }
    val trigger = remember(draft) { AlarmCalc.nextTrigger(draft, nowMs, place, method) }

    val shortNames = stringArrayResource(R.array.weekdays_short)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(if (initial.id == 0) R.string.new_alarm else R.string.edit_alarm),
                style = MaterialTheme.typography.titleLarge
            )

            // نوع هشدار
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == AlarmKind.CLOCK,
                    onClick = { kind = AlarmKind.CLOCK },
                    label = { Text(stringResource(R.string.kind_clock)) }
                )
                FilterChip(
                    selected = kind == AlarmKind.PRAYER,
                    onClick = { kind = AlarmKind.PRAYER },
                    label = { Text(stringResource(R.string.kind_prayer)) }
                )
            }

            if (kind == AlarmKind.CLOCK) {
                TimeWheels(
                    values = listOf(hour, minuteOfHour),
                    counts = listOf(24, 60),
                    fa = fa,
                    onChange = { col, v -> if (col == 0) hour = v else minuteOfHour = v },
                    itemHeight = 60.dp,
                    fontSize = 50
                )
            } else {
                // جمله‌ی قابل‌خواندن با چرخنده‌ها:  «۳۰  دقیقه  قبل  از  اذان صبح»
                Text(stringResource(R.string.anchor_title), style = MaterialTheme.typography.labelLarge)

                val events = PrayerEvent.values()
                    .filter { it != PrayerEvent.ASR || method.showAsr || it == anchor }
                val beforeLabel = stringResource(R.string.before)
                val afterLabel = stringResource(R.string.after)
                val minShort = stringResource(R.string.unit_min_short)
                val ofText = stringResource(R.string.offset_of)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    WheelPicker(
                        count = MAX_OFFSET_MIN + 1,
                        selectedIndex = minutes,
                        onSelected = { minutes = it },
                        modifier = Modifier.width(62.dp),
                        itemHeight = 52.dp
                    ) { i, sel -> WheelLabel(Fmt.num(i, fa), sel, 30.sp) }

                    Text(minShort, style = MaterialTheme.typography.bodyMedium)

                    WheelPicker(
                        count = 2,
                        selectedIndex = if (before) 0 else 1,
                        onSelected = { before = it == 0 },
                        modifier = Modifier.width(70.dp),
                        itemHeight = 52.dp,
                        loop = false
                    ) { i, sel -> WheelLabel(if (i == 0) beforeLabel else afterLabel, sel, 22.sp) }

                    if (ofText.isNotEmpty()) {
                        Text(ofText, style = MaterialTheme.typography.bodyMedium)
                    }

                    WheelPicker(
                        count = events.size,
                        selectedIndex = events.indexOf(anchor).coerceAtLeast(0),
                        onSelected = { anchor = events[it] },
                        modifier = Modifier.weight(1f),
                        itemHeight = 52.dp,
                        loop = false
                    ) { i, sel -> WheelLabel(stringResource(events[i].labelRes()), sel, 22.sp) }
                }
            }

            // پیش‌نمایش زمان بعدی
            if (trigger != null) {
                Text(
                    stringResource(
                        R.string.next_ring_fmt,
                        Fmt.hm(trigger, place.zone, fa),
                        remainingText(trigger - nowMs, fa)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // روزهای تکرار
            Text(stringResource(R.string.repeat_days), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekOrder(fa).forEach { idx ->
                    val selected = days and (1 shl idx) != 0
                    DayDot(
                        text = shortNames[idx],
                        selected = selected,
                        onClick = { days = days xor (1 shl idx) }
                    )
                }
            }
            Text(
                stringResource(R.string.repeat_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.delete)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(draft) }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun DayDot(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}
