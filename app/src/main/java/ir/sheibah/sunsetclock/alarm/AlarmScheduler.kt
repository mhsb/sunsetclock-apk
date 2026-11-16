package ir.sheibah.sunsetclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ir.sheibah.sunsetclock.MainActivity
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.core.AlarmCalc
import ir.sheibah.sunsetclock.core.AlarmItem
import ir.sheibah.sunsetclock.core.Place
import ir.sheibah.sunsetclock.data.AlarmStore
import ir.sheibah.sunsetclock.data.SettingsRepo
import ir.sheibah.sunsetclock.data.TimerStore

/**
 * زمان‌بندی هشدارها با AlarmManager.setAlarmClock (دقیق، بدون نیاز به مجوز «آلارم دقیق»،
 * و در حالت Doze هم به‌موقع اجرا می‌شود).
 */
object AlarmScheduler {

    private const val SNOOZE_BASE = 1_000_000
    private const val TIMER_REQ = 2_000_000

    /** همه‌ی هشدارهای فعال را دوباره زمان‌بندی می‌کند (بعد از بوت، تغییر مکان/روش، باز شدن اپ). */
    fun scheduleAll(context: Context) {
        val settings = SettingsRepo.get(context).state.value
        val place = settings.place()
        AlarmStore.all(context).forEach { item ->
            if (item.enabled) schedule(context, item, place, settings.method) else cancel(context, item.id)
        }
        val timer = TimerStore.load(context)
        if (timer.isRunning) {
            if (timer.endAt > System.currentTimeMillis()) scheduleTimer(context, timer.endAt)
            else TimerStore.clear(context)
        }
    }

    fun schedule(context: Context, item: AlarmItem): Long? {
        val settings = SettingsRepo.get(context).state.value
        return schedule(context, item, settings.place(), settings.method)
    }

    /** زمان بعدی هشدار را حساب و ثبت می‌کند؛ اگر زمانی پیدا نشد لغو می‌کند. */
    fun schedule(context: Context, item: AlarmItem, place: Place, method: CalcMethod): Long? {
        val trigger = AlarmCalc.nextTrigger(item, System.currentTimeMillis(), place, method)
        if (trigger == null) {
            cancel(context, item.id)
            return null
        }
        setClock(context, alarmIntent(context, item.id), trigger)
        return trigger
    }

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmIntent(context, id)
        am.cancel(pi)
        pi.cancel()
    }

    fun scheduleSnooze(context: Context, alarmId: Int, label: String, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_SNOOZE_FIRE)
            .putExtra(AlarmService.EXTRA_ID, alarmId)
            .putExtra(AlarmService.EXTRA_LABEL, label)
        val pi = PendingIntent.getBroadcast(
            context, SNOOZE_BASE + alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setClock(context, pi, at)
    }

    fun scheduleTimer(context: Context, endAt: Long) {
        setClock(context, timerIntent(context), endAt)
    }

    fun cancelTimer(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = timerIntent(context)
        am.cancel(pi)
        pi.cancel()
    }

    private fun alarmIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .putExtra(AlarmService.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun timerIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_TIMER_FIRE)
        return PendingIntent.getBroadcast(
            context, TIMER_REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setClock(context: Context, operation: PendingIntent, at: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
    }
}
