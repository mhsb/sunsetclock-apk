package ir.sheibah.sunsetclock.data

import android.content.Context

/**
 * وضعیت تایمر شمارش معکوس.
 *  - totalMs == 0: بیکار
 *  - endAt > 0: در حال اجرا (پایان در لحظه‌ی endAt)
 *  - pausedRemaining >= 0: متوقف با این مقدار باقی‌مانده
 */
data class TimerData(
    val totalMs: Long = 0L,
    val endAt: Long = 0L,
    val pausedRemaining: Long = -1L
) {
    val isIdle: Boolean get() = totalMs == 0L
    val isRunning: Boolean get() = totalMs > 0L && endAt > 0L
    val isPaused: Boolean get() = totalMs > 0L && endAt == 0L && pausedRemaining >= 0L

    fun remaining(now: Long): Long = when {
        isRunning -> (endAt - now).coerceAtLeast(0L)
        isPaused -> pausedRemaining
        else -> 0L
    }
}

object TimerStore {
    private const val PREFS = "timer"

    fun load(context: Context): TimerData {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return TimerData(
            totalMs = p.getLong("total", 0L),
            endAt = p.getLong("end_at", 0L),
            pausedRemaining = p.getLong("paused", -1L)
        )
    }

    fun save(context: Context, data: TimerData) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("total", data.totalMs)
            .putLong("end_at", data.endAt)
            .putLong("paused", data.pausedRemaining)
            .apply()
    }

    fun clear(context: Context) = save(context, TimerData())
}
