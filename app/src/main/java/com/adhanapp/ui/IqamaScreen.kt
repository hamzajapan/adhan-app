package com.adhanapp.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.launch

/** إعدادات الإقامة: الفاصل بالدقائق بين الأذان والإقامة لكل صلاة */
@Composable
fun IqamaScreen(settings: AppSettings, store: SettingsStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val values = remember { mutableStateMapOf<String, Int>().apply { putAll(settings.iqama) } }

    Box(Modifier.fillMaxSize()) {
        ArtImage(R.drawable.art_header_mosque, height = 210.dp, alpha = 0.55f, fadeTop = false)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BackRow(onBack)
            ScreenTitle(stringResource(R.string.iqama_title), stringResource(R.string.iqama_subtitle))
            Spacer(Modifier.height(10.dp))
            GoldOrnament()
            Spacer(Modifier.height(14.dp))

            GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 20.dp, borderColor = AppColors.Gold.copy(alpha = 0.45f)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.iqama_info_title), style = MaterialTheme.typography.titleMedium, color = AppColors.Gold)
                        Text(stringResource(R.string.iqama_info_sub), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier.size(58.dp).border(1.5f.dp, AppColors.Gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = AppColors.Gold, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PrayerKey.entries.forEach { key ->
                    val v = values[key.name] ?: 10
                    GlassCard(Modifier.fillMaxWidth(), radius = 20.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(stringResource(key.nameRes), style = MaterialTheme.typography.headlineSmall, color = AppColors.Text)
                                Text(stringResource(R.string.minute), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                            Spacer(Modifier.weight(1f))
                            StepButton(Icons.Outlined.Remove) { values[key.name] = (v - 1).coerceAtLeast(0) }
                            Spacer(Modifier.width(10.dp))
                            Box(
                                Modifier
                                    .width(92.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppColors.BgBottom.copy(alpha = 0.85f))
                                    .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$v", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp), color = AppColors.Text)
                            }
                            Spacer(Modifier.width(10.dp))
                            StepButton(Icons.Outlined.Add) { values[key.name] = (v + 1).coerceAtMost(60) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            PrimaryButton(stringResource(R.string.iqama_save), Modifier.padding(horizontal = 16.dp), icon = Icons.Outlined.Save) {
                scope.launch {
                    store.setIqama(values.toMap())
                    onBack()
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
                Icon(Icons.Outlined.Shield, null, tint = AppColors.GreenLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.iqama_note),
                    style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardLight)
            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AppColors.GoldLight, modifier = Modifier.size(24.dp))
    }
}
