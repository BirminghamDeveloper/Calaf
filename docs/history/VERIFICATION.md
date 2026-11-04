> Current handoff (3 November 2026): [Implementation status and continuation plan](IMPLEMENTATION_STATUS_AND_PLAN.md). Entries below retain their original dates and scopes; offline administrator privileges differ from the older Firebase demo member.

# Local development verification — 29 October 2026

## Demo access

In the local Android debug build, enter `admin` in the email field and `admin`
as the password on the Log in screen. This aliases a seeded Firebase emulator
member (`admin@calaf.test`) with a completed adult profile and a mutual match with
Amina. It is not an administrator role and grants no elevated permissions.
The shortcut is gated by both DEBUG and USE_FIREBASE_EMULATORS. Unit tests verify
that the default/live authentication flow rejects it and that it cannot register.

Run `scripts/Start-LocalBackend.ps1`, then `scripts/Seed-LocalBackend.ps1`, and
launch the app on an Android Studio emulator. The local backend must stay running.

## Implemented features

- Animated startup with Ahmed/Mustafa credits and Somali/East African platform purpose.
- Email/password accounts, confirmation, verification, reset, login and logout.
- Adult validation, autosaved private drafts, review, publication and restoration.
- Discovery filters, explained compatibility, like/pass and mutual matches.
- Matched messaging, older-history loading, opt-in read receipts and safety warnings.
- Activity inbox, blocking, reporting, unmatching and privacy controls.
- Guardian invitation acceptance/revocation; session revocation; partial data export;
  deletion-request recording with immediate profile suspension.
- English and draft Somali resources, including the previously missing community strings.

## Automated checks

Backend: 16 passing tests covering domain validation, matching/message idempotency,
stranger access, blocking, immutable birth dates, adult gating, discovery privacy,
read-receipt consent, notification races, reports/unmatching, guardians, deletion
authorization, scoped exports, Firestore writes/reads and Storage access restrictions.
Integration tests use `demo-calaf-tests` rather than clearing the demo account data.

Android JVM: 17 passing tests covering account/profile validation and state, local
admin gating, verification refresh, persistence isolation, failures/retries,
message-id reuse, history merging, read receipts, pagination and blocked chats.

Android device: **6 passing tests, zero failures and zero skipped**, on Small_Phone
(Android 14). These cover credits/account actions, password mismatch rejection,
registration through profile review/editing, local admin login with server-confirmed
message delivery and discovery, real emulator registration/email verification, and
account-scoped draft persistence.

Final combined run: **39 tests passed**. `scripts/Test-Local.ps1` exited successfully;
the debug APK built successfully. App lint has **0 errors and 11 warnings**; community
lint has **0 errors and 0 warnings**. Remaining warnings include dependency-version
notices and manifest guidance; this is not a release certification.

The device checks uncovered and led to fixes for invalid local Firebase SDK option
formats, ambiguous Refresh selectors, and message/list accessibility selectors.
The local configuration now passes SDK checks before callable requests. Network
security configuration explicitly disables subdomain matching on emulator hosts.
No source checks were suppressed and no failing tests were skipped.

Repeat all checks with `scripts/Test-Local.ps1`; `-SkipDevice` intentionally omits
device checks. Android reports live in each module's `build/reports` directory.

## Limits

This verifies implemented local flows, not the entire original production brief.
Photos/moderation, Google/phone sign-in, external identity verification, billing,
push notifications, durable offline sending, full account erasure and live deployment
remain unfinished. A deletion request is not proof of erasure. Storage-rule tests do
not imply that a photo-upload UI exists. Somali translations need native-speaker review.
Device checks currently target Android 14; other Android versions and release signing
are not certified by this run. Guardian acceptance currently checks the invitation and
authenticated verified email; broader guardian identity checks remain to be implemented.


## 31 October 2026 - login access and APK handoff

The local Firebase backend was not running. Restored the services and seeded demo accounts. Replaced the emulator-only host assumption: Android emulator builds use 10.0.2.2, while physical phones use 127.0.0.1 over adb reverse. Auth connection checks, verification helpers, Firestore, Functions and Storage share the selected host. Added Connect-TestDevice.ps1 for port forwarding, seeding and optional APK installation, with actionable English/Somali connection errors and docs/PHONE_TESTING.md.

Built dist/Calaf-local-debug.apk (20,349,576 bytes), SHA-256 AA392A32E7A69C135972AF38ADC0BEF6EDC872E3AB8888D5FE7B134E5C350C81. Installed and launched this saved APK on Android 14. Eight account JVM tests passed. Two real-backend device flows passed (0 failed, 0 ignored): demo login/matches/message/discovery and registration/local verification/logout/login again. The initial Gradle device runner could not install while the emulator package service was unavailable; direct instrumentation after freeing build memory passed. Logs: .firebase/login-apk-build.log and .firebase/login-device-results.txt. Physical USB hardware was not available to verify; the connection/install script was exercised against the emulator.

This APK needs the computer backend; physical phones also need the USB connection and forwarding script. It is not a standalone live release. No Firebase cloud deployment or GitHub author changes were made. Scope limited to access and APK delivery.

Remaining: live backend deployment for standalone use and previously recorded product work. The earlier full photo run had 1 PhotoRepositoryTest failure out of 10 device tests; do not report all photo tests as passing. That issue and broader feature development were left outside this login/APK task.


## 3 November 2026 - offline APK verified and detailed handoff saved

Fixed the two offline coroutine test signatures to return Unit, allowing JUnit to run them. Rebuilt the offline APK/test package and ran the account unit suite: 8 passed, no failures/errors/skips. Direct Android 14 instrumentation completed 5 offline tests in 87.672 seconds, with 0 failed and 0 ignored. Covers no internet permission/no Firebase initialization; registration persistence and restricted administrator operations; suspension/restoration; persisted messages and blocked sends; admin-panel login and registration/logout/login UI.

Standalone artifact: `dist/Calaf-offline-demo.apk` (20,471,456 bytes), package `com.calaf.app.offline`. SHA-256: `93FDA10486DE8F5B4B980B58FE7D9ABE24247D78BA34DF41DBBD9D7EB5065E13`. Open **Calaf Offline Demo** and use admin/admin for offline administrator controls. No computer backend or USB setup is needed. No real cloud administrator was provisioned.

Detailed completed-feature inventory, original-brief gap matrix, verification history, eight-phase implementation plan, dependencies, commands and next-session checklist: [IMPLEMENTATION_STATUS_AND_PLAN.md](IMPLEMENTATION_STATUS_AND_PLAN.md).

Remaining immediate work: physical-phone feedback; offline unmatched-block notification polish and process-restart coverage; reproduce/fix the separate Firebase photo test failure; then complete profile/photo moderation foundation. Live authentication/deployment, public photos, production moderation, push, billing, identity-provider integration, full erasure/export and release hardening remain incomplete. This run is 13 targeted passing checks, not a new full-suite certification. No Git author changes or production deployment were performed.
