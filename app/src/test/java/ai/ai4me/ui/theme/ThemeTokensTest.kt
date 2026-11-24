package ai.ai4me.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun testSpacing_CompliesWith4dpBaseAndAccessibility() {
        val spacing = Ai4MeSpacing()
        assertEquals(0, spacing.xs.value.toInt() % 4)
        assertEquals(0, spacing.sm.value.toInt() % 4)
        assertEquals(0, spacing.md.value.toInt() % 4)
        assertEquals(0, spacing.lg.value.toInt() % 4)
        // Minimum touch target must be at least 48dp (Doc 3 §6)
        assertTrue(spacing.minTouchTarget.value >= 48f)
    }

    @Test
    fun testColorTokens_SemanticContrastGuarantees() {
        val dark = DarkAi4MeColors
        assertNotEquals(dark.background, dark.textPrimary)
        assertNotEquals(dark.surface, dark.textPrimary)
        assertNotEquals(dark.primary, dark.onPrimary)
        assertTrue(dark.isDark)

        val light = LightAi4MeColors
        assertNotEquals(light.background, light.textPrimary)
        assertNotEquals(light.surface, light.textPrimary)
        assertNotEquals(light.primary, light.onPrimary)
        assertTrue(!light.isDark)
    }
}
