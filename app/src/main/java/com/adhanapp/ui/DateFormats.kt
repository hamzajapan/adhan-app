package com.adhanapp.ui

import android.content.Context
import com.adhanapp.R
import com.adhanapp.settings.LocaleHelper
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/** "الخميس، 24 ذو القعدة 1445 هـ" / "Thursday, 24 Dhu al-Qi'dah 1445 AH" */
fun hijriLine(context: Context, date: LocalDate): String {
    val h = HijrahChronology.INSTANCE.date(date)
    val months = context.resources.getStringArray(R.array.hijri_months)
    val day = date.format(DateTimeFormatter.ofPattern("EEEE", LocaleHelper.uiLocale(context)))
    val sep = if (LocaleHelper.isArabic(context)) "، " else ", "
    return "$day$sep${h.get(ChronoField.DAY_OF_MONTH)} ${months[h.get(ChronoField.MONTH_OF_YEAR) - 1]} " +
        "${h.get(ChronoField.YEAR)} ${context.getString(R.string.hijri_suffix)}"
}

/** "02 مايو 2024 م" / "02 May 2024" */
fun gregorianLine(context: Context, date: LocalDate): String {
    val text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", LocaleHelper.uiLocale(context)))
    val suffix = context.getString(R.string.gregorian_suffix)
    return if (suffix.isBlank()) text else "$text $suffix"
}

private val clock12 = DateTimeFormatter.ofPattern("hh:mm", Locale.US)

/** الساعة بصيغة 12 ساعة بأرقام لاتينية كما في التصميم */
fun formatClock(instant: Instant, zone: ZoneId): String = instant.atZone(zone).format(clock12)

/** "01:24:36" */
fun formatCountdown(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(Locale.US, total / 3600, (total % 3600) / 60, total % 60)
}
