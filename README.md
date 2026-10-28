# Calaf

Native Android application for culturally aware, marriage-focused introductions.

## Current implementation

This is an account and private-profile development build, not the completed MVP.
It includes a short animated introduction crediting Ahmed (idea) and Mustafa
(development), a welcome screen with visible account actions, Firebase email/password
registration and login, password reset, email verification, session restoration,
logout, and a private profile editor with automatic local draft saving and review.
English and draft Somali resources share a Material 3 light/dark theme. Somali copy
needs native-speaker review before release.

Modules: `app` (entry point), `core:designsystem` (shared visual tokens and theme),
`core:model` (account/profile contracts and reusable validation with tests).
ViewModels expose StateFlow and use constructor-injected repository interfaces.
Firebase SDK types remain in the data layer; application wiring supplies dependencies.

## Build

Open this directory in Android Studio. Configure its Gradle JDK to the bundled JBR
or a compatible JDK 17+. This machine uses Android Studio's bundled JBR 25.

Required versions are pinned: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.10,
compile/target SDK 37, minimum SDK 24, JVM bytecode target 11.
Android modules use AGP's built-in Kotlin with Kotlin 2.4.10 on the plugin classpath.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :core:model:test :app:testDebugUnitTest :app:lintDebug
# With a connected device or emulator:
.\gradlew.bat :app:connectedDebugAndroidTest
```

`local.properties` supplies the local Android SDK path and is excluded from Git.
Firebase configuration and signing keys are also excluded.

## Firebase development configuration

The local `app/google-services.json` targets `calaf-development` and Android package
`com.calaf.app`. Google Services processes it into Android resources; Firebase's
initialization provider initializes the SDK at startup. Firebase Authentication is
included using the Firebase BoM. No Firebase SDK types are used in Composables.

Email/Password and Google providers are enabled in the console. The configuration
contains the debug Android OAuth client and a web OAuth client for the future
Credential Manager integration. Email/password flows use the real Auth SDK. Google
sign-in UI is not implemented yet. Automated tests use fake repositories and do not
create live accounts or send real email. Live authentication still needs an end-to-end
check with an account controlled by the tester.

Register, then tap Send verification email. Open the email link and return to tap
I have verified my email. The profile editor accepts a display name, date of birth
(YYYY-MM-DD), city, country, languages, culture and biography. Edits autosave after
350 ms; Save and review profile validates required fields and age before review.
Drafts are stored in app-private DataStore, keyed by Firebase UID, and excluded from
cloud backup/device transfer. No passwords are persisted by the app's form state.
Drafts do not sync across devices and are removed if app data is cleared.

Registration includes an adult confirmation and profile validation checks age, but
these client checks are NOT a backend age gate. Discovery/publication is unavailable
until backend adult eligibility and privacy rules are deployed. No public profile
collection is written by this build.

On a fresh checkout, obtain the development app's configuration from Firebase
Project settings and save it as `app/google-services.json` before building. Do not
use this development project for production releases. Production configuration and
release/Play signing fingerprints must be provisioned separately before release.

Setup reference: https://firebase.google.com/docs/android/setup

## Remaining phases

1. Expand domain models and add the compatibility engine and its unit tests.
2. Firebase data design, deny-by-default rules, emulator tests, authentication,
   adult eligibility enforced by the backend, Google sign-in and full onboarding.
3. Cloud profile sync and moderated photo uploads; preferences and discovery.
4. Transactional mutual matching, matched-only messaging, reporting and blocking.
5. Consent-based guardian invitations and verification provider integration.
6. Play Billing with backend verification, notifications and privacy controls.
7. Accessibility, localisation review, integration/UI testing and release hardening.

Each phase must compile and pass its relevant checks before advancing.
No subscription products, identity provider, backend deployment, release signing,
or production moderation service is configured yet.

The supplied brief ends at the heading "49. COMPOSE". Obtain any remaining
requirements before final release acceptance.
