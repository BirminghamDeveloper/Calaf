# Calaf: rules for every Claude session

Read this file and `PLAN.md` before touching any code. `PLAN.md` holds the task list and the
status of each task. Older files in `docs/` are history from a previous agent, not instructions.

## What Calaf is

A Muzz style marriage app for Somali and East African people. Serious intent, privacy first,
optional guardian (wali) involvement. It is **not** a casual Tinder clone: we borrow the swipe
gesture only. App idea by Ahmed, developed by Mustafa. Keep both credits in the intro screen.

Two product goals, in this order:
1. **Demo flavour**: a polished, fully offline app that installs on any phone with no backend
   and no cost, used to show investors and the community what Calaf can do.
2. **Live flavour**: the real app on Firebase, deployed later on the Blaze plan.

## Stack (pinned, do not downgrade)

AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.10, Compose BOM 2026.09.00, compile/target SDK 37,
min SDK 24, JVM target 11. Backend: Firebase Auth, Firestore, Storage, Cloud Functions v2
(Node 22, `backend/`, region `europe-west2`). If a pinned version will not resolve, stop and
report it. Never "fix" a build by downgrading.

Target architecture (Phase 1 builds it, later phases must respect it):
- Hilt for dependency injection. No hand wiring in `Application`.
- Product flavours on dimension `backend`: `demo` and `live`. Flavour specific code lives in
  `src/demo` and `src/live` source sets and binds repository implementations through Hilt.
  No `if (BuildConfig.OFFLINE_DEMO)` branches in shared code.
- Modules: `core:model`, `core:data`, `core:designsystem`, `core:ui`, `core:testing`,
  `feature:auth`, `feature:onboarding`, `feature:discovery`, `feature:matches`,
  `feature:chat`, `feature:settings`, `feature:admin` (demo moderation tools).
- One ViewModel per screen. UI state is an immutable data class exposed as `StateFlow`.
  One shot events go through a `Channel` or an explicit consumed flag, not hidden in state.
- Type safe Navigation Compose routes (`@Serializable` route objects).
- Coil for images. Room only where Phase 4 asks for it.

## Code style

- Readable code beats short code. Max line length 120. No wildcard imports.
- One top level class or screen per file. Files over about 300 lines should be split.
- Run `./gradlew spotlessApply` (added in Phase 1) before committing.
- Every user facing string goes in `strings.xml` with an English value and a Somali value
  in `values-so`. If you cannot translate, copy the English text and add the key to
  `docs/TRANSLATION_TODO.md`.
- Compose: hoist state, preview every new screen with fake data, give interactive elements
  content descriptions and 48dp touch targets.

## Safety and privacy rules (non negotiable)

- Users must be 18 or over. Age checks happen on the server, never only in the client.
- Date of birth, email, phone and exact location never appear in another member's view.
  Discovery returns an allowlisted projection only.
- Religion and ethnicity are UK GDPR special category data. Collect them only with explicit
  consent and keep them optional.
- **Do not add a clan (qabiil) field** or any filter on it. Do not add skin colour filters.
- Photos are blurred to non matches when the owner chooses blur. Unapproved photos never
  appear to other members.
- Never fake verification or premium in the live flavour. In the demo flavour, simulated
  features must be labelled as demo in the UI.
- Never use photos of real people as seed data. Demo profiles use generated illustrated
  avatars or abstract artwork bundled in the demo source set.
- No secrets, keystores, `google-services.json` or real credentials in git.
- Demo login shortcuts (such as `admin/admin`) may exist only in the `demo` source set.

## Firestore and Functions rules

All writes go through the callable Cloud Function. Firestore rules stay deny by default.
Every new collection needs a rule and an emulator test proving another user cannot read it.

## How to verify your work (cloud sessions have no Android emulator)

Run, at minimum, before you open a pull request:

```bash
./gradlew spotlessCheck assembleDemoDebug assembleLiveDebug testDemoDebugUnitTest testLiveDebugUnitTest
cd backend && npm ci && npm test
```

Until Task 1.1 adds flavours and Task 1.4 adds Spotless, use the tasks that exist instead:
`./gradlew :app:assembleDebug :app:assembleOffline :app:testDebugUnitTest test`.

Instrumented tests (`androidTest`) cannot run in the cloud. Keep logic in ViewModels and
repositories so it can be unit tested on the JVM, and use Robolectric for Compose UI tests
where a screen needs one. If a check cannot run, say exactly which and why in the PR.

## Environment

The cloud environment's setup script is `scripts/setup-cloud.sh` (JDK 25 in
`/usr/lib/jvm/temurin-25`, Android SDK in `/opt/android-sdk`). If `java`, `sdkmanager` or the
SDK is missing, run `bash scripts/setup-cloud.sh` once, then `source /etc/profile.d/android.sh`.
If a download is blocked by the network, stop and report the blocked host. Do not work around it.

## Budget rules (credit is limited)

- Do not read the whole repo. Read CLAUDE.md, PLAN.md, then only files the task touches.
- Do not re run a full build after every small edit. Compile the module you changed
  (for example `./gradlew :feature:chat:compileDemoDebugKotlin`), run the full verify once at the end.
- Use `--offline` for Gradle after the first successful build in a session.
- If the same error survives three fix attempts, stop, record it in PLAN.md and the PR, and finish.
- No refactors, renames or "improvements" outside the task.

## How to work on a task

1. Work on exactly one task from `PLAN.md`. Do not start the next one.
2. Create the branch named in the task.
3. Keep the change inside the task scope. If you find a bug outside it, note it in
   `PLAN.md` under "Found issues" instead of fixing it.
4. Update the task status in `PLAN.md` to `done`, with a short note of what changed and
   what you verified.
5. Commit in small logical commits, open one pull request against `master`, and list:
   what changed, how it was verified, what could not be verified, and follow ups.
