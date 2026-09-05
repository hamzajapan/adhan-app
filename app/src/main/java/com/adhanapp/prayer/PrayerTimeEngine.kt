package com.adhanapp.prayer

import android.content.Context
import androidx.annotation.StringRes
import com.adhanapp.R
import com.adhanapp.cities.City
import com.adhanapp.settings.AppSettings
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class PrayerKey(@StringRes val nameRes: Int) {
    FAJR(R.string.prayer_fajr), DHUHR(R.string.prayer_dhuhr), ASR(R.string.prayer_asr),
    MAGHRIB(R.string.prayer_maghrib), ISHA(R.string.prayer_isha);

    fun label(context: Context): String = context.getString(nameRes)

    companion object {
        /** اسم الصلاة من مفتاحها المخزّن، أو "الصلاة" لقيمة غير معروفة (مثل TEST) */
        fun labelOf(context: Context, key: String): String =
            runCatching { valueOf(key).label(context) }.getOrDefault(context.getString(R.string.prayer_generic))
    }
}

data class NextPrayer(val key: PrayerKey, val time: Instant)

/** صف في جدول اليوم: صلاة أو الشروق (الشروق ليس له أذان) */
data class DayRow(@StringRes val nameRes: Int, val time: Instant, val key: PrayerKey?)

/** غلاف حول مكتبة batoulapps/adhan — حساب فلكي محلي بالكامل */
class PrayerTimeEngine(private val city: City, private val settings: AppSettings) {

    val zone: ZoneId = ZoneId.of(city.timezone)

    private val params: CalculationParameters
        get() {
            val key = if (settings.method == "auto") defaultMethod(city.country) else settings.method
            val p = when (key) {
                TUNISIA -> tunisiaParameters()
                TUNISIA_NAMAZVAKTI -> namazvaktiParameters()
                else -> CalculationMethod.valueOf(key).parameters
            }
            p.madhab = when (settings.madhab) {
                "auto" -> if (city.country in HANAFI_COUNTRIES) Madhab.HANAFI else Madhab.SHAFI
                "HANAFI" -> Madhab.HANAFI
                else -> Madhab.SHAFI
            }
            return p
        }

    private fun prayerTimes(date: LocalDate) = PrayerTimes(
        Coordinates(city.lat, city.lng),
        DateComponents(date.year, date.monthValue, date.dayOfMonth),
        params,
    )

    /** مواقيت الصلوات الخمس ليوم محدد (بتوقيت المدينة) */
    fun timesFor(date: LocalDate): Map<PrayerKey, Instant> {
        val pt = prayerTimes(date)
        return linkedMapOf(
            PrayerKey.FAJR to pt.fajr.toInstant(),
            PrayerKey.DHUHR to pt.dhuhr.toInstant(),
            PrayerKey.ASR to pt.asr.toInstant(),
            PrayerKey.MAGHRIB to pt.maghrib.toInstant(),
            PrayerKey.ISHA to pt.isha.toInstant(),
        )
    }

    /** جدول اليوم كاملاً مع الشروق — للعرض في الشاشة الرئيسية */
    fun dayRows(date: LocalDate): List<DayRow> {
        val pt = prayerTimes(date)
        return listOf(
            DayRow(R.string.prayer_fajr, pt.fajr.toInstant(), PrayerKey.FAJR),
            DayRow(R.string.prayer_sunrise, pt.sunrise.toInstant(), null),
            DayRow(R.string.prayer_dhuhr, pt.dhuhr.toInstant(), PrayerKey.DHUHR),
            DayRow(R.string.prayer_asr, pt.asr.toInstant(), PrayerKey.ASR),
            DayRow(R.string.prayer_maghrib, pt.maghrib.toInstant(), PrayerKey.MAGHRIB),
            DayRow(R.string.prayer_isha, pt.isha.toInstant(), PrayerKey.ISHA),
        )
    }

    /** أقرب صلاة مفعّلة بعد اللحظة المعطاة */
    fun nextPrayer(enabled: Set<String>, after: Instant = Instant.now()): NextPrayer {
        var date = ZonedDateTime.ofInstant(after, zone).toLocalDate()
        repeat(7) {
            for ((key, time) in timesFor(date)) {
                if (time.isAfter(after) && key.name in enabled) return NextPrayer(key, time)
            }
            date = date.plusDays(1)
        }
        // احتياط نظري: لا يُفترض الوصول هنا إلا إذا عطّل المستخدم كل الصلوات
        return NextPrayer(PrayerKey.FAJR, timesFor(date)[PrayerKey.FAJR]!!)
    }

