package com.dhandy.smartbudget.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy colors (kept for compatibility — mapped to monochrome / neutrals)
val ElectricIndigo = Color(0xFF000000)
val CyberTeal = Color(0xFF2C2C2C)
val NeonPink = Color(0xFF555555)
val CoralRed = Color(0xFF2C2C2C)
val NeonPurple = Color(0xFF444444)
val NeonOrange = Color(0xFF222222)
val NeonBlue = Color(0xFF333333)
val EmeraldGreen = Color(0xFF111111)

// Pure Monochrome (Hitam & Putih) Palette
val WarmBackground = Color(0xFFFFFFFF)
val WarmCard = Color(0xFFFFFFFF)
val WarmCardSubtle = Color(0xFFF4F4F4)
val WarmYellowCard = Color(0xFFF0F0F0)
val WarmYellowLight = Color(0xFFFAFAFA)
val PrimaryAmber = Color(0xFF000000)
val AccentOrange = Color(0xFF000000)
val DarkNavBrown = Color(0xFF000000)
val MutedTextBrown = Color(0xFF757575)
val ChipSelectedBg = Color(0xFF000000)
val ChipUnselectedBg = Color(0xFFEFEFEF)

// Light Theme Colors (Clean White Minimalist)
val Slate50 = Color(0xFFFFFFFF)
val Slate100 = Color(0xFFF2F2F2)
val DarkIndigo = Color(0xFF000000)

// Dark Theme Colors (Deep Pure Black Minimalist)
val Slate900 = Color(0xFF000000)
val Slate800 = Color(0xFF141414)
val Slate700 = Color(0xFF242424)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkSurfaceVariant = Color(0xFF1E1E1E)
val DarkAccentOrange = Color(0xFFFFFFFF)
val DarkMutedText = Color(0xFF9E9E9E)

val LightIndigo = Color(0xFF333333)
val LightTeal = Color(0xFF888888)
val LightPink = Color(0xFF555555)

// Helper to generate dynamic monochrome tones for cards
fun getGradientForName(name: String): List<Color> {
    val gradients = listOf(
        listOf(Color(0xFF1E1E1E), Color(0xFF0A0A0A)),
        listOf(Color(0xFF2C2C2C), Color(0xFF121212)),
        listOf(Color(0xFF383838), Color(0xFF1C1C1C)),
        listOf(Color(0xFF222222), Color(0xFF080808)),
        listOf(Color(0xFF303030), Color(0xFF141414))
    )
    val hash = name.hashCode()
    val index = (if (hash < 0) -hash else hash) % gradients.size
    return gradients[index]
}