package com.mela.ussdrunner.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import com.mela.ussdrunner.domain.UssdEncoder
import com.mela.ussdrunner.domain.UssdValidator
import com.mela.ussdrunner.domain.model.SimPreference

sealed class UssdResult {
    data class DialerLaunched(val request: String, val reason: String) : UssdResult()
    data class Failure(val title: String, val hints: List<String>, val canFallback: Boolean) : UssdResult()
}

class UssdExecutor(
    private val context: Context,
    private val simManager: SimManager,
) {
    /**
     * Places the code with [Intent.ACTION_CALL] so Android shows its own USSD dialog.
     * The reply is not read or parsed inside this app.
     */
    @SuppressLint("MissingPermission")
    fun execute(
        code: String,
        preference: SimPreference,
        settingsDefault: SimPreference,
    ): UssdResult {
        val validation = UssdValidator.validate(code)
        if (validation is UssdValidator.ValidationResult.Invalid) {
            return UssdResult.Failure(
                title = "That USSD code is not valid.",
                hints = listOf(validation.message),
                canFallback = false,
            )
        }
        val normalized = (validation as UssdValidator.ValidationResult.Valid).normalized

        if (!simManager.hasTelephony()) {
            return UssdResult.Failure(
                title = "This device cannot place phone calls.",
                hints = listOf("USSD needs a phone with a SIM and mobile network."),
                canFallback = false,
            )
        }
        if (!PermissionHelper.hasCallPhone(context)) {
            return UssdResult.Failure(
                title = "Call permission is required.",
                hints = listOf(
                    "Allow phone calls so Android can show the USSD dialog.",
                    "You can enable it in Android settings if the prompt was denied.",
                ),
                canFallback = false,
            )
        }

        val sims = simManager.activeSims()
        if (PermissionHelper.hasPhoneState(context) && sims.isEmpty()) {
            return UssdResult.Failure(
                title = "No SIM is available.",
                hints = listOf(
                    "Insert an active SIM.",
                    "Wait until the SIM is registered on the network.",
                    "If you use dual SIM, check that the selected slot is on.",
                ),
                canFallback = false,
            )
        }

        val subscriptionId = simManager.resolveSubscriptionId(preference, settingsDefault)
        return launchDialer(normalized, subscriptionId)
    }

    @SuppressLint("MissingPermission")
    fun launchDialer(code: String, subscriptionId: Int?): UssdResult {
        val uri = Uri.parse(UssdEncoder.encodeForTel(code))
        val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            attachSim(this, subscriptionId)
        }
        return try {
            context.startActivity(callIntent)
            UssdResult.DialerLaunched(
                request = code,
                reason = "Android is showing the system USSD dialog.",
            )
        } catch (_: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(dialIntent)
                UssdResult.DialerLaunched(
                    request = code,
                    reason = "Call permission was not granted, so the dialer was opened. Tap call to send the code. Android will show the reply.",
                )
            } catch (t: Throwable) {
                UssdResult.Failure(
                    title = "Android does not allow this operation.",
                    hints = listOf(
                        t.localizedMessage ?: "No app can handle the USSD dial request.",
                    ) + commonHints(),
                    canFallback = false,
                )
            }
        } catch (t: Throwable) {
            UssdResult.Failure(
                title = "Could not open the system USSD dialog.",
                hints = listOf(t.localizedMessage ?: "Unknown error") + commonHints(),
                canFallback = false,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun attachSim(intent: Intent, subscriptionId: Int?) {
        if (subscriptionId == null) return
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", findHandle(subscriptionId))
        intent.putExtra("com.android.phone.extra.slot", slotIndexFor(subscriptionId))
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subscriptionId)
    }

    @SuppressLint("MissingPermission")
    private fun findHandle(subscriptionId: Int) = try {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val handles = telecom.callCapablePhoneAccounts ?: emptyList()
        handles.firstOrNull { handle ->
            handle.id == subscriptionId.toString() ||
                telecom.getPhoneAccount(handle)?.let { account ->
                    account.extras?.getInt("android.telecom.extra.EXTRA_SUBSCRIPTION_ID", -1) == subscriptionId ||
                        account.accountHandle?.id == subscriptionId.toString()
                } == true
        } ?: handles.firstOrNull()
    } catch (_: Throwable) {
        null
    }

    private fun slotIndexFor(subscriptionId: Int): Int =
        simManager.activeSims().find { it.subscriptionId == subscriptionId }?.slotIndex ?: 0

    private fun commonHints(): List<String> = listOf(
        "Your SIM is active",
        "You have network coverage",
        "The USSD code is correct for your carrier",
        "The selected SIM is available",
    )
}
