# Calaf implementation status and continuation plan

Updated: 3 November 2026. Workspace: `C:\Users\Hassan\Codex\Calaf`.

This is the current handoff report. Older dated entries in `PROGRESS.md`, `VERIFICATION.md` and `FEATURE_AUDIT.md` are historical evidence, not statements that every feature or test is currently complete. The original supplied brief contains sections 1â€“48 and ends at the heading for section 49 (COMPOSE); no missing section text has been invented.

## 1. Current objective and decisions

The user installed the previous APK directly on a phone and could not log in. That APK depended on Firebase emulators running on a computer. Granting administrator privileges would not solve the missing backend connection. The user explicitly chose **an offline demonstration app**, rather than finishing billing/account setup and deploying the live backend.

The immediate deliverable is therefore **Calaf Offline Demo**, with working device-local registration/login and privileged demo administration. It is not the production online application. Continue the existing product work in phases after this handoff, preserving working functionality and the distinction between demo and live services.

Standing requirements:

- Purpose: a social marriage platform for Somali and East African people.
- Credits: app idea by Ahmed; developed by Mustafa. Preserve these in the animated introduction and relevant app copy.
- Do not change the already-fixed Git commit author configuration.
- Do not silently deploy services, enable paid services, or represent local fixtures as real users.
- Preserve the pinned Android toolchain; do not downgrade to bypass build problems.
- Before reaching a work limit, save completed work, actual verification results, remaining work and the next executable steps in Markdown.
- Commit descriptions should use the requested `+` / `%` style and need not disclose demo credentials.

## 2. Which app to install

| Build | Package | Connection | Use |
| --- | --- | --- | --- |
| Calaf Offline Demo | `com.calaf.app.offline` | No internet permission; no Firebase initialization | Direct installation on a phone for fictional, local testing |
| Local Firebase debug build | `com.calaf.app` | Computer-hosted Firebase emulators; USB forwarding for a physical phone | Multi-account backend development and security testing |
| Future production release | `com.calaf.app` | Deployed and secured Firebase services | Real users; not ready or deployed |

Offline artifact: `C:\Users\Hassan\Codex\Calaf\dist\Calaf-offline-demo.apk`.

Old computer-connected artifact: `dist/Calaf-local-debug.apk`. Do not send that file to someone expecting a standalone demo. Both apps can coexist because they have different package IDs. Open **Calaf Offline Demo** after installing the offline file. See `docs/OFFLINE_DEMO.md` and `dist/INSTALL-OFFLINE.txt`.

Offline demo accounts:

| Role | Login | Password |
| --- | --- | --- |
| Offline administrator | `admin` | `admin` |
| Member | `hassan@calaf.test` | `CalafDemo!2026` |
| Member | `amina@calaf.test` | `CalafDemo!2026` |
| Member | `hodan@calaf.test` | `CalafDemo!2026` |
| Member | `yusuf@calaf.test` | `CalafDemo!2026` |

Registration creates additional local members. No inbox verification is necessary or claimed in this mode, and password-reset email is unavailable. New-member passwords are stored as salted PBKDF2 hashes. Seed credentials are public demonstration credentials, not production secrets. One account is signed in at a time. Accounts/messages do not synchronize between phones. Clearing app storage or uninstalling removes local data.

## 3. Work completed so far

### Foundation and interface

- Native Kotlin/Compose app with Material styling, app icon and an animated introduction.
- Somali/East African marriage-platform purpose and Ahmed/Mustafa credits.
- Welcome, registration, login, profile, discovery, matches/messages, activity and settings destinations.
- English and draft Somali resource sets; offline administration/help still contains English-only copy.
- Modules currently present: `app`, `core:model`, `core:designsystem`, `core:data`, `feature:community`.
- Pinned AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.10, compile/target SDK 37, min SDK 24 and Java 11 application target. Local builds use Android Studio's bundled JDK.

### Connected development implementation

These are implemented against local Firebase services. They have not been released against the live Firebase project.

