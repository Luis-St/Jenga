package net.luis.jenga.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Orange70,
    onPrimary = Orange10,
    primaryContainer = Orange40,
    onPrimaryContainer = Orange90,
    inversePrimary = Orange60,
    secondary = Gray80,
    onSecondary = Gray20,
    secondaryContainer = Gray40,
    onSecondaryContainer = Gray90,
    tertiary = Gold80,
    onTertiary = Gold20,
    tertiaryContainer = Gold30,
    onTertiaryContainer = Gold90,
    background = Neutral20,
    onBackground = OnSurfaceDark,
    surface = Neutral20,
    onSurface = OnSurfaceDark,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Orange70,
    surfaceDim = Neutral10,
    surfaceBright = NeutralBright,
    surfaceContainerLowest = Neutral6,
    surfaceContainerLow = Neutral12,
    surfaceContainer = Neutral20,
    surfaceContainerHigh = Neutral24,
    surfaceContainerHighest = Neutral30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color.Black,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90
)

private val LightColorScheme = lightColorScheme(
    primary = Orange60,
    onPrimary = Color.White,
    primaryContainer = Orange90,
    onPrimaryContainer = Orange10,
    inversePrimary = Orange80,
    secondary = Gray50,
    onSecondary = Color.White,
    secondaryContainer = Gray90,
    onSecondaryContainer = Gray30,
    tertiary = Gold40,
    onTertiary = Color.White,
    tertiaryContainer = Gold90,
    onTertiaryContainer = Gold10,
    background = Neutral98,
    onBackground = OnSurfaceLight,
    surface = Surface98,
    onSurface = OnSurfaceLight,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant20,
    surfaceTint = Orange60,
    surfaceDim = SurfaceDimLight,
    surfaceBright = Surface98,
    surfaceContainerLowest = Neutral99,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10
)

@Composable
fun JengaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}