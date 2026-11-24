package ai.ai4me.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Ai4Me Semantic Color Tokens (Document 3 §2.1)
 * Restrained, intelligent, private palette avoiding generic purple/blue gradients.
 */
@Immutable
data class Ai4MeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val elevatedSurface: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val outline: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    // Voice & Agent State Tokens (Doc 3 §2.1)
    val listening: Color,
    val processing: Color,
    val speaking: Color,
    val disabled: Color,
    val isDark: Boolean
)

val DarkAi4MeColors = Ai4MeColors(
    background = Color(0xFF0F141C),
    surface = Color(0xFF161E2E),
    surfaceVariant = Color(0xFF1E293B),
    elevatedSurface = Color(0xFF26334A),
    primary = Color(0xFF38BDF8), // Crisp technical sky-blue accent
    onPrimary = Color(0xFF0B192C),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    outline = Color(0xFF334155),
    divider = Color(0xFF1E293B),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    error = Color(0xFFEF4444),
    listening = Color(0xFF06B6D4), // Cyan focused listening
    processing = Color(0xFFF59E0B), // Warm amber deliberate processing
    speaking = Color(0xFF10B981), // Emerald calm speaking
    disabled = Color(0xFF475569),
    isDark = true
)

val LightAi4MeColors = Ai4MeColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    elevatedSurface = Color(0xFFE2E8F0),
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0369A1),
    onSecondary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    outline = Color(0xFFCBD5E1),
    divider = Color(0xFFE2E8F0),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    error = Color(0xFFDC2626),
    listening = Color(0xFF0891B2),
    processing = Color(0xFFD97706),
    speaking = Color(0xFF059669),
    disabled = Color(0xFF94A3B8),
    isDark = false
)

val LocalAi4MeColors = staticCompositionLocalOf { DarkAi4MeColors }
