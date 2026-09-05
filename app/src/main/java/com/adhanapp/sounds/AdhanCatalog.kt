package com.adhanapp.sounds

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** أذان واحد من فهرس إسلام ويب */
data class AdhanEntry(
    val id: Int,
    val reader: String,
    val kind: String,
    val place: String,
    val url: String,
) {
    /** "أذان - الحرم المكي - مكة المكرمة" أو "أذان" فقط */
    val subtitle: String get() = if (place.isBlank()) kind else "$kind - $place"
}

/**
 * فهرس أصوات الأذان المولَّد من صفحة الأذان في إسلام ويب.
 * النسخة المضمّنة في assets تُولَّد بالسكربت scripts/generate_adhan_catalog.py،
 * ويمكن تحديثها من داخل التطبيق فتُحفظ في تخزين التطبيق وتُفضَّل على المضمّنة.
 */
class AdhanCatalog(private val context: Context) {

    var entries: List<AdhanEntry> = load()
        private set

    private fun cacheFile() = File(context.filesDir, "adhan_catalog.json")

    private fun load(): List<AdhanEntry> {
        val cached = cacheFile()
        val json = if (cached.length() > 0) cached.readText()
        else context.assets.open("adhan_catalog.json").bufferedReader().use { it.readText() }
        return runCatching { parse(json) }.getOrElse {
            cached.delete()
            parse(context.assets.open("adhan_catalog.json").bufferedReader().use { r -> r.readText() })
        }
    }

    fun byId(id: Int): AdhanEntry? = entries.firstOrNull { it.id == id }

    /** بحث فوري باسم المؤذن أو المكان */
    fun search(query: String): List<AdhanEntry> {
        val q = query.trim()
        if (q.isEmpty()) return entries
        return entries.filter { it.reader.contains(q) || it.place.contains(q) || it.kind.contains(q) }
    }

    /** يجلب صفحة إسلام ويب ويعيد بناء الفهرس؛ يعيد عدد العناصر الجديدة */
    suspend fun refresh(): Int = withContext(Dispatchers.IO) {
        val conn = (URL(SOURCE).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        val page = try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val fresh = parsePage(page)
        if (fresh.size < 50) throw IOException("الصفحة لا تحوي فهرساً صالحاً")
        cacheFile().writeText(toJson(fresh))
        entries = fresh
        fresh.size
    }

    companion object {
        const val SOURCE = "https://audio.islamweb.net/audio/index.php?page=AudioGroup&Gtype=1"

        private fun parse(json: String): List<AdhanEntry> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AdhanEntry(o.getInt("id"), o.getString("reader"), o.getString("kind"), o.getString("place"), o.getString("url"))
            }
        }

        private fun toJson(list: List<AdhanEntry>): String = JSONArray().also { arr ->
            list.forEach { e ->
                arr.put(JSONObject().put("id", e.id).put("reader", e.reader).put("kind", e.kind).put("place", e.place).put("url", e.url))
            }
        }.toString()

        private val READER_BLOCK = Regex("""<div class="rwayabar[^"]*">""")
        private val READER_NAME = Regex("""<h1><a[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        private val ITEM = Regex("""data-mp3="([^"]+)"[^>]*/>.*?audioid=(\d+)">(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        private val TAG = Regex("<[^>]+>")

        /** نفس منطق سكربت التوليد: كتلة لكل مؤذن وداخلها عناصر بروابط mp3 مباشرة */
        fun parsePage(page: String): List<AdhanEntry> =
            page.split(READER_BLOCK).drop(1).flatMap { block ->
                val reader = READER_NAME.find(block)?.groupValues?.get(1)?.replace(TAG, "")?.trim() ?: "غير معروف"
                ITEM.findAll(block).map { m ->
                    val label = m.groupValues[3].replace(TAG, "").replace("&amp;", "&").trim()
                    val parts = label.split(",").map { it.trim() }
                    AdhanEntry(
                        id = m.groupValues[2].toInt(),
                        reader = reader,
                        kind = parts.first(),
                        place = parts.drop(1).joinToString(" - "),
                        url = m.groupValues[1],
                    )
                }.toList()
            }
    }
}
