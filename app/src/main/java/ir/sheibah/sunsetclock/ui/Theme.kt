package ir.sheibah.sunsetclock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import ir.sheibah.sunsetclock.R

// پالت «غروب»: آسمان شب نیلی + کهربایی گرم
val NightColor = Color(0xFF2B2F6B)
val DayColor = Color(0xFFFFB454)
val DuskColor = Color(0xFFB8A9FF)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB454),
    onPrimary = Color(0xFF3A2000),
    primaryContainer = Color(0xFF5A3A00),
    onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFB8A9FF),
    onSecondary = Color(0xFF2A1F6B),
    secondaryContainer = Color(0xFF3F3585),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = Color(0xFF7FD6C8),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF00504A),
    onTertiaryContainer = Color(0xFFA0F2E3),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE8E6F2),
    surface = Color(0xFF0B1020),
    onSurface = Color(0xFFE8E6F2),
    surfaceVariant = Color(0xFF232A47),
    onSurfaceVariant = Color(0xFFC6C5D6),
    surfaceContainerLowest = Color(0xFF070B17),
    surfaceContainerLow = Color(0xFF10162B),
    surfaceContainer = Color(0xFF151B33),
    surfaceContainerHigh = Color(0xFF1C2340),
    surfaceContainerHighest = Color(0xFF252D4D),
    outline = Color(0xFF8E8FA3),
    outlineVariant = Color(0xFF3B4163)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE67E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF301800),
    secondary = Color(0xFF5B4FC4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF170A5E),
    tertiary = Color(0xFF006A60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA0F2E3),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B22),
    surfaceVariant = Color(0xFFF1F1F6),
    onSurfaceVariant = Color(0xFF6B6B78),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFC),
    surfaceContainer = Color(0xFFF6F6FA),
    surfaceContainerHigh = Color(0xFFF1F1F6),
    surfaceContainerHighest = Color(0xFFEAEAF1),
    outline = Color(0xFF8C8C99),
    outlineVariant = Color(0xFFE4E4EC)
)

/** فونت استعداد (Estedad) با مجوز OFL */
val Estedad = FontFamily(
    Font(R.font.estedad_extralight, FontWeight.ExtraLight),
    Font(R.font.estedad_light, FontWeight.Light),
    Font(R.font.estedad_regular, FontWeight.Normal),
    Font(R.font.estedad_medium, FontWeight.Medium),
    Font(R.font.estedad_bold, FontWeight.Bold),
    Font(R.font.estedad_bold, FontWeight.SemiBold),
    Font(R.font.estedad_bold, FontWeight.ExtraBold)
)

private fun TextStyle.est() = copy(fontFamily = Estedad)

private fun estedadTypography(): Typography {
    val d = Typography()
    return d.copy(
        displayLarge = d.displayLarge.est(), displayMedium = d.displayMedium.est(), displaySmall = d.displaySmall.est(),
        headlineLarge = d.headlineLarge.est(), headlineMedium = d.headlineMedium.est().copy(fontWeight = FontWeight.Bold),
        headlineSmall = d.headlineSmall.est(),
        titleLarge = d.titleLarge.est(), titleMedium = d.titleMedium.est(), titleSmall = d.titleSmall.est(),
        bodyLarge = d.bodyLarge.est(), bodyMedium = d.bodyMedium.est(), bodySmall = d.bodySmall.est(),
        labelLarge = d.labelLarge.est(), labelMedium = d.labelMedium.est(), labelSmall = d.labelSmall.est()
    )
}

/** گرادیان اصلی «غروب» برای دکمه‌ها و تأکیدها */
val SunsetBrush: Brush = Brush.linearGradient(listOf(Color(0xFFFFB347), Color(0xFFFF6F61)))
val SunsetBrushSoft: Brush = Brush.linearGradient(listOf(Color(0xFFFFE2B8), Color(0xFFFFD0C9)))

@Composable
fun SunsetTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = estedadTypography(),
        content = content
    )
}
