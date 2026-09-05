package com.adhanapp.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * لغة التطبيق: "system" أو "ar"/"fr"/"en". تُحفظ في SharedPreferences (قراءة متزامنة)
 * لأنها مطلوبة في attachBaseContext قبل أي شيء آخر، وتُطبَّق أيضاً على سياقات الخدمة والمستقبل والويدجت.
 */
object LocaleHelper {
    const val SYSTEM = "system"
    val OPTIONS = listOf(SYSTEM, "ar", "fr", "en")

    private const val PREFS = "app_locale"
    private const val KEY = "lang"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** سياق بلغة التطبيق المختارة (أو السياق نفسه إن كانت لغة النظام) */
    fun wrap(context: Context): Context {
        val lang = get(context)
        if (lang == SYSTEM) return context
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }

    /** هل لغة الواجهة الحالية عربية؟ (تحدد اسم المدينة المعروض وأرقام التاريخ) */
    fun isArabic(context: Context): Boolean = context.resources.configuration.locales[0].language == "ar"

    /** لغة الواجهة الحالية بأرقام لاتينية دائماً (كما في التصميم) */
    fun uiLocale(context: Context): Locale {
        val base = context.resources.configuration.locales[0]
        return Locale.forLanguageTag("${base.toLanguageTag()}-u-nu-latn")
    }

    fun label(context: Context, lang: String): String = when (lang) {
        "ar" -> "العربية"
        "fr" -> "Français"
        "en" -> "English"
        else -> context.getString(com.adhanapp.R.string.language_system)
    }
}
