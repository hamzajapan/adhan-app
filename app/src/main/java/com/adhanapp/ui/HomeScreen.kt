package com.adhanapp.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.alarm.AdhanScheduler
import com.adhanapp.alarm.AdhanService
import com.adhanapp.openExactAlarmSettings
import com.adhanapp.prayer.DayRow
import com.adhanapp.prayer.IslamicEvents
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.prayer.PrayerTimeEngine
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * الشاشة الرئيسية: العدّاد الدائري + مواقيت اليوم،
 * وتتحول تلقائياً إلى حالة "حان وقت الأذان" أثناء فترة الإقامة.
 */
@Composable
fun HomeScreen(
    settings: AppSettings,
    store: SettingsStore,
    contentPadding: PaddingValues,
    onMenu: () -> Unit,
    onBell: () -> Unit,
    onMonth: () -> Unit,
    onTasbih: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val arabic = LocaleHelper.isArabic(context)
    val engine = remember(settings) { PrayerTimeEngine(settings.toCity(), settings) }

    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1000)
        }
    }

    val today = ZonedDateTime.ofInstant(now, engine.zone).toLocalDate()
    val rows = remember(engine, today) { engine.dayRows(today) }
    val next = engine.nextPrayer(settings.enabledPrayers, now)
    val prev = engine.previousPrayer(now)
    val iqamaEnd = prev.time.plus(Duration.ofMinutes((settings.iqama[prev.key.name] ?: 10).toLong()))
    val adhanNow = now.isBefore(iqamaEnd) && prev.key.name in settings.enabledPrayers

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        TopBar(
            title = settings.cityName(arabic),
            rightItem = { TopIcon(Icons.Outlined.Notifications, onBell) },
            leftItem = { MenuIcon(onMenu) },
        )
        // التاريخ الهجري والميلادي تحت اسم المدينة
        Column(Modifier.fillMaxWidth().offset(y = (-6).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hijriLine(context, today), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            if (!adhanNow) {
                Text(gregorianLine(context, today), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }

        if (adhanNow) {
            AdhanNowHero(
                prayer = prev.key,
                adhanTime = formatClock(prev.time, engine.zone),
                remaining = Duration.between(now, iqamaEnd),
                place = settings.mosqueName.ifBlank { settings.cityName(arabic) },
                onAdhanNow = {
                    val intent = Intent(context, AdhanService::class.java).apply {
                        action = AdhanService.ACTION_PLAY
                        putExtra(AdhanService.EXTRA_PRAYER, prev.key.name)
                    }
                    context.startForegroundService(intent)
                },
            )
        } else {
            CountdownHero(
                prayerName = stringResource(next.key.nameRes),
                remaining = Duration.between(now, next.time),
                progress = run {
                    val total = Duration.between(prev.time, next.time).seconds.coerceAtLeast(1)
                    val done = Duration.between(prev.time, now).seconds.coerceIn(0, total)
                    done.toFloat() / total
                },
            )
        }

        Spacer(Modifier.height(10.dp))
        ScheduleStatus(settings = settings, engine = engine, onFix = { openExactAlarmSettings(context) })
        Spacer(Modifier.height(10.dp))
        QuickActions(today = today, onMonth = onMonth, onTasbih = onTasbih)
        Spacer(Modifier.height(10.dp))
        PrayerTable(
            rows = rows,
            highlight = if (adhanNow) prev.key else next.key,
            enabled = settings.enabledPrayers,
            engine = engine,
            onToggle = { key, on -> scope.launch { store.setPrayerEnabled(key.name, on) } },
        )
        Spacer(Modifier.height(20.dp))
    }
}

/* ───────────────────────── العدّاد الدائري ───────────────────────── */

@Composable
private fun CountdownHero(prayerName: String, remaining: Duration, progress: Float) {
    Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
        ArtImage(
            R.drawable.art_mosque_large,
            height = 300.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
            alpha = 0.95f,
            fadeTop = true,
            alignment = Alignment.BottomCenter,
        )
        // الهلال في أعلى اليسار فعلياً مهما كان اتجاه اللغة
        Crescent(Modifier.align(AbsoluteAlignment.TopLeft).padding(horizontal = 34.dp, vertical = 12.dp).size(46.dp))
        Box(Modifier.size(262.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val r = size.minDimension / 2 - stroke
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    Brush.radialGradient(listOf(AppColors.BgBottom.copy(alpha = 0.55f), AppColors.BgMid.copy(alpha = 0.85f)), c, r),
                    radius = r, center = c,
                )
                drawCircle(AppColors.Gold.copy(alpha = 0.35f), radius = r, center = c, style = Stroke(1.5f.dp.toPx()))
                val rect = androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)
                drawArc(
                    Brush.sweepGradient(listOf(AppColors.GoldDim, AppColors.Gold, AppColors.GoldLight, AppColors.Gold), c),
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = rect.topLeft, size = Size(rect.width, rect.height),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    AppColors.GoldLight.copy(alpha = 0.25f),
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = rect.topLeft, size = Size(rect.width, rect.height),
                    style = Stroke(stroke * 3, cap = StrokeCap.Round),
                )
            }
            Crescent(Modifier.align(Alignment.TopCenter).offset(y = (-14).dp).size(22.dp), glow = false)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.home_remaining_to_adhan), style = MaterialTheme.typography.bodyMedium, color = AppColors.Gold)
                Text(
                    prayerName,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp, lineHeight = 52.sp),
                    color = AppColors.Text,
                )
                Text(
                    formatCountdown(remaining),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, letterSpacing = 1.sp),
                    color = AppColors.Gold,
                )
                Spacer(Modifier.height(6.dp))
                GoldOrnament(width = 90.dp)
            }
        }
    }
}

