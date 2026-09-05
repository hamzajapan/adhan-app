package com.adhanapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.prayer.PrayerTimeEngine
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.Backup
import com.adhanapp.settings.LocaleHelper
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

private class MonthLine(val date: LocalDate, val hijri: String, val times: List<String>, val today: Boolean)

/** الإمساكية الشهرية: جدول الشهر كاملاً مع الهجري، وتصدير كصورة للمشاركة */
@Composable
fun MonthScreen(settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = remember(settings) { PrayerTimeEngine(settings.toCity(), settings) }
    val today = remember { LocalDate.now(engine.zone) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val locale = remember { LocaleHelper.uiLocale(context) }
    val monthTitle = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val dayShort = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val lines = remember(month, engine) { buildMonth(month, engine, today, dayShort) }
    val hijriRange = remember(lines) { hijriMonths(context, lines) }
    val cityName = settings.cityName(LocaleHelper.isArabic(context))
    val headers = listOf(R.string.month_col_day, R.string.month_col_hijri, R.string.prayer_fajr, R.string.prayer_sunrise,
        R.string.prayer_dhuhr, R.string.prayer_asr, R.string.prayer_maghrib, R.string.prayer_isha).map { stringResource(it) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(
            title = stringResource(R.string.quick_month),
            subtitle = cityName,
            rightItem = {
                TopIcon(Icons.Outlined.Share, {
                    val file = renderImage(context, context.getString(R.string.month_image_title, cityName), month.format(monthTitle), hijriRange, headers, lines)
                    Backup.shareFile(context, file, "image/png", context.getString(R.string.month_share_title, cityName, month.format(monthTitle)))
                }, tint = AppColors.Gold)
            },
            leftItem = { Chevron(pointsLeft = true, onClick = onBack) },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Chevron(pointsLeft = false, color = AppColors.Gold, onClick = { month = month.minusMonths(1) })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(month.format(monthTitle), style = MaterialTheme.typography.titleLarge, color = AppColors.Text)
                Text(hijriRange, style = MaterialTheme.typography.bodySmall, color = AppColors.Gold)
            }
            Chevron(pointsLeft = true, color = AppColors.Gold, onClick = { month = month.plusMonths(1) })
        }
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.padding(horizontal = 12.dp).fillMaxWidth().weight(1f), radius = 18.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)) {
                headers.forEachIndexed { i, h ->
                    Text(
                        h, color = AppColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(if (i == 0) 1.4f else 1f),
                    )
                }
            }
            HorizontalDivider(color = AppColors.Gold.copy(alpha = 0.4f))
            LazyColumn {
                items(lines, key = { it.date.toEpochDay() }) { line ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (line.today) AppColors.HighlightRow else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                    ) {
                        val color = if (line.today) AppColors.ActiveTab else AppColors.Text
                        Text(line.date.format(dayShort), color = color, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f))
                        Text(line.hijri, color = AppColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        line.times.forEach { t ->
                            Text(t, color = color, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(color = AppColors.CardBorder)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun buildMonth(month: YearMonth, engine: PrayerTimeEngine, today: LocalDate, dayShort: DateTimeFormatter): List<MonthLine> =
    (1..month.lengthOfMonth()).map { d ->
        val date = month.atDay(d)
        MonthLine(
            date = date,
            hijri = HijrahChronology.INSTANCE.date(date).get(ChronoField.DAY_OF_MONTH).toString(),
            times = engine.dayRows(date).map { formatClock(it.time, engine.zone) },
            today = date == today,
        )
    }

/** "ربيع الأول – ربيع الآخر 1448" بحسب الشهور الهجرية التي يغطيها الشهر الميلادي */
private fun hijriMonths(context: Context, lines: List<MonthLine>): String {
    if (lines.isEmpty()) return ""
    val names = context.resources.getStringArray(R.array.hijri_months)
    fun label(date: LocalDate): Pair<String, Int> {
        val h = HijrahChronology.INSTANCE.date(date)
        return names[h.get(ChronoField.MONTH_OF_YEAR) - 1] to h.get(ChronoField.YEAR)
    }
    val (m1, y1) = label(lines.first().date)
    val (m2, y2) = label(lines.last().date)
    return if (m1 == m2) "$m1 $y1" else if (y1 == y2) "$m1 – $m2 $y1" else "$m1 $y1 – $m2 $y2"
}

/** يرسم الإمساكية صورةً بهوية التطبيق (1080 عرضاً) ويحفظها في مجلد المشاركة */
private fun renderImage(
    context: Context,
    title: String,
    monthLabel: String,
    hijri: String,
    headers: List<String>,
    lines: List<MonthLine>,
): File {
    val w = 1080
    val rowH = 46
    val top = 190
    val h = top + (lines.size + 1) * rowH + 60
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(AColor.parseColor("#07231F"))
    val gold = AColor.parseColor("#D9B55C")
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.parseColor("#EDEBE6"); textAlign = Paint.Align.CENTER; textSize = 26f }
    val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
    val goldP = Paint(bold).apply { color = gold }
    val line = Paint().apply { color = AColor.parseColor("#1B3B34"); strokeWidth = 1f }

    goldP.textSize = 40f
    c.drawText(title, w / 2f, 70f, goldP)
    text.textSize = 30f
    c.drawText("$monthLabel  •  $hijri", w / 2f, 120f, text)
    text.textSize = 26f

    // في العربية تُرتَّب الأعمدة من اليمين إلى اليسار
    val rtl = LocaleHelper.isArabic(context)
    val weights = listOf(1.4f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
    val total = weights.sum()
    fun colCenter(i: Int): Float {
        val before = weights.take(i).sum()
        val x = 30f + (before + weights[i] / 2f) / total * (w - 60f)
        return if (rtl) w - x else x
    }
    var y = top.toFloat()
    goldP.textSize = 26f
    headers.forEachIndexed { i, hd -> c.drawText(hd, colCenter(i), y, goldP) }
    y += rowH
    val dayShort = DateTimeFormatter.ofPattern("EEE d", LocaleHelper.uiLocale(context))
    lines.forEach { l ->
        if (l.today) c.drawRect(20f, y - 32f, w - 20f, y + 12f, Paint().apply { color = AColor.parseColor("#0E3A2C") })
        val p = if (l.today) goldP else text
        c.drawText(l.date.format(dayShort), colCenter(0), y, p)
        c.drawText(l.hijri, colCenter(1), y, p)
        l.times.forEachIndexed { i, t -> c.drawText(t, colCenter(2 + i), y, p) }
        c.drawLine(30f, y + 14f, w - 30f, y + 14f, line)
        y += rowH
    }
    val file = File(Backup.shareDir(context), "imsakiya.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    return file
}
