package ir.sheibah.sunsetclock

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.sheibah.sunsetclock.alarm.AlarmScheduler
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.core.AlarmItem
import ir.sheibah.sunsetclock.data.AlarmStore
import ir.sheibah.sunsetclock.data.AppSettings
import ir.sheibah.sunsetclock.data.LocationHelper
import ir.sheibah.sunsetclock.data.SettingsRepo
import ir.sheibah.sunsetclock.data.ThemeMode
import ir.sheibah.sunsetclock.data.TimerData
import ir.sheibah.sunsetclock.data.TimerStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class StopwatchState(
    val running: Boolean = false,
    /** SystemClock.elapsedRealtime() در لحظه‌ی شروع/ادامه */
    val startedAt: Long = 0L,
    val accumulated: Long = 0L,
    /** زمان کل در لحظه‌ی ثبت هر دور (جدیدترین اول) */
    val laps: List<Long> = emptyList()
) {
    fun elapsed(nowElapsed: Long): Long =
        accumulated + if (running) (nowElapsed - startedAt).coerceAtLeast(0L) else 0L
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val repo = SettingsRepo.get(ctx)

    val settings: StateFlow<AppSettings> = repo.state

    /** ساعت دیواری؛ فقط وقتی صفحه دیده می‌شود هر ۲۵۰ میلی‌ثانیه به‌روز می‌شود. */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(250)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // ---- هشدارها ----
    private val _alarms = MutableStateFlow(AlarmStore.all(ctx))
    val alarms: StateFlow<List<AlarmItem>> = _alarms

    fun reloadAlarms() {
        _alarms.value = AlarmStore.all(ctx)
        syncTimer()
    }

    fun saveAlarm(item: AlarmItem) {
        val saved = AlarmStore.save(ctx, item)
        if (saved.enabled) AlarmScheduler.schedule(ctx, saved) else AlarmScheduler.cancel(ctx, saved.id)
        _alarms.value = AlarmStore.all(ctx)
    }

    fun setAlarmEnabled(item: AlarmItem, enabled: Boolean) = saveAlarm(item.copy(enabled = enabled))

    fun deleteAlarm(id: Int) {
        AlarmScheduler.cancel(ctx, id)
        AlarmStore.delete(ctx, id)
        _alarms.value = AlarmStore.all(ctx)
    }

    fun rescheduleAll() = AlarmScheduler.scheduleAll(ctx)

    // ---- تنظیمات ----
    fun setCity(id: String) {
        repo.update { it.copy(cityId = id, useGps = false) }
        rescheduleAll()
    }

    fun setMethod(m: CalcMethod) {
        repo.update { it.copy(method = m) }
        rescheduleAll()
    }

    fun setHijriAdjust(v: Int) = repo.update { it.copy(hijriAdjust = v.coerceIn(-2, 2)) }

    fun setTheme(t: ThemeMode) = repo.update { it.copy(theme = t) }

    fun setSunCcw(ccw: Boolean) = repo.update { it.copy(sunCcw = ccw) }

    fun completeOnboarding() {
        repo.update { it.copy(onboarded = true) }
        rescheduleAll()
    }

    fun disableGps() {
        repo.update { it.copy(useGps = false) }
        rescheduleAll()
    }

    /** موقعیت تقریبی را می‌گیرد؛ نتیجه (موفق/ناموفق) به onDone داده می‌شود. */
    fun useApproximateLocation(onDone: (Boolean) -> Unit) {
        LocationHelper.requestFix(ctx) { loc ->
            if (loc != null) {
                repo.update {
                    it.copy(useGps = true, hasGpsFix = true, gpsLat = loc.latitude, gpsLng = loc.longitude)
                }
                rescheduleAll()
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    // ---- کرنومتر ----
    private val _stopwatch = MutableStateFlow(StopwatchState())
    val stopwatch: StateFlow<StopwatchState> = _stopwatch

    fun stopwatchToggle() {
        val s = _stopwatch.value
        val t = SystemClock.elapsedRealtime()
        _stopwatch.value = if (s.running) {
            s.copy(running = false, accumulated = s.elapsed(t))
        } else {
            s.copy(running = true, startedAt = t)
        }
    }

    fun stopwatchLap() {
        val s = _stopwatch.value
        if (!s.running) return
        val total = s.elapsed(SystemClock.elapsedRealtime())
        _stopwatch.value = s.copy(laps = listOf(total) + s.laps)
    }

    fun stopwatchReset() {
        _stopwatch.value = StopwatchState()
    }

    // ---- تایمر ----
    private val _timer = MutableStateFlow(TimerStore.load(ctx))
    val timer: StateFlow<TimerData> = _timer

    private fun setTimer(d: TimerData) {
        TimerStore.save(ctx, d)
        _timer.value = d
    }

    fun timerStart(totalMs: Long) {
        if (totalMs <= 0L) return
        val end = System.currentTimeMillis() + totalMs
        setTimer(TimerData(totalMs = totalMs, endAt = end, pausedRemaining = -1L))
        AlarmScheduler.scheduleTimer(ctx, end)
    }

    fun timerPause() {
        val t = _timer.value
        if (!t.isRunning) return
        val remaining = t.remaining(System.currentTimeMillis())
        AlarmScheduler.cancelTimer(ctx)
        setTimer(TimerData(totalMs = t.totalMs, endAt = 0L, pausedRemaining = remaining))
    }

    fun timerResume() {
        val t = _timer.value
        if (!t.isPaused) return
        val end = System.currentTimeMillis() + t.pausedRemaining
        setTimer(TimerData(totalMs = t.totalMs, endAt = end, pausedRemaining = -1L))
        AlarmScheduler.scheduleTimer(ctx, end)
    }

    fun timerAddMinute() {
        val t = _timer.value
        when {
            t.isRunning -> {
                val end = t.endAt + 60_000L
                setTimer(t.copy(totalMs = t.totalMs + 60_000L, endAt = end))
                AlarmScheduler.scheduleTimer(ctx, end)
            }
            t.isPaused -> setTimer(
                t.copy(totalMs = t.totalMs + 60_000L, pausedRemaining = t.pausedRemaining + 60_000L)
            )
        }
    }

    fun timerReset() {
        AlarmScheduler.cancelTimer(ctx)
        setTimer(TimerData())
    }

    /** اگر تایمر در پس‌زمینه تمام شده باشد، وضعیت را بیکار می‌کند. */
    fun syncTimer() {
        val t = TimerStore.load(ctx)
        _timer.value = if (t.isRunning && t.endAt <= System.currentTimeMillis()) {
            TimerStore.clear(ctx)
            TimerData()
        } else t
    }
}
