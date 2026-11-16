package ir.sheibah.sunsetclock.core

import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.astro.PrayerEvent
import ir.sheibah.sunsetclock.astro.PrayerTimes
import java.time.DayOfWeek
import java.time.Instant

enum class AlarmKind { CLOCK, PRAYER }

/**
 * یک هشدار. دو نوع دارد:
 *  - CLOCK: ساعت معمولی (hour:minute)
 *  - PRAYER: نسبت به یک رویدادِ اوقات (مثلاً ۴۵ دقیقه قبل از اذان صبح؛ offsetMin منفی = قبل)
 * daysMask: بیت (DayOfWeek.value - 1) برای هر روز هفته (دوشنبه=بیت۰ ... یکشنبه=بیت۶)؛ ۰ یعنی فقط یک‌بار.
 */
data class AlarmItem(
    val id: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val kind: AlarmKind = AlarmKind.CLOCK,
    val hour: Int = 6,
    val minute: Int = 0,
    val anchor: PrayerEvent = PrayerEvent.FAJR,
    val offsetMin: Int = 0,
    val daysMask: Int = 0
) {
    fun repeatsOn(day: DayOfWeek): Boolean = daysMask and (1 shl (day.value - 1)) != 0
    val isRepeating: Boolean get() = daysMask != 0
}

object AlarmCalc {

    /** نزدیک‌ترین زمان بعدی هشدار (epoch ms) یا null. */
    fun nextTrigger(item: AlarmItem, now: Long, place: Place, method: CalcMethod): Long? {
        val startDate = Instant.ofEpochMilli(now).atZone(place.zone).toLocalDate()
        for (off in 0L..8L) {
            val date = startDate.plusDays(off)
            if (item.isRepeating && !item.repeatsOn(date.dayOfWeek)) continue
            val t: Long = when (item.kind) {
                AlarmKind.CLOCK ->
                    date.atTime(item.hour, item.minute).atZone(place.zone).toInstant().toEpochMilli()
                AlarmKind.PRAYER -> {
                    val ev = PrayerTimes.compute(date, place.zone, place.lat, place.lng, method)[item.anchor]
                        ?: continue
                    ev + item.offsetMin * 60_000L
                }
            }
            if (t > now + 1000L) return t
        }
        return null
    }
}
