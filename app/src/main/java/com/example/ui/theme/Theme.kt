package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode(val title: String) {
  SYSTEM("System Default"),
  DARK_SLATE("Deep Slate Dark"),
  AMOLED_BLACK("AMOLED Pure Black"),
  LIGHT("Light")
}

private val DeepDarkColorScheme = darkColorScheme(
  primary = EmeraldLight,
  onPrimary = Slate950,
  primaryContainer = Slate800,
  onPrimaryContainer = EmeraldLight,
  secondary = CyanAccent,
  onSecondary = Slate950,
  secondaryContainer = Slate800,
  onSecondaryContainer = CyanGlow,
  tertiary = Slate400,
  onTertiary = Slate950,
  background = DeepDarkBackground,
  onBackground = Slate100,
  surface = DeepDarkSurface,
  onSurface = Slate100,
  surfaceVariant = DeepDarkSurfaceVariant,
  onSurfaceVariant = Slate200,
  outline = DeepDarkBorder,
  error = SecurityRed,
  onError = Color.White
)

private val AmoledDarkColorScheme = darkColorScheme(
  primary = EmeraldLight,
  onPrimary = PureBlack,
  primaryContainer = PureBlackCard,
  onPrimaryContainer = EmeraldLight,
  secondary = CyanAccent,
  onSecondary = PureBlack,
  secondaryContainer = PureBlackCard,
  onSecondaryContainer = CyanGlow,
  tertiary = Slate400,
  onTertiary = PureBlack,
  background = PureBlack,
  onBackground = Color.White,
  surface = PureBlackCard,
  onSurface = Color.White,
  surfaceVariant = PureBlackCardBorder,
  onSurfaceVariant = Slate200,
  outline = PureBlackCardBorder,
  error = SecurityRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldDark,
  onPrimary = Color.White,
  primaryContainer = EmeraldLight.copy(alpha = 0.2f),
  onPrimaryContainer = Slate950,
  secondary = CyanAccent,
  onSecondary = Color.White,
  secondaryContainer = CyanAccent.copy(alpha = 0.15f),
  onSecondaryContainer = Slate950,
  tertiary = Slate600,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = Slate900,
  surface = LightSurface,
  onSurface = Slate900,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Slate700,
  outline = LightBorder,
  error = SecurityRed,
  onError = Color.White
)

@Composable
fun CipherVaultTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK_SLATE,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.DARK_SLATE -> true
    AppThemeMode.AMOLED_BLACK -> true
    AppThemeMode.LIGHT -> false
  }

  val colorScheme = when (themeMode) {
    AppThemeMode.AMOLED_BLACK -> AmoledDarkColorScheme
    AppThemeMode.DARK_SLATE -> DeepDarkColorScheme
    AppThemeMode.LIGHT -> LightColorScheme
    AppThemeMode.SYSTEM -> if (systemDark) DeepDarkColorScheme else LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
