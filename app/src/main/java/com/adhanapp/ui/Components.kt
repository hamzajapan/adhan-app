package com.adhanapp.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhanapp.R
import kotlin.random.Random

/* ─────────────────────────── الخلفية والزخارف ─────────────────────────── */

/** خلفية كل الشاشات: تدرّج أخضر داكن + نجوم ذهبية خافتة */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppColors.BgTop, AppColors.BgMid, AppColors.BgBottom))),
    ) {
        StarField(Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun StarField(modifier: Modifier = Modifier, seed: Int = 7, count: Int = 80) {
    Canvas(modifier) {
        val rnd = Random(seed)
        repeat(count) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height * 0.75f
            val r = (0.5f + rnd.nextFloat() * 1.3f).dp.toPx()
            val gold = rnd.nextFloat() < 0.4f
            val base = if (gold) AppColors.GoldLight else Color.White
            drawCircle(base.copy(alpha = 0.12f + rnd.nextFloat() * 0.4f), radius = r, center = Offset(x, y))
        }
    }
}

/** صورة فنية (مسجد/هلال) تذوب أطرافها في الخلفية */
@Composable
fun ArtImage(
    @DrawableRes res: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    fadeTop: Boolean = true,
    fadeBottom: Boolean = true,
    alignment: Alignment = Alignment.Center,
) {
    Box(modifier.fillMaxWidth().height(height)) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = alignment,
            alpha = alpha,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to (if (fadeTop) AppColors.BgTop else Color.Transparent),
                    0.22f to Color.Transparent,
                    0.72f to Color.Transparent,
                    1f to (if (fadeBottom) AppColors.BgMid else Color.Transparent),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to AppColors.BgMid.copy(alpha = 0.75f),
                    0.12f to Color.Transparent,
                    0.88f to Color.Transparent,
                    1f to AppColors.BgMid.copy(alpha = 0.75f),
                ),
            ),
        )
    }
}

/** هلال ذهبي مرسوم برمجياً */
@Composable
fun Crescent(modifier: Modifier = Modifier, color: Color = AppColors.Gold, glow: Boolean = true) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        if (glow) drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center = c, radius = r * 1.6f),
            radius = r * 1.6f, center = c,
        )
        val outer = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
        val inner = Path().apply {
            val ir = r * 0.82f
            val ic = Offset(c.x + r * 0.42f, c.y - r * 0.2f)
            addOval(Rect(ic.x - ir, ic.y - ir, ic.x + ir, ic.y + ir))
        }
        val moon = Path().apply { op(outer, inner, PathOperation.Difference) }
        drawPath(moon, Brush.linearGradient(listOf(AppColors.GoldLight, color, AppColors.GoldDim)))
    }
}

/** زخرفة ذهبية: خط ─ معيّن ✦ خط */
@Composable
fun GoldOrnament(modifier: Modifier = Modifier, width: Dp = 180.dp) {
    Canvas(modifier.width(width).height(14.dp)) {
        val cy = size.height / 2
        val cx = size.width / 2
        val line = Brush.horizontalGradient(listOf(Color.Transparent, AppColors.Gold, AppColors.Gold, Color.Transparent))
        drawLine(line, Offset(0f, cy), Offset(cx - 14.dp.toPx(), cy), strokeWidth = 1.dp.toPx())
        drawLine(line, Offset(cx + 14.dp.toPx(), cy), Offset(size.width, cy), strokeWidth = 1.dp.toPx())
        val s = 5.dp.toPx()
        val diamond = Path().apply {
            moveTo(cx, cy - s); lineTo(cx + s, cy); lineTo(cx, cy + s); lineTo(cx - s, cy); close()
        }
        drawPath(diamond, AppColors.GoldLight)
        val d = 2.5f.dp.toPx()
        listOf(cx - 26.dp.toPx(), cx + 26.dp.toPx()).forEach { x ->
            val small = Path().apply { moveTo(x, cy - d); lineTo(x + d, cy); lineTo(x, cy + d); lineTo(x - d, cy); close() }
            drawPath(small, AppColors.Gold)
        }
    }
}

/** الزخرفة السفلية: خط بنقاط وقوس مسجد صغير (كما في شاشة الموقع) */
@Composable
fun ArchOrnament(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        val cx = size.width / 2
        val baseY = size.height - 8.dp.toPx()
        val stroke = Stroke(width = 1.2f.dp.toPx(), cap = StrokeCap.Round)
        val half = 130.dp.toPx()
        drawLine(AppColors.Gold, Offset(cx - half, baseY), Offset(cx - 34.dp.toPx(), baseY), stroke.width)
        drawLine(AppColors.Gold, Offset(cx + 34.dp.toPx(), baseY), Offset(cx + half, baseY), stroke.width)
        listOf(-half, -half + 12.dp.toPx(), half, half - 12.dp.toPx()).forEach { dx ->
            drawCircle(AppColors.Gold, 2.dp.toPx(), Offset(cx + dx, baseY))
        }
        drawCircle(AppColors.Gold, 2.5f.dp.toPx(), Offset(cx - 44.dp.toPx(), baseY))
        drawCircle(AppColors.Gold, 2.5f.dp.toPx(), Offset(cx + 44.dp.toPx(), baseY))
        val w = 22.dp.toPx()
        val h = 26.dp.toPx()
        val arch = Path().apply {
            moveTo(cx - w, baseY)
            lineTo(cx - w, baseY - h * 0.55f)
            quadraticTo(cx, baseY - h * 1.6f, cx + w, baseY - h * 0.55f)
            lineTo(cx + w, baseY)
        }
        drawPath(arch, AppColors.Gold, style = stroke)
        drawLine(AppColors.Gold, Offset(cx, baseY - h * 1.12f), Offset(cx, baseY - h * 1.45f), stroke.width)
        drawCircle(AppColors.Gold, 2.dp.toPx(), Offset(cx, baseY - h * 1.5f))
    }
}

/** سهم رفيع "<" أو ">" مرسوم يدوياً (لا ينقلب مع RTL) */
@Composable
fun Chevron(
    pointsLeft: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Text,
    onClick: (() -> Unit)? = null,
) {
    Canvas(
        modifier
            .size(40.dp)
            .then(if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier),
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val a = 6.dp.toPx()
        val path = Path().apply {
            if (pointsLeft) {
                moveTo(cx + a * 0.6f, cy - a); lineTo(cx - a * 0.6f, cy); lineTo(cx + a * 0.6f, cy + a)
            } else {
                moveTo(cx - a * 0.6f, cy - a); lineTo(cx + a * 0.6f, cy); lineTo(cx - a * 0.6f, cy + a)
            }
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/* ─────────────────────────── البطاقات والأزرار ─────────────────────────── */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    gold: Boolean = false,
    radius: Dp = 22.dp,
    borderColor: Color = if (gold) AppColors.Gold.copy(alpha = 0.85f) else AppColors.CardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.CardLight.copy(alpha = 0.82f), AppColors.Card.copy(alpha = 0.92f)),
                ),
            )
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/** الزر الرئيسي الأخضر الفاتح (حفظ / متابعة / أذّن الآن) */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(AppColors.Green, AppColors.GreenLight, AppColors.Green)))
            .border(1.dp, AppColors.GreenLight.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, fontSize = 21.sp)
    }
}

/** زر بيضاوي بحدود خفيفة (العداد / المفضلة) */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = AppColors.Text,
    gold: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(AppColors.Card.copy(alpha = 0.7f))
            .border(1.dp, if (gold) AppColors.Gold.copy(alpha = 0.7f) else AppColors.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = tint, style = MaterialTheme.typography.titleMedium)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** مربع أيقونة ذهبية بحدود ذهبية (بطاقات "المزيد" و"تحديد الموقع") */
@Composable
fun IconBox(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 56.dp, iconSize: Dp = 26.dp) {
    val shape = RoundedCornerShape(size / 4)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(AppColors.CardLight, AppColors.Card)))
            .border(1.dp, AppColors.Gold.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(iconSize))
    }
}

/** أيقونة قابلة للنقر في الشريط العلوي */
@Composable
fun TopIcon(icon: ImageVector, onClick: () -> Unit, tint: Color = AppColors.Text) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

/**
 * شريط علوي: [عنصر يمين] [عنوان في المنتصف] [عنصر يسار] — المواضع فيزيائية كما في التصميم
 * (القائمة/الرجوع على اليسار دائماً) مهما كان اتجاه اللغة.
 */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rightItem: @Composable () -> Unit = { Spacer(Modifier.size(42.dp)) },
    leftItem: @Composable () -> Unit = { Spacer(Modifier.size(42.dp)) },
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (rtl) rightItem() else leftItem()
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = AppColors.Text, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        if (rtl) leftItem() else rightItem()
    }
}

/** صف الرجوع أعلى الشاشات الفرعية: سهم "<" على اليسار فيزيائياً، وعنصر اختياري على اليمين */
@Composable
fun BackRow(
    onBack: (() -> Unit)?,
    color: Color = AppColors.Text,
    rightItem: @Composable () -> Unit = {},
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val back: @Composable () -> Unit = {
        if (onBack != null) Chevron(pointsLeft = true, color = color, onClick = onBack) else Spacer(Modifier.size(40.dp))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (rtl) rightItem() else back()
        Spacer(Modifier.weight(1f))
        if (rtl) back() else rightItem()
    }
}

/** عنوان الشاشة الكبير مع وصف تحته (اختيار الصوت، الإقامة، الموقع) */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = AppColors.Text, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

/* ─────────────────────────── شريط التنقل السفلي ─────────────────────────── */

enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Outlined.Home),
    ADHKAR(R.string.tab_adhkar, Icons.AutoMirrored.Outlined.MenuBook),
    QIBLA(R.string.tab_qibla, Icons.Outlined.Explore),
    MORE(R.string.tab_more, Icons.Outlined.GridView),
}

@Composable
fun BottomNavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Card.copy(alpha = 0.97f))
            .border(1.dp, AppColors.CardBorder, shape)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().height(78.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { tab -> NavItem(tab, tab == current) { onSelect(tab) } }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) AppColors.ActiveTab else AppColors.TextSecondary
    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val label = stringResource(tab.labelRes)
        Icon(tab.icon, label, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** أيقونة القائمة "☰" الرفيعة كما في التصميم */
@Composable
fun MenuIcon(onClick: () -> Unit) = TopIcon(Icons.Outlined.Menu, onClick)
