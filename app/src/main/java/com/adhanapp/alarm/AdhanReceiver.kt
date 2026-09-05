package com.adhanapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.adhanapp.MainActivity
import com.adhanapp.R
import com.adhanapp.prayer.IslamicEvents
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * يستقبل انطلاق الأذان والتذكير والفحص الليلي، وأحداث النظام التي تستدعي إعادة الجدولة:
 * إقلاع الجهاز، تغيّر الوقت، تغيّر المنطقة الزمنية، تحديث التطبيق.
 */
class AdhanReceiver : BroadcastReceiver() {

    override fun onReceive(rawContext: Context, intent: Intent) {
        val context = LocaleHelper.wrap(rawContext)
        when (intent.action) {
            AdhanScheduler.ACTION_ADHAN -> {
                val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER) ?: return
                async(context) {
                    val settings = SettingsStore(context).settings.first()
                    if (prayer in settings.vibrateOnly) {
                        Notifier.vibrateOnly(context, prayer)
                        AdhanLog.append(context, context.getString(R.string.log_vibrate, PrayerKey.labelOf(context, prayer)))
                    } else {
                        startAdhan(context, prayer)
                    }
                    // جدولة الصلاة التالية فوراً (تسلسل)
                    AdhanScheduler.scheduleNext(context)
                }
            }
            AdhanScheduler.ACTION_REMINDER -> {
                val prayer = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER) ?: return
                Notifier.preAdhan(
                    context, prayer,
                    intent.getIntExtra(AdhanScheduler.EXTRA_MINUTES, 10),
                    intent.getStringExtra(AdhanScheduler.EXTRA_TIME) ?: "",
                )
                AdhanLog.append(context, context.getString(R.string.log_reminder, PrayerKey.labelOf(context, prayer)))
            }
            AdhanScheduler.ACTION_DAILY -> async(context) {
                dailyReminders(context)
                AdhanScheduler.scheduleNext(context)
                AdhanLog.append(context, context.getString(R.string.log_daily))
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> async(context) { AdhanScheduler.scheduleNext(context) }
        }
    }

    /** تذكيرات الغد: الجمعة (الكهف والدعاء) وصيام الاثنين والخميس والأيام البيض والمناسبات */
    private suspend fun dailyReminders(context: Context) {
        val settings = SettingsStore(context).settings.first()
        val tomorrow = LocalDate.now().plusDays(1)
        if (settings.remindFriday && tomorrow.dayOfWeek == DayOfWeek.FRIDAY) {
            Notifier.info(context, context.getString(R.string.friday_title), context.getString(R.string.friday_text))
        }
        if (settings.remindFasting && !IslamicEvents.isRamadan(tomorrow)) {
            val why = when {
                IslamicEvents.isWhiteDay(tomorrow) -> R.string.fasting_white
                tomorrow.dayOfWeek == DayOfWeek.MONDAY -> R.string.fasting_monday
                tomorrow.dayOfWeek == DayOfWeek.THURSDAY -> R.string.fasting_thursday
                else -> null
            }
            if (why != null) {
                Notifier.info(context, context.getString(R.string.fasting_title), context.getString(R.string.fasting_text, context.getString(why)))
            }
        }
        val event = IslamicEvents.next(tomorrow)
        if (event.daysLeft in 0L..1L) {
            val name = context.getString(event.nameRes)
            Notifier.info(context, name, context.getString(R.string.event_notif_text, name))
        }
    }

    private fun startAdhan(context: Context, prayer: String) {
        val service = Intent(context, AdhanService::class.java).apply {
            action = AdhanService.ACTION_PLAY
            putExtra(AdhanService.EXTRA_PRAYER, prayer)
        }
        try {
            ContextCompat.startForegroundService(context, service)
        } catch (e: Exception) {
            // بعض الأنظمة (خاصة MIUI) تمنع بدء الخدمة من الخلفية — إشعار بنغمة المنبه بدل الصمت
            AdhanLog.append(context, context.getString(R.string.log_blocked, e.javaClass.simpleName))
            fallbackNotification(context, prayer)
        }
    }

    private fun fallbackNotification(context: Context, prayer: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channelId = "adhan_fallback"
        nm.createNotificationChannel(
            NotificationChannel(channelId, context.getString(R.string.channel_fallback), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
                setBypassDnd(true)
            },
        )
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            2,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_adhan_time_title, PrayerKey.labelOf(context, prayer)))
                .setContentText(context.getString(R.string.notif_fallback_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun async(context: Context, block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } catch (e: Exception) {
                AdhanLog.append(context, context.getString(R.string.log_error, e.javaClass.simpleName))
            } finally {
                pending.finish()
            }
        }
    }
}
