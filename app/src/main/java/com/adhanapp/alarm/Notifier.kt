package com.adhanapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.adhanapp.MainActivity
import com.adhanapp.R
import com.adhanapp.settings.LocaleHelper

/** الإشعارات غير الأذان: التذكير قبل الأذان، وضع الاهتزاز، الجمعة والصيام والمناسبات */
object Notifier {
    private const val CH_REMINDER = "reminder"
    private const val CH_INFO = "info"
    private const val ID_REMINDER = 10
    private const val ID_VIBRATE = 11
    private const val ID_INFO = 12

    private fun channels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, context.getString(R.string.channel_reminder), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_INFO, context.getString(R.string.channel_info), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 3, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun post(rawContext: Context, channel: String, id: Int, title: String, text: String) {
        val context = LocaleHelper.wrap(rawContext)
        channels(context)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(if (channel == CH_REMINDER) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(id, n) }
    }

    fun preAdhan(rawContext: Context, prayerKey: String, minutes: Int, time: String) {
        val c = LocaleHelper.wrap(rawContext)
        val name = com.adhanapp.prayer.PrayerKey.labelOf(c, prayerKey)
        post(c, CH_REMINDER, ID_REMINDER, c.getString(R.string.notif_pre_title, name, minutes), c.getString(R.string.notif_pre_text, time))
    }

    /** بديل الأذان في وضع الاهتزاز: إشعار + اهتزاز مطوّل دون صوت */
    fun vibrateOnly(rawContext: Context, prayerKey: String) {
        val c = LocaleHelper.wrap(rawContext)
        val name = com.adhanapp.prayer.PrayerKey.labelOf(c, prayerKey)
        post(c, CH_REMINDER, ID_VIBRATE, c.getString(R.string.notif_adhan_time_title, name), c.getString(R.string.notif_vibrate_text))
        vibrate(c, longArrayOf(0, 600, 300, 600, 300, 600))
    }

    fun info(context: Context, title: String, text: String) = post(context, CH_INFO, ID_INFO, title, text)

    fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        runCatching { vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }
}
