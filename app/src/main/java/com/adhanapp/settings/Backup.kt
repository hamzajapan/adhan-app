package com.adhanapp.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.adhanapp.R
import java.io.File

/** تصدير/استيراد الإعدادات كملف JSON، ومشاركة أي ملف من مجلد cache/share عبر FileProvider */
object Backup {

    fun shareDir(context: Context): File = File(context.cacheDir, "share").apply { mkdirs() }

    fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    suspend fun export(context: Context, store: SettingsStore) {
        val file = File(shareDir(context), "adhan-backup.json")
        file.writeText(store.exportJson())
        shareFile(context, file, "application/json", context.getString(R.string.export_share_title))
    }

    /** يقرأ الملف المختار ويستورده؛ يفشل بوضوح إن لم يكن نسخة صالحة */
    suspend fun import(context: Context, store: SettingsStore, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException(context.getString(R.string.import_bad))
        store.importJson(text)
    }
}