    /** آخر صلاة (من الخمس) وقعت قبل اللحظة المعطاة أو عندها — لحالة "حان وقت الأذان" */
    fun previousPrayer(before: Instant = Instant.now()): NextPrayer {
        var date = ZonedDateTime.ofInstant(before, zone).toLocalDate()
        repeat(3) {
            for ((key, time) in timesFor(date).entries.toList().asReversed()) {
                if (!time.isAfter(before)) return NextPrayer(key, time)
            }
            date = date.minusDays(1)
        }
        return NextPrayer(PrayerKey.ISHA, timesFor(date)[PrayerKey.ISHA]!!)
    }

    companion object {
        private val HANAFI_COUNTRIES = setOf(
            "PK", "IN", "BD", "AF", "TR", "AZ", "UZ", "KZ", "KG", "TJ", "TM"
        )

        const val TUNISIA = "TUNISIA"
        const val TUNISIA_NAMAZVAKTI = "TUNISIA_NAMAZVAKTI"

        /**
         * توقيت تونس الرسمي (وزارة الشؤون الدينية كما ينشره tunisienumerique.com):
         * فجر 18° وعشاء 18° مع دقائق ثابتة (الفجر −1، الظهر +7، المغرب +3، العشاء +1)،
         * معايَر على جدول قفصة لسنة 2026 كاملة: الفرق لا يتجاوز دقيقة واحدة.
         */
        fun tunisiaParameters(): CalculationParameters =
            CalculationParameters(18.0, 18.0, CalculationMethod.OTHER).apply {
                methodAdjustments = PrayerAdjustments(-1, 0, 7, 0, 3, 1)
            }

        /**
         * توقيت namazvakti.co.uk (طريقة تركية بدقائق احتياط أكبر): فجر 18° وعشاء 17.5°
         * مع (الشروق −5، الظهر +9، العصر +9، المغرب +6، العشاء +6) — خيار بديل.
         */
        fun namazvaktiParameters(): CalculationParameters =
            CalculationParameters(18.0, 17.5, CalculationMethod.OTHER).apply {
                methodAdjustments = PrayerAdjustments(0, -5, 9, 9, 6, 6)
            }

        /** مفتاح طريقة الحساب يُستنتج من رمز البلد */
        fun defaultMethod(country: String): String = when (country) {
            "SA" -> CalculationMethod.UMM_AL_QURA.name
            "EG" -> CalculationMethod.EGYPTIAN.name
            "PK", "IN", "BD" -> CalculationMethod.KARACHI.name
            "TN" -> TUNISIA
            // لا توجد طريقة طهران في إصدار المكتبة الحالي — MWL البديل الأنسب
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.name
        }

        /** مفتاح الطريقة ← مورد الاسم المعروض */
        val METHODS: Map<String, Int> = linkedMapOf(
            "auto" to R.string.method_auto,
            TUNISIA to R.string.method_tunisia,
            TUNISIA_NAMAZVAKTI to R.string.method_tunisia_namazvakti,
            "MUSLIM_WORLD_LEAGUE" to R.string.method_mwl,
            "UMM_AL_QURA" to R.string.method_umm_al_qura,
            "EGYPTIAN" to R.string.method_egyptian,
            "KARACHI" to R.string.method_karachi,
            "NORTH_AMERICA" to R.string.method_isna,
            "DUBAI" to R.string.method_dubai,
            "QATAR" to R.string.method_qatar,
            "KUWAIT" to R.string.method_kuwait,
            "SINGAPORE" to R.string.method_singapore,
            "MOON_SIGHTING_COMMITTEE" to R.string.method_moonsighting,
        )

        val MADHABS: Map<String, Int> = linkedMapOf(
            "auto" to R.string.madhab_auto,
            "SHAFI" to R.string.madhab_shafi,
            "HANAFI" to R.string.madhab_hanafi,
        )

        fun methodLabel(context: Context, key: String): String = METHODS[key]?.let(context::getString) ?: key
        fun madhabLabel(context: Context, key: String): String = MADHABS[key]?.let(context::getString) ?: key
    }
}
