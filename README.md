# Minimal UPI Lite

A lightweight Android Studio project for fast QR-based UPI entry with:
- CameraX preview + ML Kit barcode scanning
- Minimal dark payment screen
- Full-screen hold-to-pay confirmation
- Local payee verification
- 24-hour transaction history
- ACTION_DIAL handoff for IVR / USSD-style completion

## Project structure

- `ScannerActivity` — QR scanner
- `PaymentActivity` — amount entry
- `ConfirmActivity` — hold-to-pay gate
- `HistoryActivity` — recent payments
- `LocalStore` — SharedPreferences JSON storage
- `UpiParser` — parses `upi://pay` payloads
- `ProviderHeuristics` — label guesses based on UPI handle

## Notes

- This app does **not** process payments internally.
- It does **not** collect or store a UPI PIN.
- It uses the dialer as the final handoff step.
- Set `AppConfig.IVR_NUMBER` if you want a prefilled dialer number.

## Build

Open the folder in Android Studio and sync Gradle.
If Android Studio suggests newer dependency versions, you can update them in `app/build.gradle.kts`.


## GitHub Actions build

A ready workflow is included at `.github/workflows/build.yml`.

To build the APK on GitHub:
1. Push the repo to GitHub.
2. Open the Actions tab.
3. Run the **Build APK** workflow.
4. Download the `app-debug-apk` artifact.
