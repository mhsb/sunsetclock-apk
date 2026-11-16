package ir.sheibah.sunsetclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.sheibah.sunsetclock.data.AlarmStore
import ir.sheibah.sunsetclock.data.TimerStore

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM -> {
                val id = intent.getIntExtra(AlarmService.EXTRA_ID, 0)
                val item = AlarmStore.get(context, id) ?: return
                if (!item.enabled) return
                AlarmService.ring(context, id, item.label, isTimer = false)
                if (item.isRepeating) {
                    // نوبت بعدی (برای هشدارهای نسبت‌به‌اذان، زمانِ همان روز محاسبه می‌شود)
                    AlarmScheduler.schedule(context, item)
                } else {
                    AlarmStore.save(context, item.copy(enabled = false))
                    AlarmScheduler.cancel(context, id)
                }
            }
            ACTION_SNOOZE_FIRE -> {
                val id = intent.getIntExtra(AlarmService.EXTRA_ID, 0)
                val label = intent.getStringExtra(AlarmService.EXTRA_LABEL).orEmpty()
                AlarmService.ring(context, id, label, isTimer = false)
            }
            ACTION_TIMER_FIRE -> {
                TimerStore.clear(context)
                AlarmService.ring(context, 0, "", isTimer = true)
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "ir.sheibah.sunsetclock.ALARM_FIRE"
        const val ACTION_SNOOZE_FIRE = "ir.sheibah.sunsetclock.SNOOZE_FIRE"
        const val ACTION_TIMER_FIRE = "ir.sheibah.sunsetclock.TIMER_FIRE"
    }
}