- Email/password registration, confirmation, login/logout, verification, password reset and account-error feedback.
- Local-only verification helper because the Auth emulator sends no email.
- Authentication token refresh after verification and corrected synthetic Firebase option formats.
- Local backend startup/seed scripts and account fixture setup.
- Emulator host selection and USB forwarding/install script for physical phones.
- Local profile drafts, cloud publication/restoration, adult validation and immutable birth-date checks on the server.
- Profile fields for name, DOB, city/country, language/culture, biography, gender, religion, education/occupation, interests/values, children preferences, smoking, relocation and marriage timeline.
- Discovery projections that exclude private DOB/email, basic partner filters and compatibility explanations.
- Transactional like/pass and mutual matching, duplicate handling and free daily-interest quota.
- Matched-only text conversations, older-message loading, send retry ID reuse, opt-in read receipts and chat safety warnings.
- Activity inbox, privacy settings, reporting, blocking and unmatching.
- Guardian invitation, acceptance and revocation without automatically granting access to private chat.
- Session revocation, partial export and deletion-request recording. Recording a request is not completed data erasure.
- Firestore/Storage rules, backend domain logic and emulator integration tests.

### Private photographs

- System image picker, six private slots, thumbnail preview, reload, upload and confirmed removal.
- Image input bounds, orientation correction, JPEG resize/re-encoding and EXIF/GPS stripping.
- Firebase variant uses authenticated bounded Storage reads; offline variant uses private app files.
- Storage rules restrict owner access and permitted slot names, content type and size.
- These are **private uploads**, not approved public profile photos. Public galleries, moderation, ordering and primary-photo selection are not complete.

### Standalone offline mode

- Separate `offline` build type and package, visibly labelled **Calaf Offline Demo**.
- Internet permission and Firebase initialization provider removed in the offline manifest.
- `OfflineDemo` provides device-local authentication, profile drafts, community state and photo storage through the existing repository interfaces.
- Five seeded fictional accounts, pre-established sample matches and a sample moderation report.
- Local registration, login/logout and persistent records; no dependency on a computer, USB forwarding, Firebase configuration or an email inbox.
- Profile editing/publication, basic filters, discovery, likes/passes, matches and text conversations.
- Seed profiles simulate reciprocal interest. Newly created local members require reciprocal likes.
- Messages can be inspected by switching local accounts; no real person receives them and no automatic remote reply is implied.
- Local activity, blocking/reporting/unmatching, privacy and guardian invitation state.
- Partial export and a recorded deletion-request flag; these are explicitly limited demo behaviours.

### Privileged offline administrator

- `admin / admin` opens the offline administrator account.
- In Profile, scroll to **Administrator controls**, then **Open administrator panel**.
- View demo members, suspend/restore members, view reports and resolve/dismiss reports.
- Destructive/status-changing choices require confirmation and record a local administrative action.
- Suspended members cannot log in or appear in discovery; conversation operations also check account suspension.
- Normal members cannot call the administrative repository operations. The administrator cannot suspend itself.
- No Firebase production administrator role, cloud custom claims, or live administrative portal has been created. The older connected debug account remains a regular demo member.

## 4. Latest continuation work and verification

The interrupted offline run exposed a test-definition problem: two expression-bodied `runBlocking` tests inferred a Boolean return from SharedPreferences cleanup. JUnit requires void test methods, so the repository test class did not execute. The previous log also showed admin-panel UI success, but did not contain a completed registration UI result. That was not an all-green run.

On 3 November, the coroutine test bodies were explicitly typed `runBlocking<Unit>` so JUnit can execute them. The offline app and instrumentation package rebuilt successfully, and the account/unit and Android device checks completed. The results below are from the 3 November run.

**3 November verified result: 13 targeted checks passed (8 account JVM tests + 5 Android device tests), zero failures/errors/skips. This is not a whole-project regression total.**

