package com.adhanapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.adhanapp.R
import com.adhanapp.cities.City
import com.adhanapp.cities.CityRepository
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * تحديد الموقع (شاشة الإعداد الأول وأيضاً من الإعدادات):
 * تلقائياً عبر GPS، أو بحث عن مدينة، أو إدخال الإحداثيات يدوياً — كل ذلك دون إنترنت.
 */
@Composable
fun LocationScreen(
    store: SettingsStore,
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { CityRepository(context.applicationContext) }
    var mode by remember { mutableStateOf("auto") }
    var step by remember { mutableStateOf("options") } // options | search | manual
    var locating by remember { mutableStateOf(false) }
    var locationOff by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save(city: City, mosque: String? = null) {
        scope.launch {
            store.setCity(city)
            if (mosque != null) store.setMosqueName(mosque)
            onDone()
        }
    }

    fun locate() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            error = context.getString(R.string.loc_error_off)
            locationOff = true
            return
        }
        locationOff = false
        locating = true
        error = null
        scope.launch {
            val loc = runCatching { currentLocation(context) }.getOrNull()
            locating = false
            if (loc == null) {
                error = context.getString(R.string.loc_error_timeout)
            } else {
                val near = repo.nearest(loc.latitude, loc.longitude)
                save(City(near.nameAr, near.nameEn, near.country, loc.latitude, loc.longitude, near.timezone))
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate() else error = context.getString(R.string.loc_error_denied)
    }

    when (step) {
        "search" -> CitySearchScreen(repo, onBack = { step = "options" }, onPick = { save(it) })
        "manual" -> MapPickScreen(
            repo, initialLat = initialLat, initialLng = initialLng,
            onBack = { step = "options" }, onPick = { city, mosque -> save(city, mosque) },
        )
        else -> Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BackRow(onBack, color = AppColors.Gold)
            ScreenTitle(stringResource(R.string.loc_title), stringResource(R.string.loc_subtitle))
            ArtImage(R.drawable.art_header_mosque, height = 210.dp)

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OptionCard(Icons.Outlined.MyLocation, R.string.loc_auto, R.string.loc_auto_sub, mode == "auto") { mode = "auto" }
                OptionCard(Icons.Outlined.Search, R.string.loc_search, R.string.loc_search_sub, mode == "search") { mode = "search" }
                OptionCard(Icons.Outlined.Map, R.string.loc_map, R.string.loc_map_sub, mode == "manual") { mode = "manual" }
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    error!!, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .then(
                            if (locationOff) Modifier.clickable {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } else Modifier,
                        ),
                )
            }
            Spacer(Modifier.height(26.dp))
            if (locating) {
                CircularProgressIndicator(color = AppColors.Gold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.loc_locating), color = AppColors.TextSecondary)
            } else {
                PrimaryButton(stringResource(R.string.loc_continue), Modifier.padding(horizontal = 16.dp)) {
                    when (mode) {
                        "auto" -> {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) locate()
                            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                        "search" -> step = "search"
                        else -> step = "manual"
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            ArchOrnament()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionCard(icon: ImageVector, @StringRes title: Int, @StringRes subtitle: Int, selected: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), gold = selected, radius = 22.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(icon, size = 72.dp, iconSize = 34.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = AppColors.Text)
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}

private fun enabledProviders(lm: LocationManager): List<String> =
    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused")
        .filter { it in lm.allProviders && lm.isProviderEnabled(it) }

/** آخر موقع معروف من أي مزوّد (الأحدث)، مع حد أقصى لعمره */
@SuppressLint("MissingPermission")
private fun lastKnown(lm: LocationManager, maxAgeMs: Long): Location? =
    enabledProviders(lm)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time <= maxAgeMs }
        .maxByOrNull { it.time }

/** طلب تحديث حي من كل المزوّدات معاً؛ أول إحداثيات تصل تفوز */
@SuppressLint("MissingPermission")
private suspend fun liveLocation(lm: LocationManager): Location? {
    val providers = enabledProviders(lm)
    if (providers.isEmpty()) return null
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        providers.forEach { runCatching { lm.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) } }
        cont.invokeOnCancellation { lm.removeUpdates(listener) }
    }
}

/**
 * الموقع الحالي: آخر موقع حديث (≤ 5 دقائق) فوراً، وإلا تحديث حي بمهلة، وإلا أي موقع معروف قديم.
 * يعيد null إذا كانت خدمة الموقع مغلقة أو لم يصل شيء.
 */
private suspend fun currentLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(lm)) return null
    lastKnown(lm, maxAgeMs = 5 * 60_000L)?.let { return it }
    return withTimeoutOrNull(40_000) { liveLocation(lm) } ?: lastKnown(lm, maxAgeMs = Long.MAX_VALUE)
}

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Gold,
    unfocusedBorderColor = AppColors.CardBorder,
    cursorColor = AppColors.Gold,
    focusedContainerColor = AppColors.Card.copy(alpha = 0.8f),
    unfocusedContainerColor = AppColors.Card.copy(alpha = 0.8f),
    focusedTextColor = AppColors.Text,
    unfocusedTextColor = AppColors.Text,
    focusedPlaceholderColor = AppColors.TextMuted,
    unfocusedPlaceholderColor = AppColors.TextMuted,
    focusedLabelColor = AppColors.Gold,
    unfocusedLabelColor = AppColors.TextSecondary,
)

/** البحث عن مدينة من القائمة المضمّنة (393 مدينة) */
@Composable
fun CitySearchScreen(repo: CityRepository, onBack: () -> Unit, onPick: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { repo.search(query) }
    val arabic = LocaleHelper.isArabic(LocalContext.current)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BackRow(onBack, color = AppColors.Gold)
        ScreenTitle(stringResource(R.string.city_search_title), stringResource(R.string.city_search_sub))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.city_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AppColors.Gold) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(results.take(80)) { city ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.Card.copy(alpha = 0.7f))
                        .clickable { onPick(city) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (arabic) city.nameAr else city.nameEn, style = MaterialTheme.typography.titleMedium, color = AppColors.Text)
                    Spacer(Modifier.weight(1f))
                    Text("${if (arabic) city.nameEn else city.nameAr} · ${city.country}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }
    }
}

