package com.adhanapp.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.adhanapp.R
import com.adhanapp.prayer.PrayerKey
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.ui.AdhanAppTheme
import com.adhanapp.ui.AppBackground
import com.adhanapp.ui.AppColors
import com.adhanapp.ui.ArtImage
import com.adhanapp.ui.Crescent
import com.adhanapp.ui.GlassCard
import com.adhanapp.ui.GoldOrnament
import com.adhanapp.ui.formatClock
import java.time.Instant
import java.time.ZoneId

/** شاشة كاملة تظهر فوق قفل الشاشة أثناء الأذان مع زر إيقاف كبير */
class AdhanActivity : ComponentActivity() {

    private val doneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java)
            .requestDismissKeyguard(this, null)

        ContextCompat.registerReceiver(
            this, doneReceiver,
            IntentFilter(AdhanService.ACTION_ADHAN_DONE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val prayerName = PrayerKey.labelOf(this, intent.getStringExtra(AdhanService.EXTRA_PRAYER) ?: "")
        val time = formatClock(Instant.now(), ZoneId.systemDefault())

        setContent {
            AdhanAppTheme {
                AppBackground { AdhanScreen(prayerName, time, onStop = ::stopAdhan) }
            }
        }
    }

    private fun stopAdhan() {
        startService(Intent(this, AdhanService::class.java).setAction(AdhanService.ACTION_STOP))
        finish()
    }

    override fun onDestroy() {
        unregisterReceiver(doneReceiver)
        super.onDestroy()
    }
}

@Composable
private fun AdhanScreen(prayerName: String, time: String, onStop: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Crescent(Modifier.align(AbsoluteAlignment.TopLeft).padding(horizontal = 40.dp, vertical = 40.dp).size(48.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 66.sp,
                        shadow = Shadow(AppColors.GoldLight.copy(alpha = 0.45f), Offset.Zero, 24f),
                    ),
                    color = AppColors.Text,
                )
                Text(stringResource(R.string.home_adhan_now), style = MaterialTheme.typography.headlineSmall, color = AppColors.Text)
            }
        }
        ArtImage(R.drawable.art_mosque_large, height = 280.dp, fadeTop = false, alignment = Alignment.BottomCenter)
        GlassCard(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), gold = true, radius = 26.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.adhan_now_prayer), style = MaterialTheme.typography.titleLarge, color = AppColors.Gold)
                Text(
                    prayerName,
                    style = MaterialTheme.typography.displayMedium,
                    color = AppColors.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                GoldOrnament(width = 120.dp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(1.5f.dp, AppColors.Gold, RoundedCornerShape(32.dp))
                .clickable(onClick = onStop),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Stop, null, tint = AppColors.Gold, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.adhan_stop), style = MaterialTheme.typography.titleLarge, color = AppColors.Gold, fontSize = 22.sp)
        }
        Spacer(Modifier.height(36.dp))
    }
}
