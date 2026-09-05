package com.adhanapp.alarm

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** سجل بسيط لآخر أحداث الأذان (جُدول/انطلق/اهتزاز/تذكير) لتشخيص مشاكل الأجهزة */
object AdhanLog {
    private const val MAX_LINES = 60
    private val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")

    private fun file(context: Context) = File(context.filesDir, "adhan_log.txt")

    @Synchronized
    fun append(context: Context, event: String) {
        val f = file(context)
        val stamp = Instant.now().atZone(ZoneId.systemDefault()).format(fmt)
        val lines = (if (f.exists()) f.readLines() else emptyList()) + "$stamp  $event"
        f.writeText(lines.takeLast(MAX_LINES).joinToString("\n"))
    }

    fun read(context: Context): List<String> {
        val f = file(context)
        return if (f.exists()) f.readLines().asReversed() else emptyList()
    }
}
