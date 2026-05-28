package net.luis.jenga

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.combine
import net.luis.jenga.data.repository.SettingsRepository
import net.luis.jenga.domain.model.AppLanguage
import net.luis.jenga.domain.model.ThemeMode
import net.luis.jenga.ui.navigation.AppNavHost
import net.luis.jenga.ui.theme.JengaTheme
import java.util.Locale

private class LocalizedContextWrapper(base: Context, locale: Locale) : ContextWrapper(base) {
    private val localizedResources: Resources = run {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        base.createConfigurationContext(config).resources
    }
    override fun getResources(): Resources = localizedResources
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as JengaApp
        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            data class AppSettings(
                val themeMode: ThemeMode = ThemeMode.SYSTEM,
                val dynamicColors: Boolean = true,
                val appLanguage: AppLanguage = AppLanguage.SYSTEM
            )

            val settingsFlow = remember {
                combine(settingsRepo.themeMode, settingsRepo.dynamicColors, settingsRepo.appLanguage) { theme, dynamic, lang ->
                    AppSettings(theme, dynamic, lang)
                }
            }
            val settings by settingsFlow.collectAsState(initial = AppSettings())

            val baseContext = LocalContext.current
            val localizedContext = remember(settings.appLanguage) {
                when (settings.appLanguage) {
                    AppLanguage.SYSTEM -> baseContext
                    AppLanguage.ENGLISH -> LocalizedContextWrapper(baseContext, Locale.ENGLISH)
                    AppLanguage.GERMAN -> LocalizedContextWrapper(baseContext, Locale.GERMAN)
                }
            }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> null
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                JengaTheme(
                    darkTheme = darkTheme ?: isSystemInDarkTheme(),
                    dynamicColor = settings.dynamicColors
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController, app = app)
                }
            }
        }
    }
}
