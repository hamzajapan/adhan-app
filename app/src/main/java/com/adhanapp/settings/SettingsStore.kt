package com.adhanapp.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adhanapp.cities.City
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("adhan_settings")

/** الفاصل الافتراضي بين الأذان والإقامة بالدقائق (كما في التصميم) */
val DEFAULT_IQAMA: Map<String, Int> = linkedMapOf(
    "FAJR" to 15, "DHUHR" to 10, "ASR" to 10, "MAGHRIB" to 5, "ISHA" to 10,
)

data class AppSettings(
    val onboarded: Boolean = false,
    val cityNameAr: String = "",
    val cityNameEn: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timezone: String = "Asia/Riyadh",
    /** معرّف الأذان المعتمد من فهرس إسلام ويب (فارغ = نغمة المنبه الافتراضية) */
    val sound: String = "",
    /** أذان خاص بالفجر (فارغ = الأذان العام) */
    val fajrSound: String = "",
    val method: String = "auto",
    val madhab: String = "auto",
    val enabledPrayers: Set<String> = setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
    /** صلوات يُستبدل أذانها باهتزاز وإشعار صامت (العمل/المسجد) */
    val vibrateOnly: Set<String> = emptySet(),
    val iqama: Map<String, Int> = DEFAULT_IQAMA,
    val favorites: Set<String> = emptySet(),
    /** وقت آخر أذان جُدول في AlarmManager (ميلي ثانية)، 0 = لا شيء مجدول */
    val nextAlarm: Long = 0L,
    /** تذكير قبل الأذان بالدقائق، 0 = معطّل */
    val reminderMinutes: Int = 10,
    val remindFriday: Boolean = true,
    val remindFasting: Boolean = true,
    val mosqueName: String = "",
    val tasbihDate: String = "",
    val tasbihToday: Int = 0,
    val tasbihTotal: Int = 0,
) {
    /** إعادة بناء كائن المدينة من الإعدادات المحفوظة */
    fun toCity(): City = City(cityNameAr, cityNameEn, country, lat, lng, timezone)

    /** اسم المدينة بلغة الواجهة: العربي للعربية، والإنجليزي لغيرها */
    fun cityName(arabic: Boolean): String =
        if (arabic) cityNameAr.ifBlank { cityNameEn } else cityNameEn.ifBlank { cityNameAr }

    /** الأذان المناسب لصلاة معيّنة: أذان الفجر إن حُدد، وإلا العام */
    fun soundFor(prayer: String): String = if (prayer == "FAJR" && fajrSound.isNotBlank()) fajrSound else sound
}

class SettingsStore(private val context: Context) {

    private object K {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CITY_AR = stringPreferencesKey("city_ar")
        val CITY_EN = stringPreferencesKey("city_en")
        val COUNTRY = stringPreferencesKey("country")
        val LAT = doublePreferencesKey("lat")
        val LNG = doublePreferencesKey("lng")
        val TIMEZONE = stringPreferencesKey("timezone")
        val SOUND = stringPreferencesKey("sound")
        val FAJR_SOUND = stringPreferencesKey("fajr_sound")
        val METHOD = stringPreferencesKey("method")
        val MADHAB = stringPreferencesKey("madhab")
        val ENABLED = stringSetPreferencesKey("enabled_prayers")
        val VIBRATE_ONLY = stringSetPreferencesKey("vibrate_only")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val NEXT_ALARM = longPreferencesKey("next_alarm")
        val REMINDER_MIN = intPreferencesKey("reminder_minutes")
        val REMIND_FRIDAY = booleanPreferencesKey("remind_friday")
        val REMIND_FASTING = booleanPreferencesKey("remind_fasting")
        val MOSQUE = stringPreferencesKey("mosque_name")
        val TASBIH_DATE = stringPreferencesKey("tasbih_date")
        val TASBIH_TODAY = intPreferencesKey("tasbih_today")
        val TASBIH_TOTAL = intPreferencesKey("tasbih_total")
        fun iqama(prayer: String) = intPreferencesKey("iqama_$prayer")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboarded = p[K.ONBOARDED] ?: false,
            cityNameAr = p[K.CITY_AR] ?: "",
            cityNameEn = p[K.CITY_EN] ?: "",
            country = p[K.COUNTRY] ?: "",
            lat = p[K.LAT] ?: 0.0,
            lng = p[K.LNG] ?: 0.0,
            timezone = p[K.TIMEZONE] ?: "Asia/Riyadh",
            sound = p[K.SOUND] ?: "",
            fajrSound = p[K.FAJR_SOUND] ?: "",
            method = p[K.METHOD] ?: "auto",
            madhab = p[K.MADHAB] ?: "auto",
            enabledPrayers = p[K.ENABLED] ?: AppSettings().enabledPrayers,
            vibrateOnly = p[K.VIBRATE_ONLY] ?: emptySet(),
            iqama = DEFAULT_IQAMA.mapValues { (k, v) -> p[K.iqama(k)] ?: v },
            favorites = p[K.FAVORITES] ?: emptySet(),
            nextAlarm = p[K.NEXT_ALARM] ?: 0L,
            reminderMinutes = p[K.REMINDER_MIN] ?: 10,
            remindFriday = p[K.REMIND_FRIDAY] ?: true,
            remindFasting = p[K.REMIND_FASTING] ?: true,
            mosqueName = p[K.MOSQUE] ?: "",
            tasbihDate = p[K.TASBIH_DATE] ?: "",
            tasbihToday = p[K.TASBIH_TODAY] ?: 0,
            tasbihTotal = p[K.TASBIH_TOTAL] ?: 0,
        )
    }

    suspend fun setCity(city: City) {
        context.dataStore.edit { p ->
            p[K.CITY_AR] = city.nameAr
            p[K.CITY_EN] = city.nameEn
            p[K.COUNTRY] = city.country
            p[K.LAT] = city.lat
            p[K.LNG] = city.lng
            p[K.TIMEZONE] = city.timezone
        }
    }

    suspend fun setSound(sound: String) = context.dataStore.edit { it[K.SOUND] = sound }
    suspend fun setFajrSound(sound: String) = context.dataStore.edit { it[K.FAJR_SOUND] = sound }
    suspend fun setMethod(method: String) = context.dataStore.edit { it[K.METHOD] = method }
    suspend fun setMadhab(madhab: String) = context.dataStore.edit { it[K.MADHAB] = madhab }
    suspend fun setOnboarded() = context.dataStore.edit { it[K.ONBOARDED] = true }
    suspend fun setNextAlarm(epochMillis: Long) = context.dataStore.edit { it[K.NEXT_ALARM] = epochMillis }
    suspend fun setReminderMinutes(minutes: Int) = context.dataStore.edit { it[K.REMINDER_MIN] = minutes.coerceIn(0, 60) }
    suspend fun setRemindFriday(on: Boolean) = context.dataStore.edit { it[K.REMIND_FRIDAY] = on }
    suspend fun setRemindFasting(on: Boolean) = context.dataStore.edit { it[K.REMIND_FASTING] = on }
    suspend fun setMosqueName(name: String) = context.dataStore.edit { it[K.MOSQUE] = name }

    suspend fun setPrayerEnabled(prayer: String, enabled: Boolean) {
        context.dataStore.edit { p ->
            val current = p[K.ENABLED] ?: AppSettings().enabledPrayers
            p[K.ENABLED] = if (enabled) current + prayer else current - prayer
        }
    }

    suspend fun setVibrateOnly(prayer: String, on: Boolean) {
        context.dataStore.edit { p ->
            val current = p[K.VIBRATE_ONLY] ?: emptySet()
            p[K.VIBRATE_ONLY] = if (on) current + prayer else current - prayer
        }
    }

    /** حفظ فواصل الإقامة لكل الصلوات دفعة واحدة */
    suspend fun setIqama(values: Map<String, Int>) {
        context.dataStore.edit { p ->
            values.forEach { (k, v) -> p[K.iqama(k)] = v.coerceIn(0, 60) }
        }
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { p ->
            val cur = p[K.FAVORITES] ?: emptySet()
            p[K.FAVORITES] = if (id in cur) cur - id else cur + id
        }
    }

    /** يضيف تسبيحة: يُصفَّر عدّاد اليوم تلقائياً عند تغيّر التاريخ */
    suspend fun addTasbih(todayKey: String, count: Int = 1) {
        context.dataStore.edit { p ->
            val sameDay = p[K.TASBIH_DATE] == todayKey
            p[K.TASBIH_DATE] = todayKey
            p[K.TASBIH_TODAY] = (if (sameDay) p[K.TASBIH_TODAY] ?: 0 else 0) + count
            p[K.TASBIH_TOTAL] = (p[K.TASBIH_TOTAL] ?: 0) + count
        }
    }

    suspend fun resetTasbihToday() = context.dataStore.edit { it[K.TASBIH_TODAY] = 0 }

    /* ───────── النسخ الاحتياطي: كل المفاتيح كـ JSON ───────── */

    suspend fun exportJson(): String {
        val json = JSONObject()
        context.dataStore.data.first().asMap().forEach { (key, value) ->
            json.put(key.name, if (value is Set<*>) JSONArray(value.toList()) else value)
        }
        return json.toString(2)
    }

    /** يستورد نسخة احتياطية؛ يتجاهل المفاتيح غير المعروفة ويرفض الملف إن لم يكن JSON صالحاً */
    suspend fun importJson(text: String) {
        val json = JSONObject(text)
        context.dataStore.edit { p ->
            json.keys().forEach { name ->
                when (val v = json.get(name)) {
                    is Boolean -> p[booleanPreferencesKey(name)] = v
                    is Int -> if (name == "next_alarm") p[longPreferencesKey(name)] = v.toLong() else p[intPreferencesKey(name)] = v
                    is Long -> p[longPreferencesKey(name)] = v
                    is Double -> p[doublePreferencesKey(name)] = v
                    is String -> p[stringPreferencesKey(name)] = v
                    is JSONArray -> p[stringSetPreferencesKey(name)] = (0 until v.length()).map { v.getString(it) }.toSet()
                }
            }
        }
    }
}
