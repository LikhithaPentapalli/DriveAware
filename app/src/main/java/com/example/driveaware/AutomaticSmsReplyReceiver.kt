package com.example.driveaware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.provider.Telephony

/** Best-effort SMS_RECEIVED implementation. Actual permission grants vary by Android/installer/policy. */
class AutomaticSmsReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("driveaware", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("driving", false) || !prefs.getBoolean("auto_sms", false)) return

        val messages = try { Telephony.Sms.Intents.getMessagesFromIntent(intent) }
        catch (_: Exception) { return }
        if (messages.isNullOrEmpty()) return

        val sender = messages.firstOrNull()?.originatingAddress?.trim().orEmpty()
        if (sender.isBlank()) return
        val incomingBody = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val reply = prefs.getString("sms_reply", DEFAULT_REPLY)?.trim().orEmpty()
        if (reply.isBlank()) return
        // Avoid responding to our own recognizable automatic reply and creating a reply loop.
        if (incomingBody.contains("DriveAware auto-reply:", ignoreCase = true)) return

        val now = System.currentTimeMillis()
        val lastKey = "last_auto_reply_${sender.hashCode()}"
        val lastSent = prefs.getLong(lastKey, 0L)
        if (now - lastSent < REPLY_COOLDOWN_MS) return

        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(reply)
            if (parts.size > 1) smsManager.sendMultipartTextMessage(sender, null, parts, null, null)
            else smsManager.sendTextMessage(sender, null, reply, null, null)
            prefs.edit()
                .putLong(lastKey, now)
                .putString("last_sms_status", "Automatic reply submitted to SMS service for $sender")
                .apply()
        } catch (e: Exception) {
            prefs.edit().putString("last_sms_status", "Automatic reply failed: ${e.javaClass.simpleName}").apply()
        }
    }

    companion object {
        private const val REPLY_COOLDOWN_MS = 30 * 60 * 1000L
        private const val DEFAULT_REPLY = "DriveAware auto-reply: I'm driving right now and can't respond safely. I'll reply when stopped."
    }
}
