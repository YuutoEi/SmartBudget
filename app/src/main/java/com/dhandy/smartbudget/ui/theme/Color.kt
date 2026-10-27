package com.dhandy.smartbudget.ui.theme

import androidx.compose.ui.graphics.Color

// Custom Vibrant Palette (Gen-Z / Modern Style)
val ElectricIndigo = Color(0xFF6366F1)
val CyberTeal = Color(0xFF14B8A6)
val NeonPink = Color(0xFFEC4899)
val CoralRed = Color(0xFFF43F5E)
val NeonPurple = Color(0xFFA855F7)
val NeonOrange = Color(0xFFF97316)
val NeonBlue = Color(0xFF06B6D4)
val EmeraldGreen = Color(0xFF10B981)

// Dark Theme Colors (Deep Slate)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val LightIndigo = Color(0xFF818CF8)
val LightTeal = Color(0xFF2DD4BF)
val LightPink = Color(0xFFF472B6)

// Light Theme Colors (Soft Off-White)
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val DarkIndigo = Color(0xFF4F46E5)

// Helper to generate dynamic credit-card style gradient based on wallet/target name
fun getGradientForName(name: String): List<Color> {
    val gradients = listOf(
        listOf(ElectricIndigo, NeonPurple),
        listOf(CyberTeal, EmeraldGreen),
        listOf(NeonPink, NeonPurple),
        listOf(NeonOrange, CoralRed),
        listOf(NeonBlue, ElectricIndigo)
    )
    val hash = name.hashCode()
    val index = (if (hash < 0) -hash else hash) % gradients.size
    return gradients[index]
}