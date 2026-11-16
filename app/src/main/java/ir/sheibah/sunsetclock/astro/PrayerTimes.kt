package ir.sheibah.sunsetclock.astro

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** رویدادهای روزانه‌ای که اوقات شرعی و هشدارها به آن‌ها ارجاع می‌دهند. */
enum class PrayerEvent { FAJR, SUNRISE, DHUHR, ASR, SUNSET, MAGHRIB, ISHA, MIDNIGHT }

/**
 * روش‌های محاسبه (پارامترها مطابق الگوریتم متن‌باز PrayTimes.org).
 * [ishaMinutes] > 0 یعنی عشا = مغرب + چند دقیقه (مثل ام‌القری).
 * [maghribAngle] = 0 یعنی مغرب = لحظه‌ی غروب خورشید.
 */
enum class CalcMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutes: Int,
    val maghribAngle: Double,
    val jafariMidnight: Boolean,
    val showAsr: Boolean
) {
    TEHRAN(17.7, 14.0, 0, 4.5, true, false),
    LEVA(16.0, 14.0, 0, 4.0, true, false),
    MWL(18.0, 17.0, 0, 0.0, false, true),
    ISNA(15.0, 15.0, 0, 0.0, false, true),
    EGYPT(19.5, 17.5, 0, 0.0, false, true),
    MAKKAH(18.5, 0.0, 90, 0.0, false, true),
    KARACHI(18.0, 18.0, 0, 0.0, false, true)
}

object PrayerTimes {

    private const val MS_MIN = 60_000L

    private val cache = object : LinkedHashMap<String, Map<PrayerEvent, Long>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<PrayerEvent, Long>>?): Boolean {
            return size > 64
        }
    }

    /**
     * زمان رویدادهای یک روز تقویمی (به میلی‌ثانیه‌ی epoch، گرد‌شده به نزدیک‌ترین دقیقه).
     * نیمه‌شب شرعی همیشه بعد از غروب همین روز تقویمی است.
     */
    @Synchronized
    fun compute(
        date: LocalDate,
        zone: ZoneId,
        lat: Double,
        lng: Double,
        method: CalcMethod
    ): Map<PrayerEvent, Long> {
        val key = "${date}|${zone.id}|$lat|$lng|${method.name}"
        cache[key]?.let { return it }
        val result = computeUncached(date, zone, lat, lng, method)
        cache[key] = result
        return result
    }

    private fun computeUncached(
        date: LocalDate,
        zone: ZoneId,
        lat: Double,
        lng: Double,
        method: CalcMethod
    ): Map<PrayerEvent, Long> {
        val today = raw(date, zone, lat, lng, method)
        val tomorrow = raw(date.plusDays(1), zone, lat, lng, method)

        // نیمه‌شب: میانه‌ی غروب تا (اذان صبح یا طلوع) فردا
        val endOfNight = if (method.jafariMidnight) tomorrow.fajr else tomorrow.sunrise
        val midnightHours = today.sunset + (endOfNight + 24.0 - today.sunset) / 2.0

        val base = baseEpochSeconds(date, zone)
        fun ms(hours: Double): Long = (((base + hours * 3600.0) * 1000.0 / MS_MIN).roundToLong()) * MS_MIN

        return linkedMapOf(
            PrayerEvent.FAJR to ms(today.fajr),
            PrayerEvent.SUNRISE to ms(today.sunrise),
            PrayerEvent.DHUHR to ms(today.dhuhr),
            PrayerEvent.ASR to ms(today.asr),
            PrayerEvent.SUNSET to ms(today.sunset),
            PrayerEvent.MAGHRIB to ms(today.maghrib),
            PrayerEvent.ISHA to ms(today.isha),
            PrayerEvent.MIDNIGHT to ms(midnightHours)
        )
    }

    /** نیمه‌شب تقویمی (به وقت محلیِ ظهرِ همان روز) به ثانیه‌ی epoch. */
    private fun baseEpochSeconds(date: LocalDate, zone: ZoneId): Long {
        val offsetSec = date.atTime(12, 0).atZone(zone).offset.totalSeconds
        return date.atStartOfDay(ZoneOffset.UTC).toEpochSecond() - offsetSec
    }

    private class Raw(
        val fajr: Double,
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val sunset: Double,
        val maghrib: Double,
        val isha: Double
    )

    /** زمان‌ها به «ساعتِ محلی» (اعشاری). */
    private fun raw(date: LocalDate, zone: ZoneId, lat: Double, lng: Double, method: CalcMethod): Raw {
        val offsetHours = date.atTime(12, 0).atZone(zone).offset.totalSeconds / 3600.0
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)
        val shift = offsetHours - lng / 15.0

        fun midDay(t: Double): Double = fixHour(12.0 - sunPosition(jd + t).second)

        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jd + t).first
            val noon = midDay(t)
            val x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            val h = 1.0 / 15.0 * darccos(x)
            return noon + if (ccw) -h else h
        }

        fun asrTime(factor: Double, t: Double): Double {
            val decl = sunPosition(jd + t).first
            val angle = -darccot(factor + dtan(abs(lat - decl)))
            return angleTime(angle, t, false)
        }

        val rise = 0.833
        var fajr = angleTime(method.fajrAngle, 5.0 / 24.0, true)
        var sunrise = angleTime(rise, 6.0 / 24.0, true)
        val dhuhr = midDay(12.0 / 24.0)
        val asr = asrTime(1.0, 13.0 / 24.0)
        var sunset = angleTime(rise, 18.0 / 24.0, false)
        var maghrib = if (method.maghribAngle > 0.0) angleTime(method.maghribAngle, 18.0 / 24.0, false) else sunset
        var isha = if (method.ishaMinutes > 0) Double.NaN else angleTime(method.ishaAngle, 18.0 / 24.0, false)

        // اصلاح به ساعت محلی
        fajr += shift
        sunrise += shift
        var dhuhrL = dhuhr + shift
        var asrL = asr + shift
        sunset += shift
        maghrib += shift
        isha += shift

        // پشتیبان برای عرض‌های جغرافیایی بالا که زاویه‌ها تعریف نمی‌شوند
        if (sunrise.isNaN() || sunset.isNaN()) {
            val noon = dhuhrL
            sunrise = noon - 6.0
            sunset = noon + 6.0
            if (maghrib.isNaN()) maghrib = sunset
        }
        if (maghrib.isNaN()) maghrib = sunset
        if (method.ishaMinutes > 0) isha = maghrib + method.ishaMinutes / 60.0
        if (fajr.isNaN()) fajr = sunrise - 1.5
        if (isha.isNaN()) isha = maghrib + 1.5
        if (dhuhrL.isNaN()) dhuhrL = 12.0
        if (asrL.isNaN()) asrL = dhuhrL + 3.0

        return Raw(fajr, sunrise, dhuhrL, asrL, sunset, maghrib, isha)
    }

    // ---- ستاره‌شناسی ----

    private fun julian(year0: Int, month0: Int, day: Int): Double {
        var year = year0
        var month = month0
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5
    }

    /** (میل خورشید به درجه، معادله‌ی زمان به ساعت) */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(dsin(e) * dsin(l)))
        return Pair(decl, eqt)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double) = a - 24.0 * floor(a / 24.0)
}
