# Minimal UPI Lite

Lightweight Android app for fast QR-based UPI handoff with low UI overhead:
- CameraX preview + ML Kit barcode scan
- Minimal pay entry flow
- Hold-to-pay confirmation gate
- Local receiver verification
- 24-hour local history
- `ACTION_DIAL` handoff for IVR/USSD-style completion

## Architecture

- `MainActivity`: launcher redirect
- `ScannerActivity`: camera + QR parse
- `PaymentActivity`: amount entry
- `ConfirmActivity`: verification + hold gate
- `HistoryActivity`: recent transactions
- `LocalStore`: SharedPreferences JSON persistence
- `UpiParser`: `upi://pay` parser
- `ProviderHeuristics`: UPI handle provider guess

## Security/Scope

- No in-app payment processing
- No UPI PIN collection/storage
- Final user action happens in dialer

## Termux + Android Notes

- This repo is native Android (Gradle/Kotlin), not a Node/Vite project.
- Target platform is Android ARM64; minSdk 24, targetSdk 34.
- Keep dependencies lightweight and avoid native-heavy toolchains.
- If you build in Termux, install compatible JDK + Gradle first.

## Build

### Android Studio
1. Open this folder.
2. Sync Gradle.
3. Build `debug` APK.

### CLI (with Gradle installed)
```bash
gradle :app:assembleDebug
```

## CI Workflows

- `.github/workflows/build.yml`: debug APK build + artifact upload
- `.github/workflows/lint.yml`: Android lint for PR validation

## Config

Update `AppConfig.IVR_NUMBER` in `app/src/main/java/com/example/minimalupi/AppConfig.kt` to prefill a dialer number.
