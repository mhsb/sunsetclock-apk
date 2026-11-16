package ir.sheibah.sunsetclock.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.SunsetClockApp
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date

/** سرویس پیش‌زمینه‌ای که صدا و لرزش هشدار را پخش می‌کند. */
class AlarmService : Service() {

    data class RingInfo(val alarmId: Int, val label: String, val isTimer: Boolean)

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> stopRinging()
            ACTION_SNOOZE -> {
                val info = ringing.value
                if (info != null && !info.isTimer) {
                    AlarmScheduler.scheduleSnooze(this, info.alarmId, info.label, SNOOZE_MINUTES)
                }
                stopRinging()
            }
            else -> startRinging(intent)
        }
        return START_NOT_STICKY
    }

    private fun startRinging(intent: Intent?) {
        val info = RingInfo(
            alarmId = intent?.getIntExtra(EXTRA_ID, 0) ?: 0,
            label = intent?.getStringExtra(EXTRA_LABEL).orEmpty(),
            isTimer = intent?.getBooleanExtra(EXTRA_TIMER, false) ?: false
        )
        ringing.value = info

        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(info),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        stopSound()
        startSound()
        startVibration()

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, MAX_RING_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacks(timeout)
        stopSound()
        stopVibration()
        ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        stopSound()
        stopVibration()
        ringing.value = null
        super.onDestroy()
    }

    private fun startSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            player = null
        }
    }

    private fun stopSound() {
        try {
            player?.stop()
        } catch (e: Exception) {
            // نادیده گرفته می‌شود
        }
        player?.release()
        player = null
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            v.vibrate(effect, attrs)
            vibrator = v
        } catch (e: Exception) {
            vibrator = null
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // نادیده گرفته می‌شود
        }
        vibrator = null
    }

    private fun buildNotification(info: RingInfo): Notification {
        val title = when {
            info.isTimer -> getString(R.string.timer_done)
            info.label.isNotBlank() -> info.label
            else -> getString(R.string.alarm)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val dismiss = PendingIntent.getService(
            this, 1, Intent(this, AlarmService::class.java).setAction(ACTION_DISMISS), flags
        )
        val snooze = PendingIntent.getService(
            this, 2, Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE), flags
        )

        val builder = NotificationCompat.Builder(this, SunsetClockApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_sun)
            .setContentTitle(title)
            .setContentText(DateFormat.getTimeFormat(this).format(Date()))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
        if (!info.isTimer) builder.addAction(0, getString(R.string.snooze), snooze)
        builder.addAction(0, getString(R.string.dismiss), dismiss)
        return builder.build()
    }

    companion object {
        const val ACTION_START = "ir.sheibah.sunsetclock.START_RING"
        const val ACTION_DISMISS = "ir.sheibah.sunsetclock.DISMISS"
        const val ACTION_SNOOZE = "ir.sheibah.sunsetclock.SNOOZE"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_TIMER = "is_timer"

        private const val NOTIFICATION_ID = 4711
        const val SNOOZE_MINUTES = 10
        private const val MAX_RING_MS = 5 * 60 * 1000L

        /** وضعیت زنگ خوردن؛ صفحه‌ی هشدار آن را مشاهده می‌کند. */
        val ringing = MutableStateFlow<RingInfo?>(null)

        fun ring(context: Context, id: Int, label: String, isTimer: Boolean) {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_TIMER, isTimer)
            ContextCompat.startForegroundService(context, intent)
        }

        fun dismiss(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))
        }

        fun snooze(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))
        }
    }
}
