package com.adhanapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.adhanapp.MainActivity
import com.adhanapp.R
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.prayer.PrayerTimeEngine
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import com.adhanapp.ui.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime

/** ويدجت الشاشة الرئيسية: الصلاة القادمة ومواقيت اليوم بهوية التطبيق */
class PrayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context).settings.first()
                if (settings.onboarded) update(context, settings, PrayerTimeEngine(settings.toCity(), settings))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val NAME_IDS = intArrayOf(R.id.tv_p1, R.id.tv_p2, R.id.tv_p3, R.id.tv_p4, R.id.tv_p5)
        private val TIME_IDS = intArrayOf(R.id.tv_t1, R.id.tv_t2, R.id.tv_t3, R.id.tv_t4, R.id.tv_t5)

        /** يُستدعى من المجدول بعد كل جدولة، ومن الويدجت نفسه كل 30 دقيقة */
        fun update(rawContext: Context, settings: AppSettings, engine: PrayerTimeEngine) {
            val context = LocaleHelper.wrap(rawContext)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
            if (ids.isEmpty()) return

            val now = Instant.now()
            val next = engine.nextPrayer(settings.enabledPrayers, now)
            val today = ZonedDateTime.ofInstant(now, engine.zone).toLocalDate()
            val times = engine.timesFor(today)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer).apply {
                setTextViewText(R.id.tv_city, settings.cityName(LocaleHelper.isArabic(context)))
                setTextViewText(R.id.tv_next, "${next.key.label(context)}  ${formatClock(next.time, engine.zone)}")
                setTextViewText(R.id.tv_next_label, context.getString(R.string.widget_next_label))
                PrayerKey.entries.forEachIndexed { i, key ->
                    setTextViewText(NAME_IDS[i], key.label(context))
                    setTextViewText(TIME_IDS[i], times[key]?.let { formatClock(it, engine.zone) } ?: "--:--")
                    setTextColor(TIME_IDS[i], if (key == next.key) 0xFFA3C95A.toInt() else 0xFFEDEBE6.toInt())
                }
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context, 4, Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
