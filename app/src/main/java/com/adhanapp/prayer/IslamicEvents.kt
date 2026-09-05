package com.adhanapp.prayer

import androidx.annotation.StringRes
import com.adhanapp.R
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

data class IslamicEvent(@StringRes val nameRes: Int, val date: LocalDate, val daysLeft: Long)

/** المناسبات الإسلامية الثابتة في التقويم الهجري (أم القرى) */
object IslamicEvents {

    private val EVENTS = listOf(
        Triple(1, 1, R.string.event_hijri_new_year),
        Triple(1, 10, R.string.event_ashura),
        Triple(9, 1, R.string.event_ramadan),
        Triple(10, 1, R.string.event_eid_fitr),
        Triple(12, 9, R.string.event_arafah),
        Triple(12, 10, R.string.event_eid_adha),
    )

    /** أقرب مناسبة قادمة (اليوم نفسه يُعدّ قادماً بصفر أيام) */
    fun next(today: LocalDate): IslamicEvent {
        val hijriYear = HijrahChronology.INSTANCE.date(today).get(ChronoField.YEAR)
        return EVENTS.flatMap { (m, d, res) ->
            listOf(hijriYear, hijriYear + 1).mapNotNull { y ->
                runCatching { LocalDate.from(HijrahDate.of(y, m, d)) }.getOrNull()?.let { res to it }
            }
        }.filter { !it.second.isBefore(today) }
            .minByOrNull { it.second }!!
            .let { (res, date) -> IslamicEvent(res, date, ChronoUnit.DAYS.between(today, date)) }
    }

    /** الأيام البيض 13–15 من كل شهر هجري */
    fun isWhiteDay(date: LocalDate): Boolean =
        HijrahChronology.INSTANCE.date(date).get(ChronoField.DAY_OF_MONTH) in 13..15

    fun isRamadan(date: LocalDate): Boolean =
        HijrahChronology.INSTANCE.date(date).get(ChronoField.MONTH_OF_YEAR) == 9
}
