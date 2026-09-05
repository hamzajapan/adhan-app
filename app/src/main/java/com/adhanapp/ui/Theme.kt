package com.adhanapp.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** لوحة ألوان التصميم: أخضر داكن عميق + ذهبي + أخضر فاتح للأزرار */
object AppColors {
    val BgTop = Color(0xFF0C2B26)
    val BgMid = Color(0xFF051C19)
    val BgBottom = Color(0xFF02100F)
    val Card = Color(0xFF07231F)
    val CardLight = Color(0xFF0C2E28)
    val CardBorder = Color(0xFF1B3B34)
    val Gold = Color(0xFFD9B55C)
    val GoldLight = Color(0xFFEBD59A)
    val GoldDim = Color(0xFF9E7F35)
    val Green = Color(0xFF6FAF5C)
    val GreenLight = Color(0xFF8FCB78)
    val GreenDark = Color(0xFF4E8A42)
    val ActiveTab = Color(0xFFA3C95A)
    val HighlightRow = Color(0xFF0E3A2C)
    val Text = Color(0xFFEDEBE6)
    val TextSecondary = Color(0xFFAEBDB5)
    val TextMuted = Color(0xFF7C918A)
    val Danger = Color(0xFFE07A6A)
}

private val Scheme = darkColorScheme(
    primary = AppColors.Green,
    onPrimary = Color.White,
    primaryContainer = AppColors.GreenDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Gold,
    onSecondary = Color(0xFF1B1400),
    secondaryContainer = AppColors.CardLight,
    onSecondaryContainer = AppColors.GoldLight,
    tertiary = AppColors.GoldLight,
    background = AppColors.BgBottom,
    onBackground = AppColors.Text,
    surface = AppColors.Card,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.CardLight,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.CardBorder,
    error = AppColors.Danger,
)

/**
 * خط التطبيق: يُحمَّل Cairo من assets/fonts/cairo.ttf إن وُجد (خط متغيّر الوزن)،
 * وإلا يُستخدم خط النظام.
 */
fun appFontFamily(context: Context): FontFamily {
    val exists = runCatching { context.assets.list("fonts")?.contains("cairo.ttf") == true }
        .getOrDefault(false)
    if (!exists) return FontFamily.Default
    fun f(w: FontWeight) = Font(
        path = "fonts/cairo.ttf",
        assetManager = context.assets,
        weight = w,
        variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
    )
    return FontFamily(f(FontWeight.Normal), f(FontWeight.Medium), f(FontWeight.SemiBold), f(FontWeight.Bold))
}

fun appTypography(font: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 68.sp),
    displayMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 54.sp),
    displaySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun AdhanAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val typography = remember { appTypography(appFontFamily(context.applicationContext)) }
    MaterialTheme(colorScheme = Scheme, typography = typography, content = content)
}
