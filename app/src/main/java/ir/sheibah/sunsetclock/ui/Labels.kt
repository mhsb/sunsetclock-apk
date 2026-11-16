package ir.sheibah.sunsetclock.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.astro.PrayerEvent

@StringRes
fun PrayerEvent.labelRes(): Int = when (this) {
    PrayerEvent.FAJR -> R.string.ev_fajr
    PrayerEvent.SUNRISE -> R.string.ev_sunrise
    PrayerEvent.DHUHR -> R.string.ev_dhuhr
    PrayerEvent.ASR -> R.string.ev_asr
    PrayerEvent.SUNSET -> R.string.ev_sunset
    PrayerEvent.MAGHRIB -> R.string.ev_maghrib
    PrayerEvent.ISHA -> R.string.ev_isha
    PrayerEvent.MIDNIGHT -> R.string.ev_midnight
}

@StringRes
fun CalcMethod.labelRes(): Int = when (this) {
    CalcMethod.TEHRAN -> R.string.method_tehran
    CalcMethod.LEVA -> R.string.method_leva
    CalcMethod.MWL -> R.string.method_mwl
    CalcMethod.ISNA -> R.string.method_isna
    CalcMethod.EGYPT -> R.string.method_egypt
    CalcMethod.MAKKAH -> R.string.method_makkah
    CalcMethod.KARACHI -> R.string.method_karachi
}

/** ترتیب نمایش روزهای هفته (اندیس ۰=دوشنبه ... ۶=یکشنبه): فارسی از شنبه شروع می‌شود. */
fun weekOrder(fa: Boolean): List<Int> =
    if (fa) listOf(5, 6, 0, 1, 2, 3, 4) else listOf(0, 1, 2, 3, 4, 5, 6)

/** «تا ۳ ساعت و ۱۰ دقیقه دیگر» */
@Composable
fun remainingText(ms: Long, fa: Boolean): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0L)
    return when {
        totalMin < 1L -> stringResource(R.string.in_less_than_min)
        totalMin < 60L -> stringResource(R.string.in_min, Fmt.num(totalMin.toInt(), fa))
        totalMin < 24L * 60L -> stringResource(
            R.string.in_hm,
            Fmt.num((totalMin / 60L).toInt(), fa),
            Fmt.num((totalMin % 60L).toInt(), fa)
        )
        else -> stringResource(
            R.string.in_dh,
            Fmt.num((totalMin / 1440L).toInt(), fa),
            Fmt.num(((totalMin % 1440L) / 60L).toInt(), fa)
        )
    }
}

/** خلاصه‌ی روزهای تکرار هشدار */
@Composable
fun daysSummary(mask: Int, fa: Boolean): String {
    val shortNames = stringArrayResource(R.array.weekdays_short)
    return when (mask) {
        0 -> stringResource(R.string.once)
        127 -> stringResource(R.string.every_day)
        else -> weekOrder(fa)
            .filter { mask and (1 shl it) != 0 }
            .joinToString(if (fa) "، " else ", ") { shortNames[it] }
    }
}
