package ir.sheibah.sunsetclock.core

import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.astro.PrayerEvent
import ir.sheibah.sunsetclock.astro.PrayerTimes
import java.time.Instant
import java.time.LocalDate

data class TimedEvent(val event: PrayerEvent, val time: Long)

/**
 * وضعیت لحظه‌ای «ساعت غروب‌کوک».
 * ساعت صفر = لحظه‌ی غروب؛ فاصله‌ی دو غروب پیاپی به ۲۴ ساعتِ مساوی تقسیم می‌شود.
 */
data class ClockSnapshot(
    val now: Long,
    val lastSunset: Long,
    val nextSunset: Long,
    val nextSunrise: Long,
    /** ثانیه‌ی سپری‌شده از غروب، مقیاس‌شده به ۰..۸۶۴۰۰ */
    val sunsetSeconds: Double,
    val isNight: Boolean,
    val nightProgress: Double,
    val dayProgress: Double,
    /** تاریخ تقویمی «روز شرعی» جاری (روز از غروب شروع می‌شود، پس بعد از غروب = فردا) */
    val islamicDate: LocalDate,
    /** رویدادهای بین غروب قبلی و غروب بعدی (برای نمایش روی صفحه‌ی ساعت) */
    val cycleEvents: List<TimedEvent>,
    /** رویدادهای روز تقویمی جاری، مرتب‌شده بر اساس زمان */
    val todayEvents: List<TimedEvent>,
    val next: TimedEvent?
)

object ClockCalc {

    fun snapshot(now: Long, place: Place, method: CalcMethod): ClockSnapshot {
        val today = Instant.ofEpochMilli(now).atZone(place.zone).toLocalDate()

        val perDay = (-1L..2L).map { off ->
            val d = today.plusDays(off)
            PrayerTimes.compute(d, place.zone, place.lat, place.lng, method)
        }
        fun visible(e: PrayerEvent) = e != PrayerEvent.ASR || method.showAsr

        val all = perDay
            .flatMap { m -> m.map { TimedEvent(it.key, it.value) } }
            .sortedBy { it.time }

        val sunsets = all.filter { it.event == PrayerEvent.SUNSET }.map { it.time }
        val lastSunset = sunsets.last { it <= now }
        val nextSunset = sunsets.first { it > now }
        val nextSunrise = all.first { it.event == PrayerEvent.SUNRISE && it.time > lastSunset }.time

        val cycle = (nextSunset - lastSunset).toDouble()
        val seconds = (now - lastSunset).toDouble() / cycle * 86400.0
        val isNight = now < nextSunrise
        val nightProgress = if (isNight) (now - lastSunset).toDouble() / (nextSunrise - lastSunset) else 1.0
        val dayProgress = if (isNight) 0.0 else (now - nextSunrise).toDouble() / (nextSunset - nextSunrise)

        val islamic = Instant.ofEpochMilli(lastSunset).atZone(place.zone).toLocalDate().plusDays(1)

        val cycleEvents = all.filter {
            it.time in lastSunset until nextSunset && it.event != PrayerEvent.SUNSET && visible(it.event)
        }
        val todayEvents = perDay[1].map { TimedEvent(it.key, it.value) }
            .filter { visible(it.event) }
            .sortedBy { it.time }
        val next = all.firstOrNull { it.time > now && visible(it.event) }

        return ClockSnapshot(
            now = now,
            lastSunset = lastSunset,
            nextSunset = nextSunset,
            nextSunrise = nextSunrise,
            sunsetSeconds = seconds.coerceIn(0.0, 86399.999),
            isNight = isNight,
            nightProgress = nightProgress.coerceIn(0.0, 1.0),
            dayProgress = dayProgress.coerceIn(0.0, 1.0),
            islamicDate = islamic,
            cycleEvents = cycleEvents,
            todayEvents = todayEvents,
            next = next
        )
    }
}
