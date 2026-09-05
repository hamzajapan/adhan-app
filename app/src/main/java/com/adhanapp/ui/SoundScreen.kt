package com.adhanapp.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adhanapp.R
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.SettingsStore
import com.adhanapp.sounds.AdhanCatalog
import com.adhanapp.sounds.AdhanDownloader
import com.adhanapp.sounds.AdhanEntry
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * اختيار صوت الأذان من فهرس إسلام ويب: استماع مباشر لأي أذان،
 * ثم «اعتماد» ينزّل الملف على الجهاز ويجعله أذان التطبيق (العام أو الفجر).
 */
@Composable
fun SoundScreen(
    settings: AppSettings,
    store: SettingsStore,
    onBack: (() -> Unit)?,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = remember { AdhanCatalog(context.applicationContext) }

    var forFajr by remember { mutableStateOf(false) }
    val adoptedId = (if (forFajr) settings.fajrSound else settings.sound).toIntOrNull()

    var query by remember { mutableStateOf("") }
    var catalogVersion by remember { mutableIntStateOf(0) }
    val results = remember(query, catalogVersion) { catalog.search(query) }
    var selected by remember { mutableStateOf<AdhanEntry?>(null) }
    var playingId by remember { mutableStateOf<Int?>(null) }
    var bufferingId by remember { mutableStateOf<Int?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // النص، هل هو خطأ

    DisposableEffect(Unit) { onDispose { SoundPreview.stop() } }

    fun togglePlay(entry: AdhanEntry) {
        if (playingId == entry.id || bufferingId == entry.id) {
            SoundPreview.stop()
            playingId = null
            bufferingId = null
            return
        }
        val local = AdhanDownloader.fileFor(context, entry.id).takeIf { it.length() > 0 }
        val source = local?.let { android.net.Uri.fromFile(it).toString() } ?: entry.url
        message = null
        bufferingId = entry.id
        playingId = null
        SoundPreview.play(
            context, source,
            onPrepared = {
                bufferingId = null
                playingId = entry.id
            },
            onDone = { playingId = null },
            onError = {
                bufferingId = null
                playingId = null
                message = context.getString(R.string.sound_play_error) to true
            },
        )
    }

    fun adopt() {
        val entry = selected ?: return
        SoundPreview.stop()
        playingId = null
        bufferingId = null
        message = null
        downloading = true
        progress = -1f
        scope.launch {
            try {
                AdhanDownloader.download(context, entry) { progress = it }
                if (forFajr) store.setFajrSound(entry.id.toString()) else store.setSound(entry.id.toString())
                val keep = setOfNotNull(
                    (if (forFajr) settings.sound else entry.id.toString()).toIntOrNull(),
                    (if (forFajr) entry.id.toString() else settings.fajrSound).toIntOrNull(),
                )
                AdhanDownloader.keepOnly(context, keep)
                onSaved()
            } catch (e: Exception) {
                message = context.getString(R.string.sound_download_error) to true
            } finally {
                downloading = false
            }
        }
    }

    fun refreshCatalog() {
        refreshing = true
        message = null
        scope.launch {
            try {
                val n = catalog.refresh()
                catalogVersion++
                message = context.getString(R.string.sound_refreshed, n) to false
            } catch (e: Exception) {
                message = context.getString(R.string.sound_refresh_error) to true
            } finally {
                refreshing = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackRow(onBack, rightItem = {
            if (refreshing) CircularProgressIndicator(Modifier.size(22.dp).padding(horizontal = 8.dp), color = AppColors.Gold, strokeWidth = 2.dp)
            else TopIcon(Icons.Outlined.Refresh, ::refreshCatalog, tint = AppColors.Gold)
        })
        ScreenTitle(stringResource(R.string.sound_title), stringResource(R.string.sound_subtitle))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.sound_general), selected = !forFajr) { forFajr = false; selected = null }
            Chip(stringResource(R.string.sound_fajr), selected = forFajr) { forFajr = true; selected = null }
        }
        if (forFajr && adoptedId == null) {
            Text(
                stringResource(R.string.sound_fajr_none),
                style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.sound_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AppColors.Gold) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = appTextFieldColors(),
        )
        Text(
            stringResource(R.string.sound_count, results.size),
            style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted,
            modifier = Modifier.padding(top = 6.dp),
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(results, key = { it.id }) { entry ->
                SoundCard(
                    entry = entry,
                    selected = selected?.id == entry.id,
                    adopted = adoptedId == entry.id,
                    downloaded = AdhanDownloader.isDownloaded(context, entry.id),
                    playing = playingId == entry.id,
                    buffering = bufferingId == entry.id,
                    onSelect = { selected = entry },
                    onPlay = { togglePlay(entry) },
                )
            }
        }

        message?.let { (text, isError) ->
            Text(
                text, color = if (isError) AppColors.Danger else AppColors.GreenLight, style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (downloading) {
                if (progress >= 0f) LinearProgressIndicator(
                    progress = { progress }, modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Gold, trackColor = AppColors.CardBorder,
                ) else LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Gold, trackColor = AppColors.CardBorder)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (progress >= 0f) stringResource(R.string.sound_downloading_pct, (progress * 100).roundToInt()) else stringResource(R.string.sound_downloading),
                    color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            } else {
                val label = stringResource(
                    when {
                        selected == null -> R.string.sound_choose
                        selected?.id == adoptedId -> R.string.sound_adopted_current
                        forFajr -> R.string.sound_adopt_fajr
                        else -> R.string.sound_adopt
                    },
                )
                if (selected != null && selected?.id != adoptedId) {
                    PrimaryButton(label, onClick = ::adopt)
                } else {
                    PillButton(label, Modifier.fillMaxWidth(), tint = AppColors.TextSecondary) { }
                }
                if (onBack != null && settings.sound.isBlank()) {
                    Text(
                        stringResource(R.string.sound_skip),
                        color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onSaved).padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundCard(
    entry: AdhanEntry,
    selected: Boolean,
    adopted: Boolean,
    downloaded: Boolean,
    playing: Boolean,
    buffering: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), gold = selected, radius = 22.dp, onClick = onSelect) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            // دائرة الاختيار على اليمين: خضراء للمعتمد، ذهبية للمحدد
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (adopted) AppColors.Green else Color.Transparent)
                    .border(1.5f.dp, if (adopted) AppColors.Green else if (selected) AppColors.Gold else AppColors.TextMuted, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (adopted) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                else if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(AppColors.Gold))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.reader, style = MaterialTheme.typography.titleLarge, color = AppColors.Text, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (downloaded) {
                        Icon(Icons.Outlined.DownloadDone, null, tint = AppColors.GreenLight, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Gold.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.width(12.dp))
            // زر الاستماع بحلقة ذهبية على اليسار
            Box(Modifier.size(56.dp).clip(CircleShape).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2 - 2.dp.toPx()
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(AppColors.Gold.copy(alpha = 0.35f), r, c, style = Stroke(1.dp.toPx()))
                    drawArc(
                        AppColors.Gold,
                        startAngle = 120f, sweepAngle = if (playing) 360f else 150f, useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                if (buffering) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = AppColors.Gold, strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        null, tint = AppColors.Gold, modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
