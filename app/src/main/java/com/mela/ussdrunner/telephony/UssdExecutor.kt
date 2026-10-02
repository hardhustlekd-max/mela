package com.mela.ussdrunner.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import com.mela.ussdrunner.domain.UssdEncoder
import com.mela.ussdrunner.domain.UssdValidator
import com.mela.ussdrunner.domain.model.SimPreference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed class UssdResult {
    data class Response(val request: String, val body: String) : UssdResult()
    data class DialerLaunched(val request: String, val reason: String) : UssdResult()
    data class Failure(val title: String, val hints: List<String>, val canFallback: Boolean) : UssdResult()
}

class UssdExecutor(
    private val context: Context,
    private val simManager: SimManager,
) {
    fun nativeApiAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @SuppressLint("MissingPermission")
    suspend fun execute(
        code: String,
        preference: SimPreference,
        settingsDefault: SimPreference,
        preferNative: Boolean = true,
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
                    "Allow phone calls so Mela can send the USSD code.",
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
                canFallback = true,
            )
        }

        val subscriptionId = simManager.resolveSubscriptionId(preference, settingsDefault)

        if (preferNative && nativeApiAvailable()) {
            val native = sendNative(normalized, subscriptionId)
            if (native != null) return native
        }

        return launchDialer(normalized, subscriptionId)
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendNative(code: String, subscriptionId: Int?): UssdResult? {
        val tm = try {
            simManager.telephonyFor(subscriptionId)
        } catch (t: Throwable) {
            return UssdResult.Failure(
                title = "Could not use the selected SIM.",
                hints = listOf(
                    t.localizedMessage ?: "The subscription is not available.",
                    "Try Default SIM, or the other slot.",
                ),
                canFallback = true,
            )
        }

        if (tm.simState != TelephonyManager.SIM_STATE_READY &&
            tm.simState != TelephonyManager.SIM_STATE_UNKNOWN
        ) {
            return UssdResult.Failure(
                title = "The selected SIM is not ready.",
                hints = listOf(
                    "Your SIM may be locked, missing, or still starting up.",
                    "Check that the selected SIM is available.",
                ),
                canFallback = true,
            )
        }

        val timed: UssdResult? = try {
            withTimeoutOrNull(NATIVE_TIMEOUT_MS) {
                suspendCancellableCoroutine<UssdResult> { cont ->
                    val callback = object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager,
                            request: String,
                            response: CharSequence,
                        ) {
                            if (cont.isActive) {
                                cont.resume(UssdResult.Response(request, response.toString()))
                            }
                        }

                        override fun onReceiveUssdResponseFailed(
                            telephonyManager: TelephonyManager,
                            request: String,
                            failureCode: Int,
                        ) {
                            if (!cont.isActive) return
                            val message = when (failureCode) {
                                TelephonyManager.USSD_RETURN_FAILURE ->
                                    "The carrier returned a USSD failure."
                                TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL ->
                                    "USSD service is unavailable on this network."
                                else -> "USSD request failed (code $failureCode)."
                            }
                            cont.resume(
                                UssdResult.Failure(
                                    title = message,
                                    hints = commonHints(),
                                    canFallback = true,
                                ),
                            )
                        }
                    }
                    try {
                        tm.sendUssdRequest(code, callback, Handler(Looper.getMainLooper()))
                    } catch (t: SecurityException) {
                        if (cont.isActive) {
                            cont.resume(
                                UssdResult.Failure(
                                    title = "Android blocked the USSD request.",
                                    hints = listOf(
                                        t.localizedMessage ?: "Missing permission or policy restriction.",
                                        "You can still try the system dialer fallback.",
                                    ),
                                    canFallback = true,
                                ),
                            )
                        }
                    } catch (t: Throwable) {
                        if (cont.isActive) {
                            cont.resume(
                                UssdResult.Failure(
                                    title = "Native USSD is not supported on this device.",
                                    hints = listOf(
                                        t.localizedMessage ?: "sendUssdRequest() could not be started.",
                                        "Mela can open the system dialer instead.",
                                    ),
                                    canFallback = true,
                                ),
                            )
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            UssdResult.Failure(
                title = "USSD request could not be sent.",
                hints = listOf(t.localizedMessage ?: "Unexpected error") + commonHints(),
                canFallback = true,
            )
        }

        return timed ?: UssdResult.Failure(
            title = "USSD request timed out.",
            hints = listOf("The network did not answer in time.") + commonHints(),
            canFallback = true,
        )
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
                reason = "Android opened the system call / USSD interface. The carrier dialog appears outside this app, so Mela cannot read the reply.",
            )
        } catch (_: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(dialIntent)
                UssdResult.DialerLaunched(
                    request = code,
                    reason = "Call permission was not granted, so the dialer was opened instead. Tap the call button to send the code.",
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
                title = "Could not open the dialer.",
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

    companion object {
        const val NATIVE_TIMEOUT_MS = 25_000L
    }
}
