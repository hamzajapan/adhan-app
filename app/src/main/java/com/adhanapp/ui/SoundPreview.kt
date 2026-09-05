package com.adhanapp.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.adhanapp.sounds.AdhanDownloader

/**
 * معاينة صوتية لمقاطع الأذان (قناة وسائط عادية — ليست الأذان الفعلي).
 * يقبل رابط https للبث المباشر أو مسار ملف محلي منزَّل.
 */
object SoundPreview {
    private var player: MediaPlayer? = null
    var playingSource: String? = null
        private set

    fun play(
        context: Context,
        source: String,
        onPrepared: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: () -> Unit = {},
    ) {
        stop()
        playingSource = source
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener {
                it.start()
                onPrepared()
            }
            setOnCompletionListener {
                this@SoundPreview.stop()
                onDone()
            }
            setOnErrorListener { _, _, _ ->
                this@SoundPreview.stop()
                onError()
                true
            }
            runCatching {
                val uri = Uri.parse(source)
                // خادم إسلام ويب يعيد التوجيه إلى صفحة HTML إن غابت ترويسة Referer
                if (source.startsWith("http")) setDataSource(context, uri, AdhanDownloader.HEADERS)
                else setDataSource(context, uri)
                prepareAsync()
            }.onFailure {
                this@SoundPreview.stop()
                onError()
            }
        }
    }

    fun stop() {
        player?.runCatching { release() }
        player = null
        playingSource = null
    }
}
