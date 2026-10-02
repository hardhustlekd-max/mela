# Mela USSD Runner

Native Android app (Kotlin + Jetpack Compose) for saving and running USSD shortcuts offline.

- Package: `com.mela.ussdrunner`
- Min SDK: 26 (Android 8.0)
- Target / compile SDK: 35
- Local Room + DataStore storage (no server, no account)
- Native `TelephonyManager.sendUssdRequest()` with encoded `tel:` dialer fallback
- Dual-SIM selection and Glance home-screen widgets

This repository is **Android only**. It does not include a website or download portal.

## Build locally

Requires JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Signed release (optional): copy `keystore.properties.example` to `keystore.properties`, point it at your keystore, then:

```bash
./gradlew assembleRelease
```

Never commit `*.jks`, `keystore.properties`, or `local.properties`.

## GitHub Actions

Every push and pull request on `main` runs unit tests and publishes a **debug APK** as a workflow artifact (`mela-ussd-runner-apk`).

To also build a **signed release APK**, add these repository secrets:

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 your-release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

Tags matching `v*` attach the APKs to a GitHub Release.

## Permissions

The app requests only:

- `CALL_PHONE` — send USSD
- `READ_PHONE_STATE` / `READ_PHONE_NUMBERS` — list SIM cards on dual-SIM devices

Presets stay on-device. Sample codes are labelled **Example — verify with your carrier**.
