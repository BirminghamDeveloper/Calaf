# Run the local Calaf APK on a phone

This is a debug APK connected to your computer's local Firebase backend. It does not connect to the live Calaf Development project and will not work as a standalone online app. Local accounts and live Firebase accounts are separate.

1. Open PowerShell in `C:\Users\Hassan\Codex\Calaf`.
2. Run `.\scripts\Start-LocalBackend.ps1` and leave that terminal running.
3. Enable Developer options and USB debugging on your Android phone. Connect it by USB and accept the debugging prompt.
4. Open a second PowerShell terminal in the same project folder and run `.\scripts\Connect-TestDevice.ps1 -Install`.
5. Open Calaf, choose Log in, and use `admin` / `admin` to explore the fictional profiles. This is a demo member, not an elevated administrator.
6. To register instead, enter a new email address, a password meeting the displayed requirements, confirm it, and confirm you are an adult. Select **Verify local demo email** to finish local verification. You can then log out and sign in with that same email and password.

Keep the computer, backend and USB connection running. Repeat step 4 after reconnecting or rebooting the phone. If more than one device is connected, supply `-Serial DEVICE_ID` (listed by Android SDK `adb devices`). Do not uninstall the existing app to resolve an installation error without first preserving any data you need.

APK: `C:\Users\Hassan\Codex\Calaf\dist\Calaf-local-debug.apk`

An Android Studio emulator connects directly through 10.0.2.2 and still needs the local backend running and fixtures seeded. The phone uses 127.0.0.1 with USB port forwarding for Auth, Firestore, Functions and Storage.

Live standalone use remains dependent on the separate Firebase backend deployment, which has not been completed.
