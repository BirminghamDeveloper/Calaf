# Work checkpoint — 29 October 2026



User preference: before reaching a context or execution limit, report completed work,

verification results, and remaining tasks. Keep this checkpoint current for continuation.



## Completed



- Local debug-only admin/admin login, seeded regular demo member and mutual match with Amina.

- Fixed synthetic Firebase option formats that prevented Android callable requests.

- Added backend/security, account, community ViewModel and Android device test coverage.

- Fixed ambiguous selectors and verified server-confirmed message delivery, not input text.

- Added 124 missing draft Somali translations; fixed debug network configuration lint errors.

- Added scripts/Test-Local.ps1 and full feature/verification report docs/VERIFICATION.md.



## Final verification



scripts/Test-Local.ps1 completed successfully: 16 backend + 17 Android JVM + 6 Android

14 device tests = 39 passing tests, zero failures/skips. Debug build passed. App lint:

0 errors/11 warnings. Community lint: 0 errors/0 warnings. Native-speaker Somali review

and multi-device/release validation remain necessary.



## Demo use



Keep scripts/Start-LocalBackend.ps1 running. Run scripts/Seed-LocalBackend.ps1 in a

second terminal if fixtures are missing, then run app in an Android Studio emulator.

Enter admin in the email field and admin as password. This is a local regular demo

member, not production credentials or a privileged administrator.



## Remaining product work



Photos/moderation, Google/phone sign-in, external identity verification, billing,

push delivery, reliable offline sends, automatic erasure, expanded guardian flows,

live deployment and release hardening. Somali copy needs native-speaker review.

Do not touch GitHub identity configuration; user already fixed it.





## 30 October 2026 â€” private photos in progress



Added a profile photo section with system image picker, owner-only preview/reload,

confirmed removal and six fixed upload slots. Images are bounded to 15 MB input,

resized/re-encoded as JPEG with corrected orientation and removed EXIF/GPS metadata.

Authenticated Storage reads are used without publishing download URLs. Rules reject

seventh slots and overwrites. Public approval, ordering and discovery rendering are

not implemented; UI explicitly says photos stay private.



Backend/domain/security rerun: 16 passed, including the new slot restriction.

Android debug build, app unit tests and lint passed. Four new photo device tests and
the full device regression run are pending at this checkpoint.

Run the full scripts/Test-Local.ps1 after the current build finishes. Update the

verification report with actual results; do not carry forward 39 as the new test count.



## 31 October 2026 - login access and APK handoff

The local Firebase backend was not running. Restored the services and seeded demo accounts. Replaced the emulator-only host assumption: Android emulator builds use 10.0.2.2, while physical phones use 127.0.0.1 over adb reverse. Auth connection checks, verification helpers, Firestore, Functions and Storage share the selected host. Added Connect-TestDevice.ps1 for port forwarding, seeding and optional APK installation, with actionable English/Somali connection errors and docs/PHONE_TESTING.md.

Built dist/Calaf-local-debug.apk (20,349,576 bytes), SHA-256 AA392A32E7A69C135972AF38ADC0BEF6EDC872E3AB8888D5FE7B134E5C350C81. Installed and launched this saved APK on Android 14. Eight account JVM tests passed. Two real-backend device flows passed (0 failed, 0 ignored): demo login/matches/message/discovery and registration/local verification/logout/login again. The initial Gradle device runner could not install while the emulator package service was unavailable; direct instrumentation after freeing build memory passed. Logs: .firebase/login-apk-build.log and .firebase/login-device-results.txt. Physical USB hardware was not available to verify; the connection/install script was exercised against the emulator.

This APK needs the computer backend; physical phones also need the USB connection and forwarding script. It is not a standalone live release. No Firebase cloud deployment or GitHub author changes were made. Scope limited to access and APK delivery.

Remaining: live backend deployment for standalone use and previously recorded product work. The earlier full photo run had 1 PhotoRepositoryTest failure out of 10 device tests; do not report all photo tests as passing. That issue and broader feature development were left outside this login/APK task.


## 31 October 2026 - standalone offline demo in progress

User clarified APK was installed directly on a phone, then explicitly chose an offline demo instead of cloud deployment. Added a separate offline build/package with no internet permission or Firebase initialization. Added persistent local accounts, community repositories and local photos, plus administrator-only member suspension/restoration and report resolution with a local action log. Added tests for persistence, member denial, suspension, conversations and UI login/registration. Build and device verification pending at this checkpoint. See docs/OFFLINE_DEMO.md for the intended usage. No production administrator privileges were granted.
