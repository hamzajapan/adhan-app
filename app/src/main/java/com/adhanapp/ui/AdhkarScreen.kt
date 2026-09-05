package com.adhanapp.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.adhkar.AdhkarData
import com.adhanapp.adhkar.Dhikr
import com.adhanapp.adhkar.DhikrCategory
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.launch

/** شاشة الأذكار: بطاقة لكل فئة مع تنقّل وعدّاد ومفضلة (نص الذكر عربي دائماً) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(
    settings: AppSettings,
    store: SettingsStore,
    contentPadding: PaddingValues,
    onMenu: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val index = remember { mutableStateMapOf<String, Int>() }
    val counts = remember { mutableStateMapOf<String, Int>() }
    var totalCount by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<String?>(null) } // "all" | "favorites"

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        TopBar(
            title = stringResource(R.string.tab_adhkar),
            rightItem = { TopIcon(Icons.AutoMirrored.Outlined.MenuBook, { sheet = "all" }) },
            leftItem = { MenuIcon(onMenu) },
        )
        ArtImage(R.drawable.art_adhkar_mosque, height = 190.dp, fadeTop = false)

        AdhkarData.all.forEach { cat ->
            val i = index[cat.id] ?: 0
            val dhikr = cat.items[i]
            DhikrCard(
                category = cat,
                position = i,
                dhikr = dhikr,
                repeated = counts[dhikr.id] ?: 0,
                favorite = dhikr.id in settings.favorites,
                onPrev = { index[cat.id] = (i - 1 + cat.items.size) % cat.items.size },
                onNext = { index[cat.id] = (i + 1) % cat.items.size },
                onCount = {
                    val c = (counts[dhikr.id] ?: 0) + 1
                    counts[dhikr.id] = c
                    totalCount++
                    if (c >= dhikr.count) index[cat.id] = (i + 1) % cat.items.size
                },
                onShuffle = { index[cat.id] = cat.items.indices.random() },
                onReset = {
                    counts[dhikr.id] = 0
                    index[cat.id] = 0
                },
                onFavorite = { scope.launch { store.toggleFavorite(dhikr.id) } },
                onShare = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${dhikr.text}\n\n— ${context.getString(cat.titleRes)}")
                    }
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.adhkar_share)))
                },
            )
            Spacer(Modifier.height(16.dp))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PillButton(stringResource(R.string.adhkar_favorites), Modifier.weight(1f), icon = Icons.Outlined.FavoriteBorder) { sheet = "favorites" }
            CounterPill(totalCount, Modifier.weight(1f)) { totalCount = 0 }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (sheet != null) {
        val favorites = settings.favorites
        val list: List<Pair<DhikrCategory, Dhikr>> =
            AdhkarData.all.flatMap { c -> c.items.map { c to it } }
                .filter { sheet == "all" || it.second.id in favorites }
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppColors.Card,
            contentColor = AppColors.Text,
        ) {
            Text(
                stringResource(if (sheet == "all") R.string.adhkar_all else R.string.adhkar_fav_title),
                style = MaterialTheme.typography.titleLarge, color = AppColors.Gold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center,
            )
            if (list.isEmpty()) {
                Text(
                    stringResource(R.string.adhkar_fav_empty),
                    color = AppColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(list) { (cat, d) ->
                    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp), radius = 16.dp, onClick = {
                        index[cat.id] = cat.items.indexOf(d)
                        sheet = null
                    }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(cat.titleRes), style = MaterialTheme.typography.labelMedium, color = AppColors.Gold)
                            ArabicText(d.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** نص عربي يبقى من اليمين إلى اليسار حتى في الواجهة الفرنسية/الإنجليزية */
@Composable
private fun ArabicText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    Text(
        text,
        style = style.copy(textDirection = TextDirection.Rtl),
        color = AppColors.Text,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DhikrCard(
    category: DhikrCategory,
    position: Int,
    dhikr: Dhikr,
    repeated: Int,
    favorite: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCount: () -> Unit,
    onShuffle: () -> Unit,
    onReset: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), gold = true, radius = 26.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Chevron(pointsLeft = false, color = AppColors.Gold, onClick = onPrev)
                Text(
                    stringResource(category.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.Gold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Chevron(pointsLeft = true, color = AppColors.Gold, onClick = onNext)
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppColors.CardLight.copy(alpha = 0.75f))
                    .clickable(onClick = onCount)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArabicText(dhikr.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 34.sp))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${position + 1}", color = AppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(" / ${category.items.size}", color = AppColors.TextSecondary, fontSize = 16.sp)
                    if (dhikr.count > 1) {
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.adhkar_repeat, repeated, dhikr.count), color = AppColors.TextMuted, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SmallAction(Icons.Outlined.Share, onShare)
                SmallAction(if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, onFavorite,
                    tint = if (favorite) AppColors.Danger else AppColors.Text)
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(AppColors.Green)
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.adhkar_next), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(6.dp))
                SmallAction(Icons.Outlined.Replay, onReset)
                SmallAction(Icons.Outlined.Shuffle, onShuffle)
            }
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, onClick: () -> Unit, tint: Color = AppColors.Text) {
    Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CounterPill(count: Int, modifier: Modifier, onReset: () -> Unit) {
    Row(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(AppColors.Card.copy(alpha = 0.7f))
            .clickable(onClick = onReset),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.adhkar_counter), color = AppColors.Text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(AppColors.CardLight),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", color = AppColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
