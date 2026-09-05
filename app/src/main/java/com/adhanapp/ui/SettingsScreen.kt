package com.adhanapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adhanapp.R
import com.adhanapp.alarm.AdhanLog
import com.adhanapp.alarm.AdhanScheduler
import com.adhanapp.alarm.AdhanService
import com.adhanapp.openExactAlarmSettings
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.prayer.PrayerTimeEngine
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.Backup
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import com.adhanapp.sounds.AdhanCatalog
import kotlinx.coroutines.launch

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 30)

/** إعدادات التطبيق: الموقع، الصوت، الإقامة، التنبيهات، الصلوات، الحساب، الموثوقية، النسخ الاحتياطي */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    settings: AppSettings,
    store: SettingsStore,
    onBack: () -> Unit,
    onLocation: () -> Unit,
    onSound: () -> Unit,
    onIqama: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = remember { AdhanCatalog(context.applicationContext) }
    var sheet by remember { mutableStateOf<String?>(null) } // method | madhab | log
    var batteryRefresh by remember { mutableIntStateOf(0) }
    var testScheduled by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { SoundPreview.stop() } }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            notice = runCatching { Backup.import(context, store, uri) }
                .fold({ context.getString(R.string.import_ok) }, { context.getString(R.string.import_bad) })
        }
    }
    val currentLang = LocaleHelper.get(context)

    val pm = context.getSystemService(PowerManager::class.java)
    val batteryIgnored = batteryRefresh >= 0 && pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    val exactAllowed = AdhanScheduler.canScheduleExact(context)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackRow(onBack)
        ScreenTitle(stringResource(R.string.settings_title), stringResource(R.string.more_settings_sub))
        Spacer(Modifier.height(10.dp))
        GoldOrnament()
        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel(stringResource(R.string.sec_language))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocaleHelper.OPTIONS.forEach { lang ->
                        Chip(LocaleHelper.label(context, lang), selected = currentLang == lang) {
                            if (currentLang != lang) {
                                LocaleHelper.set(context, lang)
                                (context as? Activity)?.recreate()
                            }
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.sec_basics))
            GlassCard(Modifier.fillMaxWidth()) {
                SettingRow(Icons.Outlined.LocationOn, stringResource(R.string.row_location), locationLabel(context, settings), onClick = onLocation)
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(R.string.row_sound), soundLabel(context, catalog, settings), onClick = onSound)
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(Icons.Outlined.Schedule, stringResource(R.string.iqama_title), stringResource(R.string.row_iqama_sub), onClick = onIqama)
            }

            SectionLabel(stringResource(R.string.sec_alerts))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Notifications, null, tint = AppColors.Gold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.reminder_before), style = MaterialTheme.typography.titleMedium, color = AppColors.Text)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        REMINDER_OPTIONS.forEach { m ->
                            Chip(if (m == 0) stringResource(R.string.reminder_none) else stringResource(R.string.reminder_min, m), selected = settings.reminderMinutes == m) {
                                scope.launch { store.setReminderMinutes(m) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = AppColors.CardBorder)
                ToggleRow(Icons.Outlined.Mosque, stringResource(R.string.remind_friday), stringResource(R.string.remind_friday_sub), settings.remindFriday) {
                    scope.launch { store.setRemindFriday(it) }
                }
                HorizontalDivider(color = AppColors.CardBorder)
                ToggleRow(Icons.Outlined.DarkMode, stringResource(R.string.remind_fasting), stringResource(R.string.remind_fasting_sub), settings.remindFasting) {
                    scope.launch { store.setRemindFasting(it) }
                }
            }

            SectionLabel(stringResource(R.string.sec_prayers))
            GlassCard(Modifier.fillMaxWidth()) {
                PrayerKey.entries.forEachIndexed { i, key ->
                    val enabled = key.name in settings.enabledPrayers
                    val vibrate = enabled && key.name in settings.vibrateOnly
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(key.nameRes), style = MaterialTheme.typography.titleMedium, color = AppColors.Text, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(stringResource(R.string.mode_adhan), selected = enabled && !vibrate) {
                                scope.launch {
                                    store.setPrayerEnabled(key.name, true)
                                    store.setVibrateOnly(key.name, false)
                                }
                            }
                            Chip(stringResource(R.string.mode_vibrate), selected = vibrate) {
                                scope.launch {
                                    store.setPrayerEnabled(key.name, true)
                                    store.setVibrateOnly(key.name, true)
                                }
                            }
                            Chip(stringResource(R.string.mode_off), selected = !enabled) { scope.launch { store.setPrayerEnabled(key.name, false) } }
                        }
                    }
                    if (i < PrayerKey.entries.lastIndex) HorizontalDivider(color = AppColors.CardBorder)
                }
            }

            SectionLabel(stringResource(R.string.sec_calc))
            GlassCard(Modifier.fillMaxWidth()) {
                SettingRow(Icons.Outlined.Public, stringResource(R.string.row_method), PrayerTimeEngine.methodLabel(context, settings.method)) { sheet = "method" }
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(Icons.Outlined.Tune, stringResource(R.string.row_madhab), PrayerTimeEngine.madhabLabel(context, settings.madhab)) { sheet = "madhab" }
            }

            SectionLabel(stringResource(R.string.sec_reliability))
            GlassCard(Modifier.fillMaxWidth()) {
                SettingRow(
                    Icons.Outlined.Alarm, stringResource(R.string.row_exact),
                    stringResource(if (exactAllowed) R.string.state_on else R.string.state_off_tap),
                    valueColor = if (exactAllowed) AppColors.GreenLight else AppColors.Danger,
                ) { if (!exactAllowed) openExactAlarmSettings(context) }
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(
                    Icons.Outlined.BatterySaver, stringResource(R.string.row_battery),
                    stringResource(if (batteryIgnored) R.string.state_on else R.string.state_off_tap),
                    valueColor = if (batteryIgnored) AppColors.GreenLight else AppColors.Danger,
                ) {
                    if (!batteryIgnored) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                        )
                        batteryRefresh++
                    }
                }
                val autostart = miuiAutostartIntent(context)
                if (autostart != null) {
                    HorizontalDivider(color = AppColors.CardBorder)
                    SettingRow(
                        Icons.Outlined.Alarm, stringResource(R.string.row_autostart),
                        stringResource(R.string.row_autostart_sub),
                        valueColor = AppColors.Gold,
                    ) { runCatching { context.startActivity(autostart) } }
                }
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(Icons.Outlined.History, stringResource(R.string.row_log), stringResource(R.string.row_log_sub)) { sheet = "log" }
            }

            PillButton(stringResource(R.string.test_now), Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.VolumeUp, gold = true, tint = AppColors.Gold) {
                val intent = Intent(context, AdhanService::class.java).apply {
                    action = AdhanService.ACTION_PLAY
                    putExtra(AdhanService.EXTRA_PRAYER, AdhanScheduler.TEST_PRAYER)
                }
                context.startForegroundService(intent)
            }
            PillButton(stringResource(R.string.test_in_minute), Modifier.fillMaxWidth(), icon = Icons.Outlined.Alarm, gold = true, tint = AppColors.Gold) {
                AdhanScheduler.scheduleTest(context, 60)
                testScheduled = true
            }
            if (testScheduled) {
                Text(
                    stringResource(R.string.test_hint),
                    style = MaterialTheme.typography.bodySmall, color = AppColors.GreenLight,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel(stringResource(R.string.sec_backup))
            GlassCard(Modifier.fillMaxWidth()) {
                SettingRow(Icons.Outlined.Upload, stringResource(R.string.export_title), stringResource(R.string.export_sub)) {
                    scope.launch { runCatching { Backup.export(context, store) }.onFailure { notice = context.getString(R.string.export_fail) } }
                }
                HorizontalDivider(color = AppColors.CardBorder)
                SettingRow(Icons.Outlined.Download, stringResource(R.string.import_title), stringResource(R.string.import_sub)) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }
            if (notice != null) {
                Text(notice!!, style = MaterialTheme.typography.bodySmall, color = AppColors.GreenLight, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    when (sheet) {
        "method", "madhab" -> ChoiceSheet(
            isMethod = sheet == "method", settings = settings,
            onPick = { key -> scope.launch { if (sheet == "method") store.setMethod(key) else store.setMadhab(key) } },
            onDismiss = { sheet = null },
        )
        "log" -> LogSheet(onDismiss = { sheet = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(isMethod: Boolean, settings: AppSettings, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val options = if (isMethod) PrayerTimeEngine.METHODS else PrayerTimeEngine.MADHABS
    val current = if (isMethod) settings.method else settings.madhab
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Card,
        contentColor = AppColors.Text,
    ) {
        Text(
            stringResource(if (isMethod) R.string.row_method else R.string.row_madhab),
            style = MaterialTheme.typography.titleLarge, color = AppColors.Gold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center,
        )
        options.forEach { (key, labelRes) ->
            val label = stringResource(labelRes)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(key)
                        onDismiss()
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = if (key == current) AppColors.Gold else AppColors.Text, modifier = Modifier.weight(1f))
                if (key == current) Icon(Icons.Outlined.Check, null, tint = AppColors.Gold)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lines = remember { AdhanLog.read(context) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Card,
        contentColor = AppColors.Text,
    ) {
        Text(
            stringResource(R.string.row_log), style = MaterialTheme.typography.titleLarge, color = AppColors.Gold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center,
        )
        if (lines.isEmpty()) {
            Text(stringResource(R.string.log_empty), color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            items(lines) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, color = AppColors.Text, modifier = Modifier.padding(vertical = 6.dp))
                HorizontalDivider(color = AppColors.CardBorder)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** شاشة «التشغيل التلقائي» في MIUI إن وُجدت على الجهاز، وإلا null */
private fun miuiAutostartIntent(context: android.content.Context): Intent? {
    val intent = Intent().setClassName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity",
    )
    return intent.takeIf { context.packageManager.resolveActivity(it, 0) != null }
}

private fun locationLabel(context: android.content.Context, settings: AppSettings): String =
    listOf(settings.cityName(LocaleHelper.isArabic(context)).ifBlank { context.getString(R.string.location_unset) }, settings.mosqueName)
        .filter { it.isNotBlank() }.joinToString(" · ")

/** اسم الأذان المعتمد للعرض (مع أذان الفجر إن وُجد)، أو النغمة الافتراضية إن لم يُعتمد شيء */
private fun soundLabel(context: android.content.Context, catalog: AdhanCatalog, settings: AppSettings): String {
    fun name(id: String) = id.toIntOrNull()?.let { catalog.byId(it) }?.reader
    val general = name(settings.sound) ?: return context.getString(R.string.sound_none_label)
    val fajr = name(settings.fajrSound)
    return if (fajr != null) context.getString(R.string.sound_fajr_label, general, fajr) else general
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = AppColors.Gold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                uncheckedThumbColor = AppColors.TextSecondary, uncheckedTrackColor = AppColors.CardLight,
                uncheckedBorderColor = AppColors.CardBorder,
            ),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = AppColors.TextSecondary,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Text)
            Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
        }
        Chevron(pointsLeft = true, color = AppColors.TextMuted, modifier = Modifier.size(28.dp))
    }
}
