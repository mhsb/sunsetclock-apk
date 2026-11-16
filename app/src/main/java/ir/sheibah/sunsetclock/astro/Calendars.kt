package ir.sheibah.sunsetclock.astro

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** تاریخ سه‌تایی (سال، ماه، روز). */
data class YMD(val year: Int, val month: Int, val day: Int)

object Calendars {

    /** تبدیل میلادی به شمسی (الگوریتم ۳۳ ساله‌ی رایج؛ برای سال‌های ۱۳۰۰ تا ۱۴۷۰ درست است). */
    fun toJalali(date: LocalDate): YMD {
        var gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val cumulative = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy: Int
        if (gy > 1600) {
            jy = 979
            gy -= 1600
        } else {
            jy = 0
            gy -= 621
        }
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + cumulative[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return YMD(jy, jm, jd)
    }

    /** تاریخ قمری (ام‌القری) با تعدیل ±۱ روز؛ در صورت خارج بودن از بازه null برمی‌گرداند. */
    fun toHijri(date: LocalDate, adjustDays: Int): YMD? {
        return try {
            val h = HijrahDate.from(date.plusDays(adjustDays.toLong()))
            YMD(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
        } catch (e: Exception) {
            null
        }
    }
}
