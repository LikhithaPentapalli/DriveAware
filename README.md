# DriveAware

DriveAware is an Android application prototype designed to help reduce distractions from incoming calls and messages while driving or riding.

The app provides a manual Driving Protection mode and configurable automatic SMS replies. It is being developed and tested as a personal Android project.

## Features

- **Manual Driving Protection:** Turn protection ON or OFF when needed.
- **Incoming-call silencing:** Requests that Android silence supported incoming calls while protection is enabled, without intentionally rejecting them.
- **Automatic SMS replies:** Sends a configurable response to incoming SMS messages when the relevant settings are enabled.
- **Custom reply messages:** Configure the message used for automatic SMS replies.
- **Optional caller SMS:** Includes an option to send a message to incoming callers, subject to Android and device restrictions.
- **Contact-related settings:** Includes an option to configure access to saved contacts and call-screening-related functionality.

**Project status:** Experimental prototype. Not all features have been verified across devices. Call silencing, caller SMS, and permission behaviour may vary by Android version and manufacturer.

## Technology Stack

- **Language:** Kotlin
- **Platform:** Android
- **Development environment:** Android Studio
- **Build system:** Gradle with Kotlin DSL
- **Android APIs:** Call screening, SMS handling, contacts, and runtime permissions

## Project Structure

```text
app/
└── src/main/
    ├── AndroidManifest.xml
    └── java/com/example/driveaware/
        ├── MainActivity.kt
        ├── DrivingCallScreeningService.kt
        └── AutomaticSmsReplyReceiver.kt
```

- `MainActivity.kt` — Main screen, protection controls, and settings.
- `DrivingCallScreeningService.kt` — Handles supported incoming-call screening requests and optional caller SMS replies.
- `AutomaticSmsReplyReceiver.kt` — Processes incoming SMS broadcasts and sends configured automatic replies.
- `AndroidManifest.xml` — Declares application components and Android permissions.

## Requirements

- Android Studio
- A compatible Android SDK and JDK configured through Android Studio
- An Android device compatible with the project's SDK configuration
- SIM service for testing SMS functionality

## Getting Started

### 1. Clone the repository

Replace `YOUR_GITHUB_USERNAME` with your GitHub username.

```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/DriveAware.git
```

### 2. Open the project

1. Open Android Studio.
2. Select **Open**.
3. Choose the cloned `DriveAware` folder.
4. Allow Gradle to sync and download any required dependencies.

### 3. Build and run

1. Connect a compatible Android phone using USB debugging, or configure an emulator.
2. Select the target device in Android Studio.
3. Click **Run** to build and install the application.

Alternatively, use Android Studio's **Build > Build APK(s)** option to create a debug APK for local testing.

## Initial Setup and Testing

1. Open DriveAware.
2. Review the permissions requested by the app.
3. Enable the relevant permissions and call-screening role where required and supported.
4. Configure the automatic SMS reply options.
5. Enable Driving Protection while safely parked.
6. Use another phone to test incoming calls and SMS messages.
7. Disable protection after testing.

## Set Up Call Access

### Step 1: Check the Default Phone App

1. Open your phone's **Settings**.
2. Tap **Apps**.
3. Tap **Default apps**.
4. Select **Phone app** or **Default caller app**.
5. If **DriveAware** appears in the list, select it and confirm.
6. If DriveAware does not appear, leave your existing Phone app selected and continue to Step 2.

### Step 2: Enable Call Screening

1. Open **DriveAware**.
2. Tap **Allow Screening for Saved Contacts** or the call-screening setup button.
3. Follow the instructions displayed by Android.
4. If asked to grant call-screening access, select **DriveAware** and allow access.
5. Return to DriveAware.

If your phone says DriveAware already has call-screening access, no further action is needed for this step.

**Note:** DriveAware uses Android's Call Screening feature and may not be available as the default Phone app. Call-screening settings and wording vary by device and Android version.

Test calls and SMS messages only while parked. Do not interact with the phone while driving or riding.

## Permissions and Privacy

The current manifest declares the following permissions:

| Permission | Intended purpose |
|---|---|
| `READ_PHONE_STATE` | Phone-state-related functionality, where required |
| `RECEIVE_SMS` | Receive incoming SMS broadcasts |
| `READ_CONTACTS` | Support contact-related functionality |
| `SEND_SMS` | Send automatic SMS replies |

The application also declares an Android `CallScreeningService` for supported incoming-call management.

These permissions grant access to sensitive functionality. Review the source code and grant only the permissions required for the features you use.

The published source should be reviewed to determine what data is stored, how long it is retained, and whether any data is transmitted outside the device. Do not enter sensitive personal information into experimental builds.

## Compatibility and Limitations

- Initial testing has been performed on a OnePlus device.
- Automatic replies to incoming SMS messages have been tested successfully.
- Incoming-call silencing and caller SMS behaviour require further compatibility testing.
- Android permissions, call-screening roles, SMS restrictions, carrier conditions, and manufacturer-specific behaviour can affect functionality.
- Successful SMS submission does not guarantee delivery.
- This prototype is not a substitute for safe driving practices.

## Security Notice

This project uses sensitive SMS and phone-related permissions. Review Android and Google Play Protect warnings carefully.

The debug APK is not intended for public distribution at this stage. Public installation builds will be considered after further security review and compatibility testing. Never disable device security protections to install an experimental build.

## Future Improvements

- Test call-silencing behaviour across different Android devices.
- Improve error handling and SMS status reporting.
- Review and minimize required permissions.
- Improve setup instructions and user-facing permission explanations.
- Add screenshots and documented test results.

## Contributing and Feedback

Bug reports and suggestions are welcome. When reporting a problem, include the Android version, device model, and steps to reproduce it. Do not include private phone numbers, message contents, or other sensitive personal information.

## License

No open-source license has been selected yet. Unless a license is added, standard copyright restrictions apply; public availability does not automatically grant permission to reuse or redistribute the code.
