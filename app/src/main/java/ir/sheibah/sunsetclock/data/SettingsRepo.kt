package ir.sheibah.sunsetclock.data

import android.content.Context
import android.content.SharedPreferences
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.core.Cities
import ir.sheibah.sunsetclock.core.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val cityId: String = Cities.DEFAULT_ID,
    val useGps: Boolean = false,
    val hasGpsFix: Boolean = false,
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val method: CalcMethod = CalcMethod.TEHRAN,
    val hijriAdjust: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM
) {
    /** مکان مؤثر: موقعیت تقریبی GPS اگر فعال و موجود باشد، وگرنه شهر انتخابی. */
    fun place(): Place =
        if (useGps && hasGpsFix) Place(gpsLat, gpsLng, ZoneId.systemDefault())
        else Cities.byId(cityId).toPlace()
}

class SettingsRepo private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            cityId = prefs.getString("city", d.cityId) ?: d.cityId,
            useGps = prefs.getBoolean("use_gps", d.useGps),
            hasGpsFix = prefs.getBoolean("has_gps_fix", d.hasGpsFix),
            gpsLat = Double.fromBits(prefs.getLong("gps_lat", d.gpsLat.toBits())),
            gpsLng = Double.fromBits(prefs.getLong("gps_lng", d.gpsLng.toBits())),
            method = runCatching { CalcMethod.valueOf(prefs.getString("method", d.method.name) ?: d.method.name) }
                .getOrDefault(d.method),
            hijriAdjust = prefs.getInt("hijri_adjust", d.hijriAdjust).coerceIn(-2, 2),
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", d.theme.name) ?: d.theme.name) }
                .getOrDefault(d.theme)
        )
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_state.value)
        prefs.edit()
            .putString("city", s.cityId)
            .putBoolean("use_gps", s.useGps)
            .putBoolean("has_gps_fix", s.hasGpsFix)
            .putLong("gps_lat", s.gpsLat.toBits())
            .putLong("gps_lng", s.gpsLng.toBits())
            .putString("method", s.method.name)
            .putInt("hijri_adjust", s.hijriAdjust)
            .putString("theme", s.theme.name)
            .apply()
        _state.value = s
    }

    companion object {
        @Volatile
        private var instance: SettingsRepo? = null

        fun get(context: Context): SettingsRepo =
            instance ?: synchronized(this) {
                instance ?: SettingsRepo(context.applicationContext).also { instance = it }
            }
    }
}
