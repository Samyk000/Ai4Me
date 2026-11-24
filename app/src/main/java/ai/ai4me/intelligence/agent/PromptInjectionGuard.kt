package ai.ai4me.intelligence.agent

/**
 * Prompt Injection Defense (Document 1 §5.7)
 * Strictly wraps external data (notifications, OCR, accessibility node text, messages)
 * as untrusted observations, guaranteeing they are never parsed as system instructions.
 */
object PromptInjectionGuard {

    fun wrapObservation(source: String, rawContent: String): String {
        val sanitized = rawContent
            .replace("<observation", "&lt;observation")
            .replace("</observation>", "&lt;/observation&gt;")
            .replace("### SYSTEM POLICY", "[FILTERED_INJECTION_ATTEMPT]")
            .replace("### AVAILABLE TOOLS", "[FILTERED_INJECTION_ATTEMPT]")

        return """<observation source="$source" untrusted="true">
$sanitized
</observation>"""
    }

    fun isHighRiskInjection(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("ignore previous instructions") ||
                lower.contains("ignore your rules") ||
                lower.contains("override system policy") ||
                lower.contains("send all contacts") ||
                lower.contains("you are now in developer mode")
    }
}
