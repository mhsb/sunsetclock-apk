package ir.sheibah.sunsetclock.data

import android.content.Context
import ir.sheibah.sunsetclock.astro.PrayerEvent
import ir.sheibah.sunsetclock.core.AlarmItem
import ir.sheibah.sunsetclock.core.AlarmKind
import org.json.JSONArray
import org.json.JSONObject

/** ذخیره‌ی هشدارها به‌صورت JSON در SharedPreferences. */
object AlarmStore {

    private const val PREFS = "alarms"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT_ID = "next_id"

    @Synchronized
    fun all(context: Context): List<AlarmItem> {
        val raw = prefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> fromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun get(context: Context, id: Int): AlarmItem? = all(context).firstOrNull { it.id == id }

    /** ذخیره یا جایگزینی هشدار؛ اگر id صفر بود شناسه‌ی جدید می‌سازد و هشدار ذخیره‌شده را برمی‌گرداند. */
    @Synchronized
    fun save(context: Context, item: AlarmItem): AlarmItem {
        val p = prefs(context)
        val saved = if (item.id == 0) {
            val id = p.getInt(KEY_NEXT_ID, 1)
            p.edit().putInt(KEY_NEXT_ID, id + 1).apply()
            item.copy(id = id)
        } else item
        val list = all(context).toMutableList()
        val idx = list.indexOfFirst { it.id == saved.id }
        if (idx >= 0) list[idx] = saved else list.add(saved)
        write(context, list)
        return saved
    }

    @Synchronized
    fun delete(context: Context, id: Int) {
        write(context, all(context).filter { it.id != id })
    }

    private fun write(context: Context, list: List<AlarmItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY_LIST, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun toJson(a: AlarmItem): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("label", a.label)
        put("enabled", a.enabled)
        put("kind", a.kind.name)
        put("hour", a.hour)
        put("minute", a.minute)
        put("anchor", a.anchor.name)
        put("offset", a.offsetMin)
        put("days", a.daysMask)
    }

    private fun fromJson(o: JSONObject): AlarmItem? = try {
        AlarmItem(
            id = o.getInt("id"),
            label = o.optString("label", ""),
            enabled = o.optBoolean("enabled", true),
            kind = AlarmKind.valueOf(o.optString("kind", AlarmKind.CLOCK.name)),
            hour = o.optInt("hour", 6),
            minute = o.optInt("minute", 0),
            anchor = PrayerEvent.valueOf(o.optString("anchor", PrayerEvent.FAJR.name)),
            offsetMin = o.optInt("offset", 0),
            daysMask = o.optInt("days", 0)
        )
    } catch (e: Exception) {
        null
    }
}