/* ───────────────────────── حالة "حان وقت الأذان" ───────────────────────── */

@Composable
private fun AdhanNowHero(
    prayer: PrayerKey,
    adhanTime: String,
    remaining: Duration,
    place: String,
    onAdhanNow: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            Crescent(Modifier.align(AbsoluteAlignment.TopLeft).padding(horizontal = 40.dp, vertical = 24.dp).size(44.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))
                Text(
                    adhanTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        shadow = Shadow(AppColors.GoldLight.copy(alpha = 0.45f), Offset.Zero, 24f),
                    ),
                    color = AppColors.Text,
                )
                Text(stringResource(R.string.home_adhan_now), style = MaterialTheme.typography.headlineSmall, color = AppColors.Text)
            }
        }
        ArtImage(R.drawable.art_mosque_large, height = 250.dp, fadeTop = false, alignment = Alignment.BottomCenter)

        GlassCard(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), gold = true, radius = 26.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.home_remaining_to_iqama), style = MaterialTheme.typography.titleLarge, color = AppColors.Gold)
                Text(
                    formatCountdown(remaining),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 50.sp),
                    color = AppColors.Text,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DarkMode, null, tint = AppColors.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.home_iqama_of, stringResource(prayer.nameRes)),
                        style = MaterialTheme.typography.bodyLarge, color = AppColors.Text,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            stringResource(R.string.home_play_now),
            Modifier.padding(horizontal = 24.dp),
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            onClick = onAdhanNow,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(AppColors.Card.copy(alpha = 0.8f))
                .border(1.dp, AppColors.CardBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocationOn, null, tint = AppColors.GreenLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(place, style = MaterialTheme.typography.titleMedium, color = AppColors.Text)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ───────────────────────── اختصارات + المناسبة القادمة ───────────────────────── */

@Composable
private fun QuickActions(today: LocalDate, onMonth: () -> Unit, onTasbih: () -> Unit) {
    val event = remember(today) { IslamicEvents.next(today) }
    val name = stringResource(event.nameRes)
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton(stringResource(R.string.quick_month), Modifier.weight(1f), icon = Icons.Outlined.CalendarMonth, onClick = onMonth)
        PillButton(stringResource(R.string.quick_tasbih), Modifier.weight(1f), icon = Icons.Outlined.TouchApp, onClick = onTasbih)
    }
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Card.copy(alpha = 0.6f))
            .border(1.dp, AppColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Event, null, tint = AppColors.Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            when (event.daysLeft) {
                0L -> stringResource(R.string.event_today, name)
                1L -> stringResource(R.string.event_tomorrow, name)
                else -> stringResource(R.string.event_in_days, name, event.daysLeft.toInt())
            },
            style = MaterialTheme.typography.bodySmall, color = AppColors.Text,
        )
    }
}

