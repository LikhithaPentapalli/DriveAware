package com.example.driveaware

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var contactsPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var smsPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            Toast.makeText(
                this,
                if (granted) "Contacts access granted"
                else "Contacts access denied",
                Toast.LENGTH_LONG
            ).show()
        }

        smsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED

            Toast.makeText(
                this,
                if (granted) "SMS permissions granted. Test with a second phone."
                else "SMS permissions not granted. Automatic SMS features may not work.",
                Toast.LENGTH_LONG
            ).show()
        }

        tts = TextToSpeech(this, this)
        val prefs = getSharedPreferences("driveaware", MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DriveAwareHome(
                        initialDriving = prefs.getBoolean("driving", false),
                        initialAutoSms = prefs.getBoolean("auto_sms", false),
                        initialReply = prefs.getString(
                            "sms_reply",
                            "DriveAware auto-reply: I'm driving right now and can't respond safely. I'll reply when stopped."
                        ) ?: "DriveAware auto-reply: I'm driving right now and can't respond safely. I'll reply when stopped.",

                        initialCallSms = prefs.getBoolean("callSmsEnabled", false),
                        initialCallSmsMessage = prefs.getString(
                            "callSmsMessage",
                            "I'm driving right now and can't answer. I'll get back to you when it's safe."
                        ) ?: "I'm driving right now and can't answer. I'll get back to you when it's safe.",

                        onDrivingChanged = { enabled ->
                            prefs.edit().putBoolean("driving", enabled).apply()
                        },
                        onAutoSmsChanged = { enabled ->
                            prefs.edit().putBoolean("auto_sms", enabled).apply()
                        },
                        onReplyChanged = { message ->
                            prefs.edit().putString("sms_reply", message).apply()
                        },
                        onCallSmsChanged = { enabled ->
                            prefs.edit().putBoolean("callSmsEnabled", enabled).apply()
                        },
                        onCallSmsMessageChanged = { message ->
                            prefs.edit().putString("callSmsMessage", message).apply()
                        },
                        onSpeak = { speakGreeting() },
                        onRequestCallScreening = { requestCallScreeningRole() },
                        onRequestSmsPermissions = { requestSmsPermissions() },
                        onRequestContactsPermission = { requestContactsPermission() }
                    )
                }
            }
        }
    }

    private fun requestContactsPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "Contacts access is already granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun requestSmsPermissions() {
        val required = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                    PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            Toast.makeText(
                this,
                "SMS permissions are already granted.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            smsPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (
                roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
            ) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    startActivity(
                        roleManager.createRequestRoleIntent(
                            RoleManager.ROLE_CALL_SCREENING
                        )
                    )
                } else {
                    Toast.makeText(
                        this,
                        "DriveAware already has call-screening access.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Call screening is not available on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "Call screening requires Android 10 or later in this prototype.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.US
        }
    }

    private fun speakGreeting() {
        val message =
            "Hello. This is DriveAware. The person you are calling is currently driving and may not be able to answer safely. Please try again later."
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "driveaware_greeting"
            )
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun DriveAwareHome(
    initialDriving: Boolean,
    initialAutoSms: Boolean,
    initialReply: String,
    initialCallSms: Boolean,
    initialCallSmsMessage: String,
    onDrivingChanged: (Boolean) -> Unit,
    onAutoSmsChanged: (Boolean) -> Unit,
    onReplyChanged: (String) -> Unit,
    onCallSmsChanged: (Boolean) -> Unit,
    onCallSmsMessageChanged: (String) -> Unit,
    onSpeak: () -> Unit,
    onRequestCallScreening: () -> Unit,
    onRequestSmsPermissions: () -> Unit,
    onRequestContactsPermission: () -> Unit
) {
    var driving by remember { mutableStateOf(initialDriving) }
    var autoSms by remember { mutableStateOf(initialAutoSms) }
    var customMessage by remember { mutableStateOf(initialReply) }
    var callSmsEnabled by remember { mutableStateOf(initialCallSms) }
    var callSmsMessage by remember { mutableStateOf(initialCallSmsMessage) }
    var lastEvent by remember { mutableStateOf("Ready to test") }
    val history = remember { mutableStateListOf<String>() }

    fun logEvent(text: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        history.add(0, "$time — $text")
        lastEvent = text
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("DriveAware", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Manual driving protection • Offline-first prototype",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (driving) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (driving) "DRIVING PROTECTION ON" else "DRIVING PROTECTION OFF",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Driving Protection", modifier = Modifier.weight(1f))
                    Switch(
                        checked = driving,
                        onCheckedChange = {
                            driving = it
                            onDrivingChanged(it)
                            logEvent(
                                if (it) "Driving Protection enabled"
                                else "Driving Protection disabled"
                            )
                        }
                    )
                }
                Text(
                    "Turn this on before driving. No automatic driving detection is used.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("Incoming calls", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = {
                onRequestCallScreening()
                logEvent("Requested Android call-screening role")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Call Screening")
        }
        Text(
            "When Driving Protection is ON, the service requests silent ringing, not rejection. Android's role must be granted, and behavior needs testing on this OnePlus phone.",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Call-triggered SMS", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Text caller when protection is ON",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = callSmsEnabled,
                onCheckedChange = {
                    callSmsEnabled = it
                    onCallSmsChanged(it)
                    logEvent(
                        if (it) "Call-triggered SMS enabled"
                        else "Call-triggered SMS disabled"
                    )
                }
            )
        }
        OutlinedTextField(
            value = callSmsMessage,
            onValueChange = {
                callSmsMessage = it
                onCallSmsMessageChanged(it)
            },
            label = { Text("Message to callers") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(
            onClick = {
                onRequestContactsPermission()
                logEvent("Requested Contacts permission")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Screening for Saved Contacts")
        }

        Text("Automatic SMS reply", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable automatic replies", modifier = Modifier.weight(1f))
            Switch(
                checked = autoSms,
                onCheckedChange = {
                    autoSms = it
                    onAutoSmsChanged(it)
                    logEvent(
                        if (it) "Automatic SMS replies enabled in settings"
                        else "Automatic SMS replies disabled"
                    )
                }
            )
        }
        OutlinedTextField(
            value = customMessage,
            onValueChange = {
                customMessage = it
                onReplyChanged(it)
            },
            label = { Text("Automatic reply message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(
            onClick = {
                onRequestSmsPermissions()
                logEvent("Requested SMS permissions")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant SMS Permissions")
        }
        Text(
            "Incoming-message replies and call-triggered texts use separate switches and messages. SMS sending requires permission and active cellular service.",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Local voice demo", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = {
                if (driving) {
                    onSpeak()
                    logEvent("Local voice demo played on this phone")
                } else {
                    logEvent("Voice demo test: turn Driving Protection on first")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Sample Voice Message")
        }
        Text(
            "This is only a local demonstration. It does not play audio to a caller.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Latest status", style = MaterialTheme.typography.titleMedium)
                Text(lastEvent)
            }
        }

        Text("Test log", style = MaterialTheme.typography.titleMedium)
        if (history.isEmpty()) {
            Text("No test events yet.")
        } else {
            history.take(8).forEach {
                Text("• $it")
            }
        }

        Text(
            "Not yet verified on a physical device. Call screening and SMS delivery must be tested with a second phone and an active SIM.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
