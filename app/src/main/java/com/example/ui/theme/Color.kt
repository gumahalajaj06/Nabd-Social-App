package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial Human-Crafted "Warm Alabaster & Deep Ink" Palette
val PulseTealLight = Color(0xFF0D9488)
val PulseTealDark = Color(0xFF2DD4BF)
val PulseTealContainerLight = Color(0xFFE6F6F4)
val PulseTealContainerDark = Color(0xFF123532)

val InkPrimaryLight = Color(0xFF121212)
val InkSecondaryLight = Color(0xFF4A4A48)
val InkMutedLight = Color(0xFF6B6A66)
val AlabasterBg = Color(0xFFFAF9F6)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceVariant = Color(0xFFF3F1EC)
val HairlineLight = Color(0xFFE6E2D8)

val IvoryPrimaryDark = Color(0xFFF3F4F6)
val IvorySecondaryDark = Color(0xFF9CA3AF)
val ObsidianBg = Color(0xFF0B0D10)
val SlateSurface = Color(0xFF111419)
val SlateSurfaceVariant = Color(0xFF191E26)
val HairlineDark = Color(0xFF232832)

// Semantic Status & Verification Badges
val VerifiedPersonalBlue = Color(0xFF1D9BF0)
val VerifiedBusinessGold = Color(0xFFEAB308)
val VerifiedGovernmentSlate = Color(0xFF64748B)
val LikeCoral = Color(0xFFF43F5E)
val RepostEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorCrimson = Color(0xFFEF4444)
val ReadBlue = Color(0xFF38BDF8)

// Refined Monogram Swatches
val AvatarSwatches = listOf(
    "#0D9488", // Deep Teal
    "#1E40AF", // Editorial Cobalt
    "#4338CA", // Ink Indigo
    "#BE123C", // Carmine
    "#9A3412", // Terracotta
    "#047857", // Forest Emerald
    "#334155", // Slate Charcoal
    "#6D28D9"  // Plum Ink
)

data class StoryBackgroundPreset(
    val id: String,
    val label: String,
    val startHex: String,
    val endHex: String
)

val StoryPresets = listOf(
    StoryBackgroundPreset("ink_teal", "Midnight Ink", "#0B0D10", "#0D9488"),
    StoryBackgroundPreset("emerald_night", "Forest Press", "#064E3B", "#059669"),
    StoryBackgroundPreset("royal_indigo", "Editorial Slate", "#1E1B4B", "#4338CA"),
    StoryBackgroundPreset("crimson_dusk", "Burgundy Ink", "#4C0519", "#BE123C"),
    StoryBackgroundPreset("amber_sand", "Warm Ochre", "#78350F", "#B45309"),
    StoryBackgroundPreset("pure_obsidian", "Pure Charcoal", "#090D16", "#1F2937")
)
