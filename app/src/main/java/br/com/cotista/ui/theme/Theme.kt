package br.com.cotista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF080C0B)
    val Surface = Color(0xFF111816)
    val Surface2 = Color(0xFF18211E)
    val Surface3 = Color(0xFF212C28)
    val Stroke = Color(0xFF1F2A26)
    val Mint = Color(0xFF3DDC97)
    val MintDeep = Color(0xFF0E3B2A)
    val Amber = Color(0xFFF5B841)
    val AmberDeep = Color(0xFF3A2C0C)
    val Coral = Color(0xFFFF6B6B)
    val Sky = Color(0xFF6EA8FE)
    val Lilac = Color(0xFFB69CFF)
    val Text = Color(0xFFEAF2EE)
    val Text2 = Color(0xFF9DB0A7)
    val Text3 = Color(0xFF62746B)

    val palette = listOf(Mint, Sky, Amber, Lilac, Coral, Color(0xFF4FD1C5), Color(0xFFF08BD0), Color(0xFFA3E635), Color(0xFFFFA26B))

    fun forKey(key: String): Color = palette[(key.hashCode() and 0x7fffffff) % palette.size]
    fun signed(v: Double?): Color = when { v == null -> Text2; v > 0.00001 -> Mint; v < -0.00001 -> Coral; else -> Text2 }
}

private val scheme = darkColorScheme(
    primary = C.Mint,
    onPrimary = Color(0xFF00281A),
    primaryContainer = C.MintDeep,
    onPrimaryContainer = C.Mint,
    secondary = C.Amber,
    onSecondary = Color(0xFF2A1E00),
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Bg,
    onSurface = C.Text,
    surfaceVariant = C.Surface2,
    onSurfaceVariant = C.Text2,
    surfaceContainerLowest = C.Bg,
    surfaceContainerLow = C.Surface,
    surfaceContainer = C.Surface,
    surfaceContainerHigh = C.Surface2,
    surfaceContainerHighest = C.Surface3,
    outline = C.Stroke,
    outlineVariant = C.Stroke,
    error = C.Coral,
)

private val base = Typography()

val Num = TextStyle(fontFeatureSettings = "tnum")

private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = "tnum"),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = "tnum"),
    bodySmall = base.bodySmall.copy(fontFeatureSettings = "tnum"),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

@Composable
fun CotistaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
