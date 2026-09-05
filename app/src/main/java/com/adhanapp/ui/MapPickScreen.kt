package com.adhanapp.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adhanapp.R
import com.adhanapp.cities.City
import com.adhanapp.cities.CityRepository
import com.adhanapp.settings.LocaleHelper
import java.time.ZoneId
import java.util.Locale

/**
 * خريطة تفاعلية (OpenStreetMap عبر Leaflet داخل WebView): يحرّك المستخدم الخريطة
 * حتى يقع الدبوس الذهبي على موقعه، ويمكنه إظهار المساجد القريبة واختيار مسجده،
 * ثم يعتمد الإحداثيات. تحتاج إنترنت لعرض الخريطة.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickScreen(
    repo: CityRepository,
    initialLat: Double,
    initialLng: Double,
    onBack: () -> Unit,
    onPick: (City, String?) -> Unit,
) {
    val context = LocalContext.current
    val arabic = LocaleHelper.isArabic(context)
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mosque by remember { mutableStateOf<String?>(null) }
    val near = center?.let { (la, ln) -> repo.nearest(la, ln) }
    val zone = ZoneId.systemDefault().id
    val hasInitial = initialLat != 0.0 || initialLng != 0.0
    val startUrl = remember {
        val lat = if (hasInitial) initialLat else 30.0
        val lng = if (hasInitial) initialLng else 20.0
        val zoom = if (hasInitial) 13 else 3
        val lang = context.resources.configuration.locales[0].language.takeIf { it in setOf("ar", "fr", "en") } ?: "en"
        "file:///android_asset/map.html?lat=$lat&lng=$lng&zoom=$zoom&lang=$lang"
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        BackRow(onBack, color = AppColors.Gold)
        ScreenTitle(stringResource(R.string.map_title), stringResource(R.string.map_subtitle))
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(false)
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onCenter(lat: Double, lng: Double) {
                                    post { center = lat to lng }
                                }

                                @JavascriptInterface
                                fun onMosque(name: String, lat: Double, lng: Double) {
                                    post {
                                        mosque = name
                                        center = lat to lng
                                    }
                                }
                            },
                            "AndroidBridge",
                        )
                        loadUrl(startUrl)
                    }
                },
                onRelease = { it.destroy() },
            )
        }

        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), radius = 18.dp) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (near != null && center != null) {
                    if (mosque != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Mosque, null, tint = AppColors.GreenLight, modifier = Modifier.width(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(mosque!!, color = AppColors.Text, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(6.dp))
                            TopIcon(Icons.Outlined.Close, { mosque = null }, tint = AppColors.TextMuted)
                        }
                    }
                    val cityLabel = if (arabic) "${near.nameAr} (${near.nameEn})" else "${near.nameEn} (${near.nameAr})"
                    Text(
                        stringResource(R.string.map_nearest, cityLabel),
                        color = if (mosque == null) AppColors.Text else AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        String.format(Locale.US, "%.4f , %.4f", center!!.first, center!!.second),
                        color = AppColors.Gold, style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(stringResource(R.string.map_loading), color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (near != null && center != null) {
            PrimaryButton(
                stringResource(if (mosque != null) R.string.map_adopt_mosque else R.string.map_adopt_location),
                Modifier.padding(horizontal = 16.dp),
            ) {
                onPick(City(near.nameAr, near.nameEn, near.country, center!!.first, center!!.second, zone), mosque)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
