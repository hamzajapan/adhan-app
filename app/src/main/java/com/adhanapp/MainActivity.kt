package com.adhanapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhanapp.alarm.AdhanScheduler
import com.adhanapp.settings.AppSettings
import com.adhanapp.settings.LocaleHelper
import com.adhanapp.settings.SettingsStore
import com.adhanapp.ui.AdhanAppTheme
import com.adhanapp.ui.AdhkarScreen
import com.adhanapp.ui.AppBackground
import com.adhanapp.ui.AppSettingsScreen
import com.adhanapp.ui.BottomNavBar
import com.adhanapp.ui.FaqScreen
import com.adhanapp.ui.HomeScreen
import com.adhanapp.ui.IqamaScreen
import com.adhanapp.ui.LocationScreen
import com.adhanapp.ui.MonthScreen
import com.adhanapp.ui.MoreScreen
import com.adhanapp.ui.PrivacyScreen
import com.adhanapp.ui.QiblaScreen
import com.adhanapp.ui.SoundScreen
import com.adhanapp.ui.Tab
import com.adhanapp.ui.TasbihScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** تطبيق لغة التطبيق المختارة قبل إنشاء أي واجهة */
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AdhanAppTheme {
                AppBackground { AppRoot() }
            }
        }
    }
}

/** الشاشات الفرعية فوق التبويبات الرئيسية */
enum class Route { MAIN, SOUND, IQAMA, LOCATION, APP_SETTINGS, FAQ, PRIVACY, MONTH, TASBIH }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    val settings by store.settings.collectAsStateWithLifecycle<AppSettings?>(initialValue = null)
    val s = settings ?: return // بانتظار قراءة الإعدادات من القرص

    // طلب إذن الإشعارات (أندرويد 13+)
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // اتجاه الواجهة يتبع لغة التطبيق (RTL للعربية، LTR للفرنسية والإنجليزية) عبر إعدادات النظام
    if (!s.onboarded) {
        OnboardingFlow(store = store, settings = s)
    } else {
        // إعادة الجدولة عند فتح التطبيق وعند أي تغيير في الإعدادات
        // (nextAlarm يكتبه المجدول نفسه، فلا يُعدّ تغييراً يستدعي إعادة الجدولة)
        LaunchedEffect(s.copy(nextAlarm = 0L)) { AdhanScheduler.scheduleNext(context) }
        MainShell(store = store, settings = s)
    }
}

/** الإعداد الأول: تحديد الموقع ← اختيار صوت الأذان ← البدء */
@Composable
private fun OnboardingFlow(store: SettingsStore, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(if (settings.cityNameEn.isEmpty()) 0 else 1) }
    when (step) {
        0 -> LocationScreen(store = store, onBack = null, onDone = { step = 1 })
        else -> SoundScreen(
            settings = settings,
            store = store,
            onBack = { step = 0 },
            onSaved = {
                // يُطلب إذن التنبيهات الدقيقة قبل setOnboarded لأن هذه الشاشة تُزال من التركيب
                // فور حفظه (فيُلغى ما بعده)، والجدولة نفسها تتم في MainShell عند الدخول.
                if (!AdhanScheduler.canScheduleExact(context)) openExactAlarmSettings(context)
                scope.launch { store.setOnboarded() }
            },
        )
    }
    BackHandler(enabled = step != 0) { step = 0 }
}

@Composable
private fun MainShell(store: SettingsStore, settings: AppSettings) {
    val stack = remember { mutableStateListOf(Route.MAIN) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val push: (Route) -> Unit = { r -> if (stack.last() != r) stack.add(r) }

    BackHandler(enabled = stack.size > 1 || tab != Tab.HOME) {
        if (stack.size > 1) pop() else tab = Tab.HOME
    }

    when (stack.last()) {
        Route.MAIN -> Box(Modifier.fillMaxSize()) {
            val padding = PaddingValues(bottom = 100.dp)
            when (tab) {
                Tab.HOME -> HomeScreen(
                    settings = settings, store = store, contentPadding = padding,
                    onMenu = { push(Route.APP_SETTINGS) },
                    onBell = { push(Route.IQAMA) },
                    onMonth = { push(Route.MONTH) },
                    onTasbih = { push(Route.TASBIH) },
                )
                Tab.ADHKAR -> AdhkarScreen(settings = settings, store = store, contentPadding = padding, onMenu = { push(Route.APP_SETTINGS) })
                Tab.QIBLA -> QiblaScreen(settings = settings, contentPadding = padding, onMenu = { push(Route.APP_SETTINGS) })
                Tab.MORE -> MoreScreen(
                    contentPadding = padding,
                    onAppSettings = { push(Route.APP_SETTINGS) },
                    onFaq = { push(Route.FAQ) },
                    onPrivacy = { push(Route.PRIVACY) },
                )
            }
            BottomNavBar(current = tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
        }
        Route.SOUND -> SoundScreen(settings = settings, store = store, onBack = pop, onSaved = pop)
        Route.IQAMA -> IqamaScreen(settings = settings, store = store, onBack = pop)
        Route.LOCATION -> LocationScreen(
            store = store, onBack = pop, onDone = pop,
            initialLat = settings.lat, initialLng = settings.lng,
        )
        Route.APP_SETTINGS -> AppSettingsScreen(
            settings = settings, store = store, onBack = pop,
            onLocation = { push(Route.LOCATION) },
            onSound = { push(Route.SOUND) },
            onIqama = { push(Route.IQAMA) },
        )
        Route.FAQ -> FaqScreen(onBack = pop)
        Route.PRIVACY -> PrivacyScreen(onBack = pop)
        Route.MONTH -> MonthScreen(settings = settings, onBack = pop)
        Route.TASBIH -> TasbihScreen(settings = settings, store = store, onBack = pop)
    }
}

/** فتح شاشة إذن التنبيهات الدقيقة في إعدادات النظام */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }
}
