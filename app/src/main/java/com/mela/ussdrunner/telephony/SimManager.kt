package com.mela.ussdrunner.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.domain.model.SimSlot

class SimManager(private val context: Context) {

    fun hasTelephony(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun canReadSubscriptions(): Boolean {
        val phoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        val phoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return phoneState || phoneNumbers
    }

    @SuppressLint("MissingPermission")
    fun activeSims(): List<SimSlot> {
        if (!hasTelephony() || !canReadSubscriptions()) return emptyList()
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val list = try {
            manager.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            null
        } ?: return emptyList()
        return list.map { info ->
            SimSlot(
                slotIndex = info.simSlotIndex,
                subscriptionId = info.subscriptionId,
                displayName = info.displayName?.toString()?.ifBlank { "SIM ${info.simSlotIndex + 1}" }
                    ?: "SIM ${info.simSlotIndex + 1}",
                carrierName = info.carrierName?.toString(),
                number = info.number?.takeIf { it.isNotBlank() },
            )
        }.sortedBy { it.slotIndex }
    }

    fun isDualSim(): Boolean = activeSims().size >= 2

    fun resolveSubscriptionId(
        preference: SimPreference,
        fallback: SimPreference = SimPreference.DEFAULT,
    ): Int? {
        val sims = activeSims()
        if (sims.isEmpty()) return null
        val resolved = when (preference) {
            SimPreference.DEFAULT -> when (fallback) {
                SimPreference.DEFAULT -> defaultSubscriptionId()
                else -> resolveSlot(sims, fallback)
            }
            SimPreference.SLOT_0,
            SimPreference.SLOT_1,
            -> resolveSlot(sims, preference)
        }
        return resolved ?: sims.first().subscriptionId
    }

    private fun resolveSlot(sims: List<SimSlot>, preference: SimPreference): Int? {
        val index = when (preference) {
            SimPreference.SLOT_0 -> 0
            SimPreference.SLOT_1 -> 1
            SimPreference.DEFAULT -> return defaultSubscriptionId()
        }
        return sims.find { it.slotIndex == index }?.subscriptionId
            ?: sims.getOrNull(index)?.subscriptionId
    }

    @SuppressLint("MissingPermission")
    private fun defaultSubscriptionId(): Int? {
        val id = SubscriptionManager.getDefaultSubscriptionId()
        return id.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
            ?: activeSims().firstOrNull()?.subscriptionId
    }

    @SuppressLint("MissingPermission")
    fun telephonyFor(subscriptionId: Int?): TelephonyManager {
        val base = context.getSystemService(TelephonyManager::class.java)
        return if (subscriptionId != null && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            base.createForSubscriptionId(subscriptionId)
        } else {
            base
        }
    }
}
