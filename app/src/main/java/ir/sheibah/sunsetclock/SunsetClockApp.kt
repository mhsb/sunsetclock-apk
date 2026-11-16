package ir.sheibah.sunsetclock

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

class SunsetClockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
