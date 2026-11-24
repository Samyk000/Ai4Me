package ai.ai4me.automation.verification

sealed class ActionVerificationResult {
    data class Verified(
        val evidence: String,
        val detailsJson: String = "{}"
    ) : ActionVerificationResult()

    data class Unverified(
        val attemptedAction: String,
        val reason: String
    ) : ActionVerificationResult()

    data class Failed(
        val failureReason: String
    ) : ActionVerificationResult()
}

/**
 * Evidence-Based Action Verifier (Document 1 §3.4, §6 & §10)
 * Prohibits false completion:
 * - If delivery receipt/confirmation is observable -> Verified
 * - If action dispatch was attempted without return confirmation -> "sent action attempted; delivery unverified"
 * - If action failed or crashed -> Failed
 */
object EvidenceBasedActionVerifier {

    fun verifyDelivery(
        actionName: String,
        isIntentDispatched: Boolean,
        observableConfirmation: String? = null
    ): ActionVerificationResult {
        if (!isIntentDispatched) {
            return ActionVerificationResult.Failed("Dispatch failed: Intent could not be handled by system.")
        }

        return if (!observableConfirmation.isNullOrBlank()) {
            ActionVerificationResult.Verified(
                evidence = "Observed confirmation: $observableConfirmation"
            )
        } else {
            // Document 1 §3.4 strict requirement:
            // "If delivery cannot be verified, report 'sent action attempted; delivery unverified,' not 'delivered.'"
            ActionVerificationResult.Unverified(
                attemptedAction = actionName,
                reason = "sent action attempted; delivery unverified"
            )
        }
    }
}
