package ir.sheibah.sunsetclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** بعد از روشن شدن گوشی، تغییر ساعت/منطقه‌ی زمانی یا به‌روزرسانی اپ، هشدارها را دوباره ثبت می‌کند. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.scheduleAll(context)
    }
}