- `assembleOffline`, `assembleOfflineAndroidTest` and `testOfflineUnitTest`: BUILD SUCCESSFUL (58 seconds).
- `AccountFlowTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- Android 14 / Small_Phone: `OK (5 tests)` in 87.672 seconds; runner reported 0 failed and 0 ignored.
- Device checks: no network permission/no Firebase initialization; registration persistence and administrator-only moderation; message persistence and blocked-chat rejection; admin login opening the panel; registration/logout/returning login with no email or backend.
- The JUnit initialization error is fixed; the repository tests now execute rather than being skipped or rejected.
- Installed the saved offline artifact directly; no local backend or USB forwarding script was required.
- Saved APK size: 20,471,456 bytes. Package: `com.calaf.app.offline`, version `0.1.0-offline-demo`, minimum SDK 24.
- SHA-256: `93FDA10486DE8F5B4B980B58FE7D9ABE24247D78BA34DF41DBBD9D7EB5065E13`.
- The copied APK matches the built APK. This turn changed test signatures and documentation, not the app binary; the previous build fingerprint is retained.
- Scope limits: local-store reopening was tested; process-death/migration and real-handset testing remain follow-up work. Backend tests, photo tests and release/lint certification were not rerun in this scoped offline verification.

Historical evidence (do not add these together as a current whole-project test total):

| Date/scope | Recorded result | Limit |
| --- | --- | --- |
| 29 October complete local baseline | 16 backend + 17 JVM + 6 device = 39 passing | Before later photo/offline changes |
| Later photo run | 10 device tests, 1 failure in `PhotoRepositoryTest` | Exact assertion and Storage behaviour need reproduction |
| 31 October login/APK task | 8 account JVM tests and 2 connected login/registration device flows passed | Local Firebase build, not offline mode or live deployment |
| Offline first attempt | Build passed; repository test class rejected by JUnit; UI run incomplete | Superseded only when the new run completes |

No physical handset verification, production deployment, Play release certification or full current regression pass should be inferred from these results.

## 5. Gaps against the original requirements

| Area / brief sections | Remaining work |
| --- | --- |
| Accessibility/design, 4â€“7 | TalkBack traversal, contrast/font scaling, touch targets, RTL, full small-screen review, additional screen polish |
| Architecture/models, 8â€“10 | Split the broad community feature and offline repository into focused components; dependency injection and stronger domain boundaries; keep demo/live implementations isolated |
| Authentication, 11 | Google and phone sign-in, production email configuration, recent-auth and provider-linking UX, real multi-device session validation |
| Firebase/data, 12â€“13 | Deploy functions/rules/indexes, App Check setup and enforcement, production configuration, operational monitoring and environment separation |
| Onboarding/profile, 14â€“15 | Structured onboarding/completion percentage and fields such as country of birth, height, marital status, distinct ethnicity/practice and alcohol preference where appropriate |
| Photos, 16 | Resolve outstanding Storage device test, moderation state, progress/retry, reorder/primary selection, approved public thumbnails/gallery and caching |
| Preferences, 17 | Extend beyond age/gender/country/language/smoking/children to culture/religion/practice, height, education, marital status, timeline, relocation, verification, values/deal-breakers and privacy-safe distance |
| Discovery/engine, 18â€“19 | Detailed public profile/gallery, broader compatibility explanations, scalable pagination/ranking, location buckets, smooth accessible interactions |
| Likes/matches, 20 | Likes-received surface, entitlement-checked undo and advanced interest actions; retain transactional race handling |
| Chat/safety, 21â€“22 | Durable send queue and delivery state, reconnect behaviour, stronger safety UX, broader multi-device coverage; offline demo has no safety-warning/receipt parity yet |
| Guardian, 23 | Expiry, permission UX, invitation delivery, consent history and expanded privacy-safe family workflows |
| Verification, 24 | Phone/selfie/manual review and provider-backed identity verification, expiry/rejection/appeal paths; never promote a local mock to a genuine verification badge |
| Safety/location/privacy, 25â€“27 | Moderator notes/evidence, account warnings/bans/appeals, approximate distance controls, retention policies, complete export and actual erasure processing |
| Plans/billing, 28â€“30 | Billing client abstraction, Play products/prices, purchase/restore/pending/error states and server-verified entitlements, renewal/revocation handling |
| Notifications, 31 | FCM token lifecycle, permission UX, per-category preferences, server dispatch and secure deep links; inbox is not push delivery |
| Localisation/navigation/settings, 32â€“35 | Native-speaker Somali review, missing translations, RTL/back-stack/deep-link checks and completion of settings that currently explain unavailable services |
| Security/offline/errors, 36â€“40 | Release security review, App Check/device integration, durable Room/WorkManager outbox/cache for the connected build, richer retry/recovery UI |
| Validation/age/moderation, 41â€“43 | Extend validation for new fields, moderation workflows, separately secured production administration and immutable audit records |
| Analytics/crashes, 44â€“45 | Consent-aware diagnostics and Crashlytics with sensitive-data redaction; current preferences do not establish those integrations |
| Tests/quality/performance, 46â€“48 | Fix outstanding photo test; current full-suite run; CI, release build/signing, migration tests, baseline/startup benchmarks and device/accessibility matrix |

Offline-specific limits: the local demo does not enforce the backend's full quota/eligibility/compatibility system, does not send emails/push notifications, does not implement real remote receipts or payment ownership, and does not provide full deletion/export. SharedPreferences JSON is a small-demo store, not the planned production synchronization layer. Blocking a previously unmatched demo member currently creates a pair through the sample-match helper and can also create a misleading MATCH activity item; correct that demo-only behaviour in the next focused polish pass.

## 6. Implementation order and acceptance criteria

### Phase 1 â€” close the offline handoff

**Completed on the emulator on 3 November:** the five-device-test run and eight account JVM checks, copied-artifact installation and fingerprint verification. The offline APK path is recorded above. Keep the failed/interrupted earlier logs as history. Verify on the user's physical handset when feedback is available; do not equate an emulator pass with all-device support.

Targets: `app/src/androidTest/.../OfflineDemoTest.kt`, `OfflineDemoScreenTest.kt`, `app/src/main/.../offline`, `docs/OFFLINE_DEMO.md`, this report and the progress/verification records.

Acceptance: direct install; admin login exposes the panel; ordinary login does not; registration/login work without email/network; suspension and restoration enforce access; messages survive reopening; app has no internet permission and initializes no Firebase app.

### Phase 2 â€” offline polish and storage-test repair

Separate pair creation from match-notification creation so blocking does not emit a match. Add a regression test for unmatched blocking. Add process-restart coverage and clearer guidance for switching accounts. Factor local auth/community/moderation persistence into smaller classes before adding substantial demo state. Do not add fake premium or verified-ID flags.

For the outstanding Firebase photo failure, inspect the exact failed assertion, compare Android SDK upload behaviour with Storage rule semantics, and reproduce with a unique object and a changed payload. Preserve owner-only access; fix the actual rule/repository expectation rather than weakening the test. Then rerun the affected test and related access-denial cases.

Acceptance: no spurious MATCH notification after block; local account/photo isolation remains intact; the Storage test passes for the intended behaviour and unauthorized reads/writes remain denied.

### Phase 3 â€” complete profile and public-photo foundation

Extend `core:model` drafts/public projections/preferences with explicit types and migration defaults. Add onboarding sections and a derived completion percentage. Update validation and server allowlists before exposing new fields. Keep private DOB/contact information out of public responses.

Add photo records with owner, ordering, primary selection, processing state, moderation state and timestamps. Upload originals privately; process thumbnails server-side; publish only approved derivatives. Add retry/progress UI and controlled authenticated/private access. Cache approved thumbnails without leaking private uploads.

Targets: `core:model`, `core:data`, app profile/photo UI, `backend/src/service.js`, Storage/Firestore rules and integration tests.

Acceptance: old drafts still load, invalid/underage inputs cannot publish, photo ordering persists, unapproved photos never appear in discovery, deletion revokes access, image limits/metadata removal remain tested.

### Phase 4 â€” discovery, preferences and matching depth

Add the missing structured filters, profile-detail/gallery screen and transparent compatibility reasons. Apply eligibility, blocks, visibility and entitlements on the backend. Implement approximate location buckets rather than exposing coordinates. Add cursor-based paging/indexes and bounded ranking work. Add likes-received/undo only with explicit entitlement rules.

Acceptance: server and UI filters agree; no blocked/paused/ineligible profile appears; paging is stable; compatibility can be explained; location privacy and entitlement bypass attempts are tested.

### Phase 5 â€” connected chat resilience, guardians and notifications

Introduce a Room-backed outbox with stable message IDs, account ownership, bounded retry/backoff and WorkManager dispatch. Model pending/sent/failed states; keep server idempotency. Cancel or quarantine pending sends after logout/block/suspension. Preserve opt-in receipt behaviour.

Add FCM token registration/revocation and authenticated server events, respecting category settings. Route deep links through authentication and membership checks. Extend guardian invitations with expiry and explicit consent/permissions, without exposing private conversations automatically.

Acceptance: reconnect sends each message once; logout cannot send as another account; blocked chats reject queued sends; push deep links cannot open another member's content; expired/revoked guardian invitations fail.

### Phase 6 â€” real administration, verification and data lifecycle

Build trusted server-side administrator/moderator roles with least-privilege permissions and current account-status checks. Never authorize from the `admin` text alias or a client-local flag. Provide a separately secured admin surface with strong authentication for production. Add report queues, evidence access checks, moderator notes, reasoned warnings/suspensions/bans, appeals and audit records.

Add an idempotent deletion worker covering Auth, Firestore records, private/public Storage and notification tokens, with retry/status tracking and a documented retention policy. Expand exports to the intended data scope. Integrate a chosen identity provider only after its configuration/contract exists; verify callbacks and keep sensitive documents out of ordinary profile data.

Acceptance: ordinary clients cannot grant themselves roles; role revocation takes effect; all administrative mutations are attributable; deletion completes across services rather than setting a flag; forged verification callbacks are rejected.

### Phase 7 â€” billing and premium

Add a billing abstraction and retrieve prices/products from Play configuration. Implement purchase/restore/pending/error UI. Verify purchase tokens on the server, handle renewal/cancellation/refund events, and gate premium actions there. Keep safety features free. Use Play test products/accounts; no local Boolean should grant real premium ownership.

Acceptance: valid purchase restores across devices; pending/expired/revoked purchases do not grant access; forged/reused tokens fail; prices are Play-sourced.

### Phase 8 â€” production activation and release hardening

When the user chooses online activation, confirm project/account/billing readiness and prepare reviewable deployment configuration. Validate project IDs and signing fingerprints; deploy rules/indexes/functions to the intended environment; configure App Check, authentication providers and monitoring. Keep demo fixtures and public credentials out of production provisioning.

Complete native-speaker translation review, accessibility/RTL checks, redacted consent-aware analytics/crash reporting, startup/list performance checks and CI. Build and inspect the signed release; test on representative supported devices; prepare policy/support content and a controlled Play rollout.

Acceptance: a fresh install on a phone works without a development computer; real multi-user flows pass against the intended backend; no demo bypass, private-data leakage or unverified premium/ID status remains.

## 7. External inputs needed later

These are not required to use the offline APK:

- User decision to resume online deployment, Firebase billing readiness and deploy authentication.
- Google/phone provider configuration and signing fingerprints for the distribution build.
- Play Console products, API access, test users and release signing ownership.
- Identity-verification provider selection/configuration if government-ID verification is required.
- Actual moderation operators, support contacts, policy/terms content and retention decisions.
- Native-speaker Somali review and physical-device feedback.

No current offline action authorizes spending money or deploying to production. Prepare changes locally before requesting any missing activation input.

## 8. Reproduction and evidence locations

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleOffline :app:assembleOfflineAndroidTest :app:testOfflineUnitTest `
  '-Pcalaf.testBuildType=offline' --max-workers=1 `
  '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```

