package ir.sheibah.sunsetclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** آیا زبان فعلی فارسی است؟ (برای ارقام فارسی و جهت‌ها) */
@Composable
fun isFarsi(): Boolean = LocalConfiguration.current.locales[0].language == "fa"

object Fmt {
    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val hmFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    /** تبدیل ارقام انگلیسی به فارسی در صورت نیاز. */
    fun digits(s: String, fa: Boolean): String {
        if (!fa) return s
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') faDigits[c - '0'] else c)
        return sb.toString()
    }

    fun num(n: Int, fa: Boolean): String = digits(n.toString(), fa)

    /** تبدیل ارقام فارسی/عربی به انگلیسی (برای ورودی کاربر). */
    fun ascii(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** ساعت و دقیقه‌ی یک لحظه در منطقه‌ی زمانی داده‌شده، مثل ۰۵:۱۴ */
    fun hm(epochMs: Long, zone: ZoneId, fa: Boolean): String =
        digits(Instant.ofEpochMilli(epochMs).atZone(zone).format(hmFormatter), fa)

    fun hm(hour: Int, minute: Int, fa: Boolean): String =
        digits(String.format(Locale.ENGLISH, "%02d:%02d", hour, minute), fa)

    /** مقدار ثانیه‌ای به صورت HH:mm:ss */
    fun clock(totalSeconds: Long, fa: Boolean): String {
        val s = totalSeconds.coerceAtLeast(0L)
        return digits(String.format(Locale.ENGLISH, "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60), fa)
    }

    /** مدت زمان کوتاه: اگر کمتر از یک ساعت باشد mm:ss وگرنه H:mm:ss */
    fun duration(totalSeconds: Long, fa: Boolean): String {
        val s = totalSeconds.coerceAtLeast(0L)
        val h = s / 3600
        val m = s % 3600 / 60
        val sec = s % 60
        val out = if (h > 0) String.format(Locale.ENGLISH, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.ENGLISH, "%02d:%02d", m, sec)
        return digits(out, fa)
    }

    /** مدت به‌صورت ساعت:دقیقه (بدون ثانیه) برای نوارهای شب/روز */
    fun hoursMinutes(totalMs: Long, fa: Boolean): String {
        val totalMin = (totalMs.coerceAtLeast(0L) / 60_000L)
        return digits(String.format(Locale.ENGLISH, "%d:%02d", totalMin / 60, totalMin % 60), fa)
    }

    /** مرحله‌ی کرنومتر: mm:ss.cc */
    fun stopwatch(ms: Long, fa: Boolean): String {
        val t = ms.coerceAtLeast(0L)
        val minutes = t / 60_000L
        val seconds = t % 60_000L / 1000L
        val centi = t % 1000L / 10L
        return digits(String.format(Locale.ENGLISH, "%02d:%02d.%02d", minutes, seconds, centi), fa)
    }
}
