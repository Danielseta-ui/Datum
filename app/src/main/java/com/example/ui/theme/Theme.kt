package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val BoldTypographyColorScheme =
  lightColorScheme(
    primary = M3Primary,
    secondary = M3Secondary,
    tertiary = M3Tertiary,
    background = M3Background,
    surface = M3Surface,
    onPrimary = M3OnPrimary,
    onSecondary = M3OnSecondary,
    onBackground = M3OnBackground,
    onSurface = M3OnSurface,
    surfaceVariant = M3SurfaceVariant,
    onSurfaceVariant = M3Secondary,
    outline = M3Outline,
    error = Color(0xFFBA1A1A)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Set default to false to reflect light theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = BoldTypographyColorScheme // Enforce our custom bold financial palette!

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