Install app and test APKs using Android SDK `adb`, then run:

```powershell
adb shell am instrument -w -r `
  -e class com.calaf.app.OfflineDemoTest,com.calaf.app.OfflineDemoScreenTest `
  com.calaf.app.offline.test/androidx.test.runner.AndroidJUnitRunner
```

Use the SDK's full adb path if it is not on PATH. The emulator must finish booting and expose the package service before installation. On this low-memory computer, build first, stop the idle Gradle daemon, then start the emulator. Do not terminate the user's Android Studio session to free memory.

Current logs: `.firebase/offline-verification-nov03-build.log` and `.firebase/offline-verification-nov03-device.txt`. Unit XML reports: `app/build/test-results/testOfflineUnitTest`. Logs/artifacts are local and ignored by Git. Source tests and this report are reviewable project files.

For connected development use `scripts/Start-LocalBackend.ps1`, `Seed-LocalBackend.ps1`, `Connect-TestDevice.ps1` and `Test-Local.ps1`. Do not use those as prerequisites for the offline demo.

## 9. Next session checklist

1. Read this report and the latest dated progress entry; inspect current working changes without overwriting user work.
2. The targeted offline checks passed. Preserve that baseline; reproduce the separate outstanding Firebase photo failure when beginning Phase 2.
3. Use the offline APK path for the user's standalone phone testing; request the exact visible error and app name if it fails.
4. Continue Phase 2, then the profile/photo foundation, preserving working flows.
5. Keep demo behaviour and production capability clearly separated in code, UI, tests and reports.
6. Update this document before stopping or reaching a limit, with commands/results and the next concrete task.