/* ───────────────────────── حالة الجدولة ───────────────────────── */

/** يُطمئن المستخدم أن الأذان القادم مجدول فعلاً في النظام، أو يحذّره إن مُنع إذن التنبيهات الدقيقة */
@Composable
private fun ScheduleStatus(settings: AppSettings, engine: PrayerTimeEngine, onFix: () -> Unit) {
    val context = LocalContext.current
    val exact = AdhanScheduler.canScheduleExact(context)
    val scheduled = settings.nextAlarm > 0L
    val ok = exact && scheduled
    val text = when {
        !exact -> stringResource(R.string.schedule_no_exact)
        scheduled -> stringResource(R.string.schedule_ok, formatClock(Instant.ofEpochMilli(settings.nextAlarm), engine.zone))
        else -> stringResource(R.string.schedule_none)
    }
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (ok) AppColors.HighlightRow.copy(alpha = 0.7f) else AppColors.Danger.copy(alpha = 0.18f))
            .border(1.dp, if (ok) AppColors.GreenDark else AppColors.Danger.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .then(if (!exact) Modifier.clickable(onClick = onFix) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ok) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
            null, tint = if (ok) AppColors.GreenLight else AppColors.Danger, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) AppColors.Text else AppColors.Danger)
    }
}

/* ───────────────────────── جدول مواقيت اليوم ───────────────────────── */

private fun iconFor(row: DayRow): ImageVector = when (row.key) {
    PrayerKey.FAJR -> Icons.Outlined.WbTwilight
    null -> Icons.Outlined.WbSunny
    PrayerKey.DHUHR -> Icons.Outlined.LightMode
    PrayerKey.ASR -> Icons.Outlined.Brightness6
    PrayerKey.MAGHRIB -> Icons.Outlined.Brightness4
    PrayerKey.ISHA -> Icons.Outlined.DarkMode
}

@Composable
private fun PrayerTable(
    rows: List<DayRow>,
    highlight: PrayerKey,
    enabled: Set<String>,
    engine: PrayerTimeEngine,
    onToggle: (PrayerKey, Boolean) -> Unit,
) {
    GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 22.dp) {
        rows.forEachIndexed { i, row ->
            val active = row.key == highlight
            val on = row.key != null && row.key.name in enabled
            val color = if (active) AppColors.ActiveTab else AppColors.Text
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (active) AppColors.HighlightRow else Color.Transparent)
                    .then(if (row.key != null) Modifier.clickable { onToggle(row.key, !on) } else Modifier)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (active) Icons.AutoMirrored.Outlined.VolumeUp else iconFor(row),
                    null, tint = if (active) color else AppColors.TextSecondary, modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(stringResource(row.nameRes), style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp), color = color)
                Spacer(Modifier.weight(1f))
                Text(
                    formatClock(row.time, engine.zone),
                    style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    color = color,
                    modifier = Modifier.width(78.dp),
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    when {
                        row.key == null -> Icons.Outlined.WbSunny
                        active && on -> Icons.Outlined.NotificationsActive
                        on -> iconFor(row)
                        else -> Icons.Outlined.NotificationsOff
                    },
                    null,
                    tint = when {
                        active -> AppColors.ActiveTab
                        !on && row.key != null -> AppColors.TextMuted
                        else -> AppColors.TextSecondary
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
            if (i < rows.lastIndex) HorizontalDivider(color = AppColors.CardBorder, thickness = 1.dp)
        }
    }
}
