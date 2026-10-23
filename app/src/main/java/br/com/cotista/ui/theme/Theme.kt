package br.com.cotista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.cotista.R

object C {
    val Bg = Color(0xFF06090A)
    val Surface = Color(0xFF0E1413)
    val Surface2 = Color(0xFF151D1B)
    val Surface3 = Color(0xFF1E2926)
    val Stroke = Color(0xFF1A2421)
    val Mint = Color(0xFF3DDC97)
    val Teal = Color(0xFF22B8A7)
    val MintDeep = Color(0xFF0D3527)
    val Amber = Color(0xFFF7BE4F)
    val AmberDeep = Color(0xFF33270C)
    val Coral = Color(0xFFFF6B6B)
    val Sky = Color(0xFF6EA8FE)
    val Lilac = Color(0xFFB69CFF)
    val Text = Color(0xFFF1F6F3)
    val Text2 = Color(0xFFA2B3AB)
    val Text3 = Color(0xFF66776F)

    val palette = listOf(Mint, Sky, Amber, Lilac, Coral, Color(0xFF4FD1C5), Color(0xFFF08BD0), Color(0xFFA3E635), Color(0xFFFFA26B))

    fun forKey(key: String): Color = palette[(key.hashCode() and 0x7fffffff) % palette.size]
    fun signed(v: Double?): Color = when { v == null -> Text2; v > 0.00001 -> Mint; v < -0.00001 -> Coral; else -> Text2 }

    val heroBrush = Brush.linearGradient(listOf(Color(0xFF13694A), Color(0xFF0B3527), Color(0xFF0A1714)))
    val amberBrush = Brush.linearGradient(listOf(Color(0xFF5A4110), Color(0xFF2A200B), Color(0xFF111614)))
    val mintBrush = Brush.horizontalGradient(listOf(Mint, Teal))
    val borderBrush = Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color(0x08FFFFFF)))
}

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

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

private fun s(size: Int, weight: FontWeight, spacing: Double = 0.0, line: Int = (size * 1.35).toInt()) = TextStyle(
    fontFamily = Manrope, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.sp,
    lineHeight = line.sp, fontFeatureSettings = "tnum",
)

val Num = TextStyle(fontFeatureSettings = "tnum")

private val typography = Typography(
    displayLarge = s(44, FontWeight.ExtraBold, -1.6, 50),
    displayMedium = s(40, FontWeight.ExtraBold, -1.4, 46),
    displaySmall = s(34, FontWeight.ExtraBold, -1.2, 40),
    headlineLarge = s(30, FontWeight.ExtraBold, -1.0),
    headlineMedium = s(26, FontWeight.Bold, -0.8),
    headlineSmall = s(22, FontWeight.Bold, -0.5),
    titleLarge = s(19, FontWeight.Bold, -0.3),
    titleMedium = s(16, FontWeight.Bold, -0.2),
    titleSmall = s(14, FontWeight.Bold, -0.1),
    bodyLarge = s(16, FontWeight.Medium),
    bodyMedium = s(14, FontWeight.Medium),
    bodySmall = s(12, FontWeight.Medium),
    labelLarge = s(14, FontWeight.Bold),
    labelMedium = s(12, FontWeight.SemiBold, 0.2),
    labelSmall = s(11, FontWeight.Bold, 0.8),
)

@Composable
fun CotistaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
