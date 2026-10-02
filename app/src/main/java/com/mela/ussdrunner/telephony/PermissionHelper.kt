package com.mela.ussdrunner.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {
    val runtimePermissions: List<String> = buildList {
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Manifest.permission.READ_PHONE_NUMBERS)
        }
    }

    fun missing(context: Context): List<String> =
        runtimePermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun hasCallPhone(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPhoneState(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun appSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun rationaleFor(permission: String): String = when (permission) {
        Manifest.permission.CALL_PHONE ->
            "Call permission lets Mela send the USSD code through the phone’s telephony system."
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        ->
            "Phone state permission is used only to list SIM cards so you can pick SIM 1 or SIM 2."
        else -> "This permission is required to run USSD codes."
    }
}
