package com.adhanapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.adhanapp.R
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import com.adhanapp.sounds.AdhanDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * خدمة أمامية من نوع mediaPlayback: تشغّل الأذان كاملاً من أوله لآخره
 * عبر قناة USAGE_ALARM التي تخرق وضع الصامت.
 */
class AdhanService : Service() {

    companion object {
        const val ACTION_PLAY = "com.adhanapp.action.PLAY"
        const val ACTION_STOP = "com.adhanapp.action.STOP"
        const val EXTRA_PRAYER = "prayer"
        const val ACTION_ADHAN_DONE = "com.adhanapp.action.ADHAN_DONE"
        private const val CHANNEL_ID = "adhan"
        private const val NOTIF_ID = 1
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAdhan()
            ACTION_PLAY -> {
                val prayer = intent.getStringExtra(EXTRA_PRAYER)
                if (prayer != null) startAdhan(prayer)
            }
        }
        return START_STICKY
    }

    private fun startAdhan(prayer: String) {
        val ctx = LocaleHelper.wrap(this)
        createChannel(ctx)
        val prayerName = PrayerKey.labelOf(ctx, prayer)

        // إشعار بشاشة كاملة فوق قفل الشاشة
        val fullScreen = PendingIntent.getActivity(
            this, 1,
            Intent(this, AdhanActivity::class.java).apply {
                putExtra(EXTRA_PRAYER, prayer)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.notif_adhan_time_title, prayerName))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        // WakeLock جزئي يضمن استمرار التشغيل والشاشة مطفأة
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "adhanapp:adhan").apply {
            acquire(10 * 60 * 1000L) // سقف 10 دقائق
        }

        scope.launch {
            val sound = SettingsStore(this@AdhanService).settings.first().soundFor(prayer)
            AdhanLog.append(this@AdhanService, ctx.getString(R.string.log_played, prayerName))
            play(sound)
        }
    }

    /**
     * يشغّل الأذان المعتمد (ملف منزَّل من إسلام ويب في تخزين التطبيق)،
     * وإن لم يُعتمد أذان بعد أو فُقد الملف فنغمة المنبه الافتراضية للنظام.
     */
    private fun play(soundId: String) {
        releasePlayer()
        val downloaded = soundId.toIntOrNull()
            ?.let { AdhanDownloader.fileFor(this, it) }
            ?.takeIf { it.length() > 0 }
        val uri = when {
            downloaded != null -> Uri.fromFile(downloaded)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
        if (uri == null) {
            stopAdhan()
            return
        }
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnCompletionListener { stopAdhan() }
            setOnErrorListener { _, _, _ ->
                stopAdhan()
                true
            }
        }
        player = mp
        runCatching {
            mp.setDataSource(this, uri)
            mp.prepare()
            mp.start()
        }.onFailure { stopAdhan() }
    }

    private fun releasePlayer() {
        player?.runCatching { release() }
        player = null
    }

    private fun stopAdhan() {
        releasePlayer()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        // إعلام شاشة الأذان لإغلاق نفسها
        sendBroadcast(Intent(ACTION_ADHAN_DONE).setPackage(packageName))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        wakeLock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.channel_adhan),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = ctx.getString(R.string.channel_adhan_desc)
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
