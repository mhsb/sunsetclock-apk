package ir.sheibah.sunsetclock.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.ui.Fmt
import ir.sheibah.sunsetclock.ui.SunsetTheme
import ir.sheibah.sunsetclock.ui.isFarsi
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** صفحه‌ی تمام‌صفحه‌ای که هنگام زنگ خوردن هشدار (حتی روی قفل صفحه) نمایش داده می‌شود. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            SunsetTheme(darkTheme = true) {
                AlarmScreen(
                    onSnooze = { AlarmService.snooze(this) },
                    onDismiss = { AlarmService.dismiss(this) },
                    onGone = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(onSnooze: () -> Unit, onDismiss: () -> Unit, onGone: () -> Unit) {
    val info by AlarmService.ringing.collectAsStateWithLifecycle()
    val fa = isFarsi()

    LaunchedEffect(info) {
        if (info == null) onGone()
    }

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    val current = info
    val title = when {
        current == null -> ""
        current.isTimer -> stringResource(R.string.timer_done)
        current.label.isNotBlank() -> current.label
        else -> stringResource(R.string.alarm)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1020), Color(0xFF241B4B), Color(0xFF7A3B1E))
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.WbTwilight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = Fmt.hm(now, ZoneId.systemDefault(), fa),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFDDB0)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (current != null && !current.isTimer) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Text(stringResource(R.string.snooze), fontSize = 18.sp)
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text(stringResource(R.string.dismiss), fontSize = 18.sp)
                }
            }
        }
    }
}
