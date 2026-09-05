package com.adhanapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.alarm.Notifier
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.launch
import java.time.LocalDate

private val TARGETS = listOf(33, 99, 100, 1000)
/** صيغ التسبيح تبقى عربية في كل اللغات */
private val PHRASES = listOf("سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله", "أستغفر الله", "سبحان الله وبحمده")

/** السبحة الرقمية: عدّاد كبير مع اهتزاز عند بلوغ الهدف وحصيلة يومية وإجمالية محفوظة */
@Composable
fun TasbihScreen(settings: AppSettings, store: SettingsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var count by rememberSaveable { mutableIntStateOf(0) }
    var target by rememberSaveable { mutableIntStateOf(33) }
    var phrase by rememberSaveable { mutableStateOf(PHRASES.first()) }
    val todayKey = remember { LocalDate.now().toString() }
    val today = if (settings.tasbihDate == todayKey) settings.tasbihToday else 0

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            title = stringResource(R.string.quick_tasbih),
            rightItem = { TopIcon(Icons.Outlined.Replay, { count = 0 }, tint = AppColors.Gold) },
            leftItem = { Chevron(pointsLeft = true, onClick = onBack) },
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TARGETS.forEach { t -> Chip("$t", selected = target == t) { target = t; count = 0 } }
        }
        Spacer(Modifier.height(22.dp))
        Text(phrase, style = MaterialTheme.typography.headlineMedium, color = AppColors.Gold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))

        // القرص الكبير: كل نقرة تسبيحة، والحلقة الذهبية تبيّن التقدّم نحو الهدف
        Box(
            Modifier
                .size(260.dp)
                .clip(CircleShape)
                .clickable {
                    count++
                    scope.launch { store.addTasbih(todayKey) }
                    if (count % target == 0) Notifier.vibrate(context, longArrayOf(0, 80, 60, 80))
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val r = size.minDimension / 2 - stroke
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(Brush.radialGradient(listOf(AppColors.CardLight, AppColors.Card), c, r), r, c)
                drawCircle(AppColors.Gold.copy(alpha = 0.25f), r, c, style = Stroke(stroke))
                val progress = (count % target).toFloat() / target
                drawArc(
                    Brush.sweepGradient(listOf(AppColors.GoldDim, AppColors.Gold, AppColors.GoldLight, AppColors.Gold), c),
                    startAngle = -90f, sweepAngle = 360f * (if (count > 0 && progress == 0f) 1f else progress), useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp), color = AppColors.Text)
                Text(stringResource(R.string.tasbih_target, target), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PHRASES.take(3).forEach { p -> Chip(p, selected = phrase == p) { phrase = p } }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PHRASES.drop(3).forEach { p -> Chip(p, selected = phrase == p) { phrase = p } }
        }
        Spacer(Modifier.weight(1f))
        GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 18.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.tasbih_today), today)
                Stat(stringResource(R.string.tasbih_total), settings.tasbihTotal)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineSmall, color = AppColors.Gold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
    }
}

/** رقاقة اختيار صغيرة بهوية التطبيق */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) AppColors.Green else AppColors.Card.copy(alpha = 0.7f))
            .border(1.dp, if (selected) AppColors.GreenLight else AppColors.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = AppColors.Text, style = MaterialTheme.typography.labelLarge)
    }
}
