package com.adhanapp.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import com.adhanapp.qibla.QiblaMath
import com.adhanapp.settings.AppSettings
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** بوصلة القبلة: تدور مع حساس الاتجاه، والكعبة في المركز، والسهم الذهبي يشير إلى القبلة */
@Composable
fun QiblaScreen(settings: AppSettings, contentPadding: PaddingValues, onMenu: () -> Unit) {
    val context = LocalContext.current
    val bearing = remember(settings.lat, settings.lng) { QiblaMath.bearing(settings.lat, settings.lng) }
    val azimuth = rememberAzimuth(context)
    var hint by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ArtImage(
            R.drawable.art_footer_mosque,
            height = 260.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
            fadeBottom = false,
            alpha = 0.9f,
            alignment = Alignment.BottomCenter,
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                title = stringResource(R.string.tab_qibla),
                rightItem = { TopIcon(Icons.Outlined.GpsFixed, { hint = !hint }, tint = AppColors.Gold) },
                leftItem = { MenuIcon(onMenu) },
            )
            if (hint) {
                Text(
                    stringResource(if (azimuth.available) R.string.qibla_hint_calibrate else R.string.qibla_hint_no_sensor),
                    color = AppColors.Gold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            CompassDial(
                heading = azimuth.value,
                qibla = bearing.toFloat(),
                letters = listOf(R.string.compass_n, R.string.compass_e, R.string.compass_s, R.string.compass_w).map { stringResource(it) },
                modifier = Modifier.size(320.dp),
            )
            Spacer(Modifier.height(20.dp))
            Canvas(Modifier.size(20.dp, 14.dp)) {
                val p = Path().apply {
                    moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(p, AppColors.Gold)
            }
            Text(
                "${bearing.roundToInt()}°",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 62.sp),
                color = AppColors.Text,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Diamond()
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.qibla_direction), style = MaterialTheme.typography.titleLarge, color = AppColors.Gold)
                Spacer(Modifier.width(12.dp))
                Diamond()
            }
            Spacer(Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun Diamond() {
    Canvas(Modifier.size(9.dp)) {
        val c = size.width / 2
        val p = Path().apply { moveTo(c, 0f); lineTo(size.width, c); lineTo(c, size.height); lineTo(0f, c); close() }
        drawPath(p, AppColors.Gold)
    }
}

/** قرص البوصلة: حلقة خارجية ذهبية، شرطات، الحروف ش ج ر غ، الكعبة في المركز، وسهم القبلة */
@Composable
private fun CompassDial(heading: Float, qibla: Float, letters: List<String>, modifier: Modifier) {
    val kaaba = painterResource(R.drawable.art_kaaba)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val rOuter = size.minDimension / 2 - 10.dp.toPx()
            val rTicks = rOuter - 18.dp.toPx()
            val rInner = rOuter - 50.dp.toPx()
            val gold = AppColors.Gold

            // القرص الداكن
            drawCircle(Brush.radialGradient(listOf(AppColors.BgBottom.copy(alpha = 0.9f), AppColors.BgTop.copy(alpha = 0.6f)), c, rOuter), rOuter, c)
            drawCircle(gold.copy(alpha = 0.9f), rOuter, c, style = Stroke(1.2f.dp.toPx()))
            drawCircle(gold.copy(alpha = 0.5f), rTicks + 8.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            drawCircle(gold.copy(alpha = 0.6f), rInner, c, style = Stroke(1.dp.toPx()))

            rotate(-heading, c) {
                // الشرطات
                for (deg in 0 until 360 step 5) {
                    val major = deg % 30 == 0
                    val len = if (major) 12.dp.toPx() else 6.dp.toPx()
                    val a = Math.toRadians(deg.toDouble() - 90)
                    val p1 = Offset(c.x + (rTicks * cos(a)).toFloat(), c.y + (rTicks * sin(a)).toFloat())
                    val p2 = Offset(c.x + ((rTicks - len) * cos(a)).toFloat(), c.y + ((rTicks - len) * sin(a)).toFloat())
                    drawLine(gold.copy(alpha = if (major) 0.95f else 0.5f), p1, p2, strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx())
                }
                // نقاط ذهبية عند الاتجاهات الفرعية
                for (deg in 45 until 360 step 90) {
                    val a = Math.toRadians(deg.toDouble() - 90)
                    drawCircle(gold, 3.dp.toPx(), Offset(c.x + (rOuter * cos(a)).toFloat(), c.y + (rOuter * sin(a)).toFloat()))
                }
                // الحروف: ش (شمال) ج (جنوب) ر (شرق) غ (غرب)
                val paint = Paint().apply {
                    color = gold.toArgb()
                    textSize = 20.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                val rText = rOuter + 22.dp.toPx()
                listOf(0, 90, 180, 270).zip(letters).forEach { (deg, ch) ->
                    val a = Math.toRadians(deg.toDouble() - 90)
                    val x = c.x + (rText * cos(a)).toFloat()
                    val y = c.y + (rText * sin(a)).toFloat() + paint.textSize / 3
                    drawContext.canvas.nativeCanvas.drawText(ch, x, y, paint)
                }
                // مثلث الشمال الذهبي
                val north = Path().apply {
                    moveTo(c.x, c.y - rOuter - 4.dp.toPx())
                    lineTo(c.x - 9.dp.toPx(), c.y - rOuter + 16.dp.toPx())
                    lineTo(c.x + 9.dp.toPx(), c.y - rOuter + 16.dp.toPx())
                    close()
                }
                drawPath(north, gold)
                // سهم القبلة (برتقالي ذهبي) من المركز نحو الاتجاه
                rotate(qibla, c) {
                    val tip = c.y - rInner + 6.dp.toPx()
                    val arrow = Path().apply {
                        moveTo(c.x, tip)
                        lineTo(c.x - 11.dp.toPx(), tip + 30.dp.toPx())
                        lineTo(c.x, tip + 22.dp.toPx())
                        lineTo(c.x + 11.dp.toPx(), tip + 30.dp.toPx())
                        close()
                    }
                    drawPath(arrow, Brush.verticalGradient(listOf(AppColors.GoldLight, Color(0xFFE3A33A))))
                    drawLine(gold.copy(alpha = 0.35f), c, Offset(c.x, tip + 26.dp.toPx()), strokeWidth = 1.dp.toPx())
                }
            }
        }
        Image(kaaba, null, Modifier.size(96.dp))
    }
}

/* ───────────────────────── حساس الاتجاه ───────────────────────── */

class AzimuthState(val value: Float, val available: Boolean)

@Composable
private fun rememberAzimuth(context: Context): AzimuthState {
    var azimuth by remember { mutableFloatStateOf(0f) }
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val deg = ((Math.toDegrees(orient[0].toDouble()) + 360) % 360).toFloat()
                // تنعيم دائري بسيط
                var diff = deg - azimuth
                if (diff > 180) diff -= 360
                if (diff < -180) diff += 360
                azimuth = (azimuth + diff * 0.15f + 360) % 360
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return AzimuthState(azimuth, sensor != null)
}
