package com.example.studyapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Serene Study — exact Stitch palette ──────────────────────────────────────

// Primary: Soft Mint Green
val ScPrimary            = Color(0xFF3F665C)
val ScOnPrimary          = Color(0xFFFFFFFF)
val ScPrimaryContainer   = Color(0xFFB8E2D6)
val ScOnPrimaryContainer = Color(0xFF3F665C)
val ScPrimaryFixed       = Color(0xFFC1EBDF)
val ScPrimaryFixedDim    = Color(0xFFA6CFC3)

// Secondary: Lavender
val ScSecondary            = Color(0xFF5E5B7A)
val ScOnSecondary          = Color(0xFFFFFFFF)
val ScSecondaryContainer   = Color(0xFFDED9FD)
val ScOnSecondaryContainer = Color(0xFF605D7C)

// Tertiary: Pale Blue-Grey
val ScTertiary            = Color(0xFF4D616E)
val ScOnTertiary          = Color(0xFFFFFFFF)
val ScTertiaryContainer   = Color(0xFFC6DCEB)
val ScOnTertiaryContainer = Color(0xFF4D616E)

// Error
val ScError          = Color(0xFFBA1A1A)
val ScOnError        = Color(0xFFFFFFFF)
val ScErrorContainer = Color(0xFFFFDAD6)
val ScOnErrorContainer = Color(0xFF93000A)

// Surface system
val ScBackground             = Color(0xFFF8FAFA)
val ScOnBackground           = Color(0xFF191C1D)
val ScSurface                = Color(0xFFF8FAFA)
val ScOnSurface              = Color(0xFF191C1D)
val ScSurfaceContainerLowest = Color(0xFFFFFFFF)
val ScSurfaceContainerLow    = Color(0xFFF2F4F4)
val ScSurfaceContainer       = Color(0xFFECEEEE)
val ScSurfaceContainerHigh   = Color(0xFFE6E8E9)
val ScSurfaceContainerHighest= Color(0xFFE1E3E3)
val ScSurfaceVariant         = Color(0xFFE1E3E3)
val ScOnSurfaceVariant       = Color(0xFF414846)
val ScOutline                = Color(0xFF717976)
val ScOutlineVariant         = Color(0xFFC0C8C5)

// Text aliases
val ScTextPrimary   = ScOnSurface
val ScTextSecondary = ScOnSurfaceVariant
val ScTextMuted     = ScOutline

// Semantic
val ScSuccess      = ScPrimary
val ScSuccessLight = ScPrimaryContainer
val ScWarning      = Color(0xFFB8860B)
val ScWarningLight = Color(0xFFFFF3CD)

// Priority
val PriorityNormal = ScOutlineVariant
val PriorityMedium = Color(0xFFB8860B)
val PriorityHigh   = ScError

// Note card backgrounds (pastel tints matching Stitch)
val NoteColors = listOf(
    ScSurfaceContainerLowest,          // White
    Color(0xFFEAF5F1),                 // Mint tint (primary-container/30)
    Color(0xFFF0EDFF),                 // Lavender tint (secondary-container/40)
    Color(0xFFE8F3F9),                 // Blue tint (tertiary-container/40)
    Color(0xFFFFF8E1),                 // Warm sand
    Color(0xFFF5F0FF),                 // Soft purple
)

// Legacy aliases for DeckDetailScreen compatibility
val BrandPrimary     = ScPrimary
val BrandPrimaryDark = Color(0xFF274E45)
val BrandSecondary   = ScTertiary
val BrandTertiary    = ScSecondary
val KMABlueLight     = ScPrimary
val KMABlueDark      = Color(0xFF274E45)
val KMAAccent        = ScTertiary
val KMAAccentDark    = Color(0xFF354955)
val DarkBackground   = ScBackground
val DarkSurface      = ScSurfaceContainerLowest
val DarkCard         = ScSurfaceContainerLowest
val DarkCardElevated = ScSurfaceContainerLow
val DarkBorder       = ScOutlineVariant
val DarkBorderLight  = ScOutline
val TextPrimary      = ScOnSurface
val TextSecondary    = ScOnSurfaceVariant
val TextMuted        = ScOutline
val SuccessGreen     = ScPrimary
val WarningAmber     = ScWarning
val DangerRed        = ScError
val InfoBlue         = ScTertiary

// Calendar dots
val DotTodo         = ScPrimary
val DotHighPriority = ScError
val DotCompleted    = ScPrimaryFixedDim

// Shared surface aliases used across screens
val ScBorder     = ScOutlineVariant
val ScBorderMid  = ScOutline
val ScSurfaceVar = ScSurfaceContainerLow
