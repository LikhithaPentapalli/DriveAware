
package com.example.driveaware

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SmsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Silences supported incoming calls while Driving Protection is enabled.
 * Optionally sends an SMS to the caller.
 */
class DrivingCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incoming =
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        val prefs = getSharedPreferences("driveaware", MODE_PRIVATE)
        val protectionOn = prefs.getBoolean("driving", false)

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(incoming && protectionOn)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        // Respond to Android once, promptly.
        respondToCall(callDetails, response)

        if (incoming && protectionOn) {
            maybeTextCaller(callDetails.handle?.schemeSpecificPart, prefs)
        }
    }

    private fun maybeTextCaller(
        phoneNumber: String?,
        prefs: android.content.SharedPreferences
    ) {
        if (phoneNumber.isNullOrBlank()) return

        // These preference keys must match the ones used in MainActivity.
        if (!prefs.getBoolean("callSmsEnabled", false)) return

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("DriveAware", "SMS permission not granted")
            return
        }

        val now = System.currentTimeMillis()
        val lastSent = prefs.getLong("callSmsLast_$phoneNumber", 0L)

        // At most one call-triggered SMS per number every 30 minutes.
        if (now - lastSent < 30 * 60 * 1000L) return

        val message = prefs.getString(
            "callSmsMessage",
            "I'm driving right now and can't answer. I'll get back to you when it's safe."
        )?.trim().orEmpty()

        if (message.isBlank()) return

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )

            // Record the attempt only after sendTextMessage returns successfully.
            prefs.edit()
                .putLong("callSmsLast_$phoneNumber", now)
                .apply()

        } catch (e: Exception) {
            Log.e("DriveAware", "Could not send call-triggered SMS", e)
        }
    }
}
