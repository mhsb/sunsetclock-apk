package ir.sheibah.sunsetclock.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.data.ThemeMode

private data class Tab(val icon: ImageVector, @StringRes val label: Int)

private val tabs = listOf(
    Tab(Icons.Filled.WbTwilight, R.string.tab_clock),
    Tab(Icons.Filled.Alarm, R.string.tab_alarms),
    Tab(Icons.Filled.Timer, R.string.tab_stopwatch),
    Tab(Icons.Filled.HourglassBottom, R.string.tab_timer),
    Tab(Icons.Filled.Settings, R.string.tab_settings)
)

@Composable
fun SunsetApp(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dark = when (settings.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // وقتی برنامه جلو می‌آید: هشدارها را دوباره بخوان و زمان‌بندی کن
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.reloadAlarms()
        vm.rescheduleAll()
    }

    SunsetTheme(darkTheme = dark) {
        if (!settings.onboarded) {
            OnboardingScreen(vm = vm)
            return@SunsetTheme
        }
        var tab by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            modifier = Modifier.background(appBackgroundBrush()),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.softShadow(androidx.compose.ui.graphics.RectangleShape, 16.dp)
                ) {
                    tabs.forEachIndexed { index, t ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = {
                                val sc by animateFloatAsState(
                                    if (tab == index) 1.18f else 1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "navIcon"
                                )
                                Icon(t.icon, contentDescription = null, modifier = Modifier.scale(sc))
                            },
                            label = { Text(stringResource(t.label)) }
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .fillMaxSize(),
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { full -> full / 10 * dir }) togetherWith
                        fadeOut(tween(120))
                },
                label = "tabs"
            ) { page ->
                when (page) {
                    0 -> ClockScreen(vm = vm, onOpenSettings = { tab = 4 })
                    1 -> AlarmsScreen(vm = vm)
                    2 -> StopwatchScreen(vm = vm)
                    3 -> TimerScreen(vm = vm)
                    else -> SettingsScreen(vm = vm)
                }
            }
        }
    }
}
