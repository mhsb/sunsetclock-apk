package ir.sheibah.sunsetclock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    primary = Color(0xFF9A5B00),
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
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF211A12),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF211A12),
    surfaceVariant = Color(0xFFF2E3D2),
    onSurfaceVariant = Color(0xFF50453A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E2),
    surfaceContainer = Color(0xFFFBEBDA),
    surfaceContainerHigh = Color(0xFFF6E4D1),
    surfaceContainerHighest = Color(0xFFF0DCC8),
    outline = Color(0xFF82746A),
    outlineVariant = Color(0xFFD4C4B4)
)

@Composable
fun SunsetTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
