package ai.ai4me.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyAuditValidatorTest {

    @Test
    fun `privacy audit confirms 100 percent offline and zero telemetry enforcement`() {
        val report = PrivacyAuditValidator.performAudit()

        assertTrue(report.zeroCloudInferenceEnforced)
        assertTrue(report.zeroAnalyticsTelemetryEnforced)
        assertTrue(report.promptInjectionProtectionEnforced)
        assertTrue(report.offlineAirplaneModeCompliant)
        assertEquals(4, report.totalSecurityChecksPassed)
        assertTrue(report.auditSummary.contains("100% offline"))
    }
}
