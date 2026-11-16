package ir.sheibah.sunsetclock.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.Calendars
import ir.sheibah.sunsetclock.core.Cities
import ir.sheibah.sunsetclock.core.ClockCalc
import ir.sheibah.sunsetclock.core.ClockSnapshot
import java.time.Instant

@Composable
fun ClockScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val nowMs by vm.now.collectAsStateWithLifecycle()
    val fa = isFarsi()

    val place = settings.place()
    val method = settings.method
    val nowSec = nowMs / 1000L
    val snap = remember(nowSec, place, method) { ClockCalc.snapshot(nowSec * 1000L, place, method) }
    val zone = place.zone

    val cityName = if (settings.useGps && settings.hasGpsFix) {
        stringResource(R.string.approx_location)
    } else {
        Cities.byId(settings.cityId).let { if (fa) it.fa else it.en }
    }

    val weekdays = stringArrayResource(R.array.weekdays)
    val jalaliMonths = stringArrayResource(R.array.jalali_months)
    val hijriMonths = stringArrayResource(R.array.hijri_months)

    val weekdayName = weekdays[snap.islamicDate.dayOfWeek.value - 1]
    val headline = stringResource(if (snap.isNight) R.string.night_of else R.string.day_of, weekdayName)
    val j = Calendars.toJalali(snap.islamicDate)
    val jalaliText = Fmt.digits("${j.day} ${jalaliMonths[j.month - 1]} ${j.year}", fa)
    val hijriText = Calendars.toHijri(snap.islamicDate, settings.hijriAdjust)?.let {
        Fmt.digits("${it.day} ${hijriMonths[it.month - 1]} ${it.year}", fa)
    }

    val localSeconds = Instant.ofEpochMilli(nowSec * 1000L).atZone(zone).toLocalTime().toSecondOfDay().toLong()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // مکان
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(cityName, style = MaterialTheme.typography.titleMedium)
        }

        // صفحه‌ی ساعت
        SunsetDial(
            snap = snap,
            fa = fa,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.sunset_clock),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    Fmt.clock(snap.sunsetSeconds.toLong(), fa),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.local_time_fmt, Fmt.clock(localSeconds, fa)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // تاریخ
        InfoCard {
            Text(headline, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(jalaliText, style = MaterialTheme.typography.titleMedium)
            if (hijriText != null) {
                Text(
                    hijriText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // نوارهای شب و روز
        NightDayCard(snap, fa)

        // اذان بعدی
        val next = snap.next
        if (next != null) {
            InfoCard {
                Text(
                    stringResource(R.string.next_event),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(next.event.labelRes()), style = MaterialTheme.typography.titleLarge)
                    Text(
                        Fmt.hm(next.time, zone, fa),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    stringResource(R.string.countdown_fmt, Fmt.clock((next.time - nowMs) / 1000L, fa)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // اوقات امروز
        InfoCard {
            Text(
                stringResource(R.string.todays_times),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            snap.todayEvents.forEach { ev ->
                val isNext = next != null && next.event == ev.event && next.time == ev.time
                val past = ev.time <= nowMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNext) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .alpha(if (past && !isNext) 0.5f else 1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(ev.event.labelRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        Fmt.hm(ev.time, zone, fa),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun NightDayCard(snap: ClockSnapshot, fa: Boolean) {
    val now = snap.now
    InfoCard {
        BarRow(
            label = stringResource(R.string.night_progress),
            progress = snap.nightProgress.toFloat(),
            detail = if (snap.isNight) {
                stringResource(
                    R.string.bar_detail,
                    Fmt.hoursMinutes(now - snap.lastSunset, fa),
                    Fmt.hoursMinutes(snap.nextSunrise - now, fa)
                )
            } else stringResource(R.string.bar_done),
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(14.dp))
        BarRow(
            label = stringResource(R.string.day_progress),
            progress = snap.dayProgress.toFloat(),
            detail = if (!snap.isNight) {
                stringResource(
                    R.string.bar_detail,
                    Fmt.hoursMinutes(now - snap.nextSunrise, fa),
                    Fmt.hoursMinutes(snap.nextSunset - now, fa)
                )
            } else stringResource(R.string.bar_not_started),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BarRow(label: String, progress: Float, detail: String, color: Color) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700),
        label = "bar"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}
