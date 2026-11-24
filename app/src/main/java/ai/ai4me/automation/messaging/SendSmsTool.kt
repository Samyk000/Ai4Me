package ai.ai4me.automation.messaging

import ai.ai4me.automation.contacts.ContactResolutionResult
import ai.ai4me.automation.contacts.ContactResolver
import ai.ai4me.automation.verification.ActionVerificationResult
import ai.ai4me.automation.verification.EvidenceBasedActionVerifier
import ai.ai4me.intelligence.agent.AgentTool
import ai.ai4me.intelligence.agent.ToolResult
import ai.ai4me.intelligence.agent.ToolRiskLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface SmsDispatcher {
    fun dispatchSms(phoneNumber: String, message: String): Boolean
}

/**
 * Send / Draft SMS Tool (Document 1 §3.4, §6 & Document 2 §7)
 * Enforces:
 * 1. Contact resolution: If more than one match exists, requires user disambiguation ("needs_user_input").
 * 2. Autonomy & Policy: Safe SMS dispatch via intent or default messenger.
 * 3. Evidence-Based Verification: If delivery cannot be confirmed by system receipts,
 *    reports "sent action attempted; delivery unverified", never false "delivered".
 */
class SendSmsTool(
    private val contactResolver: ContactResolver,
    private val smsDispatcher: SmsDispatcher
) : AgentTool {
    override val name: String = "send_sms"
    override val description: String = "Prepares and dispatches an SMS message to a resolved contact. Disambiguates duplicate names before sending."
    override val parameterSchemaJson: String = """{"recipientName":"string","message":"string"}"""
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.PREAUTHORIZED

    override suspend fun execute(argumentsJson: String): ToolResult {
        val json = Json { ignoreUnknownKeys = true }
        val obj = try {
            json.parseToJsonElement(argumentsJson).jsonObject
        } catch (e: Exception) {
            return ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "Invalid JSON parameters: ${e.message}",
                verificationEvidence = "Parameter schema failure"
            )
        }

        val recipientName = obj["recipientName"]?.jsonPrimitive?.content.orEmpty()
        val message = obj["message"]?.jsonPrimitive?.content.orEmpty()

        if (recipientName.isBlank()) {
            return ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "Recipient name cannot be empty.",
                verificationEvidence = "Empty recipient validation"
            )
        }

        // Contact Resolution & Ambiguity Check (Document 1 §3.4)
        val resolution = contactResolver.resolveContact(recipientName)
        val targetContact = when (resolution) {
            is ContactResolutionResult.NotFound -> {
                return ToolResult(
                    success = false,
                    status = "needs_user_input",
                    dataJson = """{"query":"$recipientName"}""",
                    errorMessage = "Contact '$recipientName' could not be found in device address book.",
                    verificationEvidence = "Directory query returned 0 matches"
                )
            }
            is ContactResolutionResult.Ambiguous -> {
                val candidateList = resolution.candidates.joinToString(", ") {
                    "${it.displayName} (${it.phoneNumber})"
                }
                return ToolResult(
                    success = false,
                    status = "needs_user_input",
                    dataJson = """{"query":"$recipientName","candidatesCount":${resolution.candidates.size}}""",
                    errorMessage = "Multiple contacts found for '$recipientName': $candidateList. Please clarify which contact to message.",
                    verificationEvidence = "Ambiguity gate: ${resolution.candidates.size} matches detected"
                )
            }
            is ContactResolutionResult.Resolved -> resolution.contact
        }

        // Dispatch action
        val dispatched = smsDispatcher.dispatchSms(targetContact.phoneNumber, message)

        // Verification check (Document 1 §3.4)
        val verification = EvidenceBasedActionVerifier.verifyDelivery(
            actionName = "send_sms",
            isIntentDispatched = dispatched,
            observableConfirmation = null // Standard SMS intents cannot verify carrier delivery receipts
        )

        return when (verification) {
            is ActionVerificationResult.Verified -> ToolResult(
                success = true,
                status = "completed",
                dataJson = """{"recipient":"${targetContact.displayName}","phone":"${targetContact.phoneNumber}"}""",
                verificationEvidence = verification.evidence
            )
            is ActionVerificationResult.Unverified -> ToolResult(
                success = true,
                status = "unverified",
                dataJson = """{"recipient":"${targetContact.displayName}","phone":"${targetContact.phoneNumber}"}""",
                verificationEvidence = verification.reason // "sent action attempted; delivery unverified"
            )
            is ActionVerificationResult.Failed -> ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = verification.failureReason,
                verificationEvidence = "System dispatch failure"
            )
        }
    }
}
