package ai.ai4me.automation.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceBasedActionVerifierTest {

    @Test
    fun `dispatched action without return confirmation produces unverified status`() {
        // Document 1 §3.4: "If delivery cannot be verified, report 'sent action attempted; delivery unverified,' not 'delivered.'"
        val result = EvidenceBasedActionVerifier.verifyDelivery(
            actionName = "send_sms",
            isIntentDispatched = true,
            observableConfirmation = null
        )

        assertTrue(result is ActionVerificationResult.Unverified)
        val unverified = result as ActionVerificationResult.Unverified
        assertEquals("sent action attempted; delivery unverified", unverified.reason)
        assertEquals("send_sms", unverified.attemptedAction)
    }

    @Test
    fun `dispatched action with observable confirmation produces verified status`() {
        val result = EvidenceBasedActionVerifier.verifyDelivery(
            actionName = "open_settings",
            isIntentDispatched = true,
            observableConfirmation = "Settings activity is in foreground"
        )

        assertTrue(result is ActionVerificationResult.Verified)
        val verified = result as ActionVerificationResult.Verified
        assertTrue(verified.evidence.contains("Settings activity is in foreground"))
    }

    @Test
    fun `failed dispatch produces failed result`() {
        val result = EvidenceBasedActionVerifier.verifyDelivery(
            actionName = "launch_unknown",
            isIntentDispatched = false,
            observableConfirmation = null
        )

        assertTrue(result is ActionVerificationResult.Failed)
        val failed = result as ActionVerificationResult.Failed
        assertTrue(failed.failureReason.contains("Dispatch failed"))
    }
}
