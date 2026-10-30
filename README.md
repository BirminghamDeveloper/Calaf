# Calaf

A social marriage platform for Somali and East African people.
App idea by Ahmed; developed by Mustafa.

## Run locally in Android Studio

The debug build currently uses **local Firebase emulators**, not live Firebase accounts.
The Android Studio virtual device connects to the computer through `10.0.2.2`.

1. Open a terminal in this project and run `.\scripts\Start-LocalBackend.ps1`.
2. Wait for **All emulators ready**, and keep that terminal open.
3. Optionally run `.\scripts\Seed-LocalBackend.ps1` in a second terminal for fictional profiles.
4. Run the `app` configuration on an Android Studio emulator.
5. Register with an email and matching passwords, and confirm you are 18 or older.
6. Tap **Verify local demo email**. Local Auth does not send email; this debug-only
   action consumes the local verification code. Live accounts still require an inbox link.
7. Complete your profile, save/review it, then publish it before using discovery.

Local shortcut: enter **admin** in the email field and **admin** as the password on Log in.
This works only in the local debug build and signs in as a regular fictional member,
with a completed profile and a match with Amina. It does not grant administrator privileges.
Run `scripts/Seed-LocalBackend.ps1` if the account is missing.

Other seeded accounts: `hassan@calaf.test`, `amina@calaf.test`, `hodan@calaf.test`,
`yusuf@calaf.test`; password `CalafDemo!2026`. These are fictional local fixtures.
Hassan and Amina have a mutual match for trying conversations.

The backend saves local accounts and data under ignored `.firebase/local-data` when
stopped normally with Ctrl+C, and imports that snapshot next time. Abrupt termination
may lose changes since the last export. Accounts created before persistence was added
may need to be registered again. Local and live Firebase accounts are separate.
For local password reset, use the link printed in the backend terminal.

## Implemented development flows

- Animated introduction, platform purpose and creator credits.
- Email/password registration, login, verification, password reset, and sign-out.
- Private autosaved profile drafts, published profile restoration and server adult eligibility.
- Discovery, partner filters, explained compatibility scores, like/pass and mutual matches.
- Matched text conversations, older-message loading, opt-in read receipts and safety warnings.
- Activity inbox with latest match/message alerts and mark-as-read actions (not push notifications).
- Blocking, unmatching, reporting, visibility/privacy settings and guardian invitations.
- Session revocation, partial account export and deletion-request recording.
- Firebase server functions and restricted Firestore/Storage rules, local demo fixtures.

Profile drafts remain separate from publication. The server stores date of birth privately;
discovery returns an allowlisted projection without email or date of birth.

## Build

Open this directory in Android Studio and use its bundled JBR (25 on this machine).
Versions: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.10, compile/target SDK 37, min SDK 24,
JVM bytecode 11. Modules: app, core:model, core:data, core:designsystem, feature:community.

```powershell
.\gradlew.bat :app:assembleDebug
```

`local.properties`, `app/google-services.json`, signing keys and emulator data are ignored.
The existing Firebase configuration targets `calaf-development`, package `com.calaf.app`.
To build against live Firebase explicitly, use `-Pcalaf.emulators=false`; server features
still require deployment. Release builds never use the local verification helper.
Do not distribute the local development APK as a production app.

## Remaining work

Photos/moderation, Google and phone login, external identity verification, Play Billing,
push notifications, reliable offline message queues, automatic deletion, expanded guardian
flows, complete Somali translation, architecture/release hardening and full device verification.
Somali copy needs native-speaker review. Live deployment requires account setup; no backend
has been deployed. Premium and government-ID verification are not represented as functional.

The supplied brief ends at heading "49. COMPOSE". The feature audit is in
`docs/FEATURE_AUDIT.md` and records initial gaps plus implementation updates.

## Verification

With the local backend and Android Studio emulator running:

```powershell
.\scripts\Test-Local.ps1
```

Use `-SkipDevice` only when intentionally omitting device tests. Backend integration
checks use the separate `demo-calaf-tests` project and do not clear demo profiles.
The current verification report is `docs/VERIFICATION.md`. Tests cover implemented
local flows; paid subscriptions, external identity verification, push delivery and
live deployment cannot be verified until implemented/configured.

## Private photos (development)

Open Profile and scroll to Private profile photos. Choose a photo using the system
picker, preview it, reload saved photos, or confirm removal. Up to six fixed slots
are available. Source images are limited to 15 MB and are resized/re-encoded as JPEG
with orientation corrected and location metadata removed before upload.

These are owner-only private uploads. They are not approved/public discovery photos.
Moderation, public rendering, ordering and primary-photo selection remain unfinished.
Uploads/read/delete require the local Storage emulator (or configured live Storage).
The app does not publish a download URL or request broad photo-library permission.
