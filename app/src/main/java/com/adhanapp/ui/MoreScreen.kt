package com.adhanapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adhanapp.R

/** شاشة "المزيد": إعدادات، مشاركة، تقييم، أسئلة شائعة، خصوصية */
@Composable
fun MoreScreen(
    contentPadding: PaddingValues,
    onAppSettings: () -> Unit,
    onFaq: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            ArtImage(R.drawable.art_header_moon, height = 270.dp, fadeTop = false)
            Text(
                stringResource(R.string.tab_more),
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.Text,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
            )
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MoreItem(Icons.Outlined.Settings, R.string.more_settings, R.string.more_settings_sub, onAppSettings)
            MoreItem(Icons.Outlined.Share, R.string.more_share, R.string.more_share_sub) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(R.string.more_share_text, "https://play.google.com/store/apps/details?id=${context.packageName}"),
                    )
                }
                context.startActivity(Intent.createChooser(send, context.getString(R.string.more_share)))
            }
            MoreItem(Icons.Outlined.StarOutline, R.string.more_rate, R.string.more_rate_sub) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")),
                    )
                }
            }
            MoreItem(Icons.AutoMirrored.Outlined.HelpOutline, R.string.more_faq, R.string.more_faq_sub, onFaq)
            MoreItem(Icons.Outlined.Shield, R.string.more_privacy, R.string.more_privacy_sub, onPrivacy)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            stringResource(R.string.more_version, versionName(context)),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun versionName(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"

@Composable
private fun MoreItem(icon: ImageVector, @StringRes title: Int, @StringRes subtitle: Int, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), radius = 22.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(icon)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = AppColors.Text)
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}
