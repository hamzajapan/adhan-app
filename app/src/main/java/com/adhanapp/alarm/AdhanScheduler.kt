package com.adhanapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adhanapp.MainActivity
import com.adhanapp.R
import com.adhanapp.prayer.PrayerTimeEngine
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import com.adhanapp.ui.formatClock
import com.adhanapp.widget.PrayerWidget
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * جدولة الأذان بنفس آلية تطبيق المنبه: setAlarmClock.
 * لا يُجدَّل إلا أذان واحد في كل مرة (تسلسل) — بعد انطلاقه يُجدَّل الذي يليه.
 * معه: تذكير قبل الأذان، وفحص ليلي يومي كشبكة أمان، وتحديث الويدجت.
 */
object AdhanScheduler {

    const val ACTION_ADHAN = "com.adhanapp.action.ADHAN"
    const val ACTION_REMINDER = "com.adhanapp.action.REMINDER"
    const val ACTION_DAILY = "com.adhanapp.action.DAILY"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_TIME = "time"
    const val TEST_PRAYER = "TEST"
    private const val REQUEST_ADHAN = 0
    private const val REQUEST_TEST = 1
    private const val REQUEST_REMINDER = 2
    private const val REQUEST_DAILY = 3

    /** موعد الفحص الليلي اليومي (بتوقيت الجهاز) */
    private val DAILY_AT: LocalTime = LocalTime.of(20, 0)

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /** إعادة حساب الصلاة التالية المفعّلة وجدولتها (مع التذكير والويدجت والفحص الليلي) */
    suspend fun scheduleNext(context: Context) {
        val store = SettingsStore(context)
        val settings = store.settings.first()
        if (!settings.onboarded || settings.enabledPrayers.isEmpty()) {
            cancel(context)
            store.setNextAlarm(0L)
            return
        }
        val engine = PrayerTimeEngine(settings.toCity(), settings)
        val next = engine.nextPrayer(enabled = settings.enabledPrayers)
        val at = next.time.toEpochMilli()
        schedule(context, ACTION_ADHAN, at, REQUEST_ADHAN) { putExtra(EXTRA_PRAYER, next.key.name) }
        if (settings.nextAlarm != at) {
            val ctx = LocaleHelper.wrap(context)
            AdhanLog.append(context, ctx.getString(R.string.log_scheduled, next.key.label(ctx), formatClock(next.time, engine.zone)))
        }
        store.setNextAlarm(at)

        scheduleReminder(context, settings.reminderMinutes, next.key.name, at, formatClock(next.time, engine.zone))
        scheduleDaily(context)
        PrayerWidget.update(context, settings, engine)
    }

    /** prayerKey هو مفتاح الصلاة (FAJR…) ويُترجم إلى اسم عند إطلاق التذكير بلغة التطبيق حينها */
    private fun scheduleReminder(context: Context, minutes: Int, prayerKey: String, adhanAt: Long, time: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = broadcastIntent(context, ACTION_REMINDER, REQUEST_REMINDER) {}
        am.cancel(intent)
        if (minutes <= 0) return
        val at = adhanAt - Duration.ofMinutes(minutes.toLong()).toMillis()
        if (at <= System.currentTimeMillis()) return
        val reminder = broadcastIntent(context, ACTION_REMINDER, REQUEST_REMINDER) {
            putExtra(EXTRA_PRAYER, prayerKey)
            putExtra(EXTRA_MINUTES, minutes)
            putExtra(EXTRA_TIME, time)
        }
        if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, reminder)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, reminder)
    }

    /** فحص ليلي غير دقيق: إعادة جدولة + تذكيرات الغد (الجمعة/الصيام) */
    fun scheduleDaily(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var next = ZonedDateTime.now(ZoneId.systemDefault()).with(DAILY_AT)
        if (!next.isAfter(ZonedDateTime.now())) next = next.plusDays(1)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(),
            broadcastIntent(context, ACTION_DAILY, REQUEST_DAILY) {},
        )
    }

    /** تنبيه تجريبي بعد ثوانٍ يمر بالمسار نفسه: AlarmManager ← المستقبل ← الخدمة */
    fun scheduleTest(context: Context, delaySeconds: Int) {
        schedule(context, ACTION_ADHAN, System.currentTimeMillis() + delaySeconds * 1000L, REQUEST_TEST) {
            putExtra(EXTRA_PRAYER, TEST_PRAYER)
        }
    }

    private fun broadcastIntent(context: Context, action: String, requestCode: Int, extras: Intent.() -> Unit): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AdhanReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun schedule(context: Context, action: String, atMillis: Long, requestCode: Int, extras: Intent.() -> Unit) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = broadcastIntent(context, action, requestCode, extras)
        if (canScheduleExact(context)) {
            // الضغط على أيقونة المنبه في شريط الحالة يفتح التطبيق
            val showIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent), alarmIntent)
        } else {
            // احتياط: قد يتأخر دقائق لكنه أفضل من ألا ينطلق الأذان أبداً
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(broadcastIntent(context, ACTION_ADHAN, REQUEST_ADHAN) {})
        am.cancel(broadcastIntent(context, ACTION_REMINDER, REQUEST_REMINDER) {})
    }
}
