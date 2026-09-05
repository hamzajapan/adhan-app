package com.adhanapp.sounds

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** تنزيل الأذان المعتمد إلى تخزين التطبيق الخاص ليُشغَّل لاحقاً دون إنترنت */
object AdhanDownloader {

    /**
     * ترويسات لازمة لخادم إسلام ويب: بدون Referer يعيد التوجيه إلى صفحة HTML
     * بدل ملف mp3 (تُستخدم في البث والتنزيل معاً).
     */
    val HEADERS: Map<String, String> = mapOf(
        "Referer" to "https://audio.islamweb.net/audio/",
        "User-Agent" to "Mozilla/5.0 (Linux; Android) AdhanApp",
    )

    private fun dir(context: Context): File = File(context.filesDir, "adhan").apply { mkdirs() }

    fun fileFor(context: Context, id: Int): File = File(dir(context), "$id.mp3")

    fun isDownloaded(context: Context, id: Int): Boolean = fileFor(context, id).length() > 0

    /**
     * ينزّل الملف إلى ملف مؤقت ثم يعيد تسميته حتى لا يبقى ملف ناقص عند الانقطاع.
     * onProgress: نسبة من 0 إلى 1، أو -1 إذا كان الحجم مجهولاً.
     */
    suspend fun download(context: Context, entry: AdhanEntry, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, entry.id)
            if (target.length() > 0) return@withContext target
            val tmp = File(target.path + ".part")
            val conn = (URL(entry.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                HEADERS.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            try {
                if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
                // إن أعاد الخادم صفحة بدل الصوت فهذا فشل وليس ملفاً صالحاً
                if (conn.contentType?.startsWith("text/") == true) throw IOException("not audio: ${conn.contentType}")
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            onProgress(if (total > 0) done.toFloat() / total else -1f)
                        }
                    }
                }
                if (tmp.length() == 0L) throw IOException("empty file")
                if (!tmp.renameTo(target)) throw IOException("rename failed")
                target
            } catch (e: Exception) {
                tmp.delete()
                throw e
            } finally {
                conn.disconnect()
            }
        }

    /** يحذف كل الملفات المنزّلة ما عدا الأذانات المعتمدة (العام والفجر) */
    fun keepOnly(context: Context, ids: Set<Int>) {
        val keep = ids.map { "$it.mp3" }.toSet()
        dir(context).listFiles()?.forEach { f -> if (f.name !in keep) f.delete() }
    }
}
