package com.example.studyapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SereneStudyColorScheme = lightColorScheme(
    primary              = ScPrimary,
    onPrimary            = ScOnPrimary,
    primaryContainer     = ScPrimaryContainer,
    onPrimaryContainer   = ScOnPrimaryContainer,
    secondary            = ScSecondary,
    onSecondary          = ScOnSecondary,
    secondaryContainer   = ScSecondaryContainer,
    onSecondaryContainer = ScOnSecondaryContainer,
    tertiary             = ScTertiary,
    onTertiary           = ScOnTertiary,
    tertiaryContainer    = ScTertiaryContainer,
    onTertiaryContainer  = ScOnTertiaryContainer,
    error                = ScError,
    onError              = ScOnError,
    errorContainer       = ScErrorContainer,
    onErrorContainer     = ScOnErrorContainer,
    background           = ScBackground,
    onBackground         = ScOnBackground,
    surface              = ScSurface,
    onSurface            = ScOnSurface,
    surfaceVariant       = ScSurfaceVariant,
    onSurfaceVariant     = ScOnSurfaceVariant,
    outline              = ScOutline,
    outlineVariant       = ScOutlineVariant,
    inverseSurface       = Color(0xFF2E3131),
    inverseOnSurface     = Color(0xFFEFF1F1),
    inversePrimary       = ScPrimaryFixedDim,
)

@Composable
fun StudyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SereneStudyColorScheme,
        typography = Typography,
        content = content
    )
}
