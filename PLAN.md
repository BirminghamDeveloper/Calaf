# Calaf build plan

Each task is one cloud session and one pull request. Do them in order unless a task says it can
run in parallel. A task may start only when every task in "Depends on" has been merged to `master`.

Status values: `todo`, `in progress`, `done`, `blocked`, `deferred`.

**Current goal: the demo flavour.** Tasks marked `deferred` are live only or cosmetic work, parked until
the owner decides to go live. Never start a `deferred` task. When a task asks you to change both flavours,
the live flavour must still compile and pass its tests, but put the effort into the demo experience.
Demo track order: 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5.

**AUTOPILOT is on.** The owner asked for the whole demo track in one run. Read `docs/AUTOPILOT.md` and `docs/PROGRESS.md`
and carry on from the first demo task that is not done. Its rules override "one task per session".

---

## Phase 0: make the project buildable anywhere

### Task 0.1 Cloud build bootstrap
Status: done | Branch: `task/0.1-cloud-bootstrap` | Depends on: none

- `scripts/setup-cloud.sh` already exists and is the environment setup script. Confirm JDK 25 and
  the SDK are present (run the script if not), write `local.properties` with
  `sdk.dir=/opt/android-sdk` (it is git ignored), and fix the script if anything in it is wrong.
- Get `./gradlew :app:assembleDebug :app:assembleOffline :app:testDebugUnitTest test` green on
  Linux without changing pinned versions. Note: `gradle.properties` sets `calaf.emulators=true`,
  and `app/google-services.json` is ignored, so the debug build may need a placeholder
  `google-services.json` for CI. Generate one in CI only, never commit a real one.
- Run `cd backend && npm ci && npm test` (domain tests). Emulator integration tests are optional
  here and need `firebase-tools`.
- Add `.github/workflows/android.yml` running build and unit tests, and backend `npm test`.
- Fix the broken character encoding in `docs/*.md` (for example `â€“` should be an en dash).
- Move `docs/IMPLEMENTATION_STATUS_AND_PLAN.md`, `PROGRESS.md`, `VERIFICATION.md`,
  `FEATURE_AUDIT.md` into `docs/history/`.

Done when: the script works on a fresh session, CI is green, nothing else changed.

Done notes (Task 0.1):
- `scripts/setup-cloud.sh`: fixed SDK package id (`platforms;android-37.0`, the old `android-37` id does
  not exist, so the platform was never installed) and the noisy JDK banner in the final message.
- Added `scripts/placeholder-google-services.sh` (fake, git ignored `app/google-services.json` for CI and
  cloud sessions). `local.properties` written with `sdk.dir=/opt/android-sdk` (git ignored).
- `gradlew` was committed non executable; mode is now 755.
- Added `.github/workflows/android.yml` (Android build and unit tests, backend `npm test`).
- Fixed mojibake in docs (two files affected), moved the four history files to `docs/history/`.
- Verified in a cloud session: `./gradlew :app:assembleDebug :app:assembleOffline :app:testDebugUnitTest test`
  green; `cd backend && npm ci && npm test` 5/5 pass.
- Not verified: the GitHub Actions workflow itself (it only runs once the PR is open); emulator
  integration tests (optional, need `firebase-tools`).

---

## Phase 1: clean architecture

### Task 1.1 Flavours and Hilt
Status: done | Branch: `task/1.1-flavours-hilt` | Depends on: 0.1

- Replace the `offline` build type with product flavours `demo` (applicationIdSuffix `.demo`,
  app name "Calaf Demo", no INTERNET permission, no Firebase init) and `live`.
- Add Hilt. Bind `AuthRepository`, `ProfileRepository`, `CommunityRepository`,
  `PhotoRepository` in `src/demo` and `src/live` Hilt modules.
- Delete the lazy wiring in `CalafApplication` and every `BuildConfig.OFFLINE_DEMO` branch in
  shared code. The demo banner comes from a flavour resource or a bound `AppMode`.
- Move the `admin/admin` shortcut into the demo source set only.
- Update the CLAUDE.md verify commands if task names change.
- Update `.github/workflows/android.yml` to the new task names (`assembleDemoDebug`,
  `assembleLiveDebug`, `testDemoDebugUnitTest`, `testLiveDebugUnitTest`).
- The google-services plugin must not run for `demo` (its `.demo` package has no Firebase client
  and the demo never uses Firebase). Disable `process*Demo*GoogleServices` the same way the
  current build disables `processOfflineGoogleServices`.

Done when: both flavours build, the demo APK has no INTERNET permission (check the merged
manifest in a unit test or script), existing unit tests pass.

Done notes (Task 1.1):
- `app` has product flavours on dimension `backend`: `demo` (`.demo` suffix, "Calaf Demo", INTERNET removed in
  its manifest, Firebase and `core:data` are `liveImplementation` only, so the demo APK has no Firebase classes)
  and `live`. The `offline` build type and `BuildConfig.OFFLINE_DEMO` are gone. `USE_FIREBASE_EMULATORS` exists
  in `live` only. `processDemo*GoogleServices` is disabled via `androidComponents.onVariants`.
- Hilt 2.60.1 and KSP 2.3.13 added (new pins, Kotlin and AGP untouched). `CalafApplication` is an empty
  `@HiltAndroidApp`, `MainActivity` is `@AndroidEntryPoint`. `DemoModule` (`src/demo`) and `LiveModule`
  (`src/live`) bind `AuthRepository`, `ProfileRepository`, `CommunityRepository`, `PhotoRepositoryFactory`
  and `AppMode`. `OfflineDemo`, `OfflineAdminSection` moved to `src/demo`. Firebase and DataStore classes
  moved to `src/live`.
- `PhotoRepository` is per member, so Hilt binds a new `PhotoRepositoryFactory` (in `core:model`) instead.
- `AppMode` (shared interface) carries the flavour differences: banner, login notice, password reset,
  emulator flag, admin section, and the optional `AdminShortcut`. The `admin/admin` shortcut and its
  credentials now exist only in `src/demo` (`DemoAppMode`). The live flavour no longer has any admin shortcut,
  including in emulator builds.
- Demo banner and login notice moved from hard coded strings to `demo_strings.xml` (Somali values are
  English copies, listed in the new `docs/TRANSLATION_TODO.md`).
- `scripts/check-demo-manifest.sh` fails if the demo merged manifest has INTERNET or Firebase. CI runs it.
- Instrumented tests split into `androidTestDemo` and `androidTestLive`, and use a new `AppEntryPoint`.
- CI workflow, CLAUDE.md verify commands and `docs/OFFLINE_DEMO.md` / README names updated.
- Verified: `assembleDemoDebug assembleLiveDebug testDemoDebugUnitTest testLiveDebugUnitTest
  :core:model:test :feature:community:testDebugUnitTest` green, manifest script OK, `aapt2` shows
  `com.calaf.app.demo` labelled "Calaf Demo" with no INTERNET, live APK has INTERNET, androidTest sources
  compile for both flavours.
- Not verified: instrumented tests (no emulator), release builds with R8 for the new flavours, the GitHub
  Actions run, and the live flavour against a real Firebase project.

### Task 1.2 Split the demo backend
Status: done | Branch: `task/1.2-demo-backend` | Depends on: 1.1

- Break `OfflineDemo` into `DemoStore` (one JSON file store with a schema version and
  migration hook), `DemoAuthRepository`, `DemoCommunityRepository`, `DemoPhotoRepository`,
  `DemoAdminRepository`, `DemoSeed`.
- Fix: blocking an unmatched member must not create a pair or a MATCH notification. Add a
  regression test.
- Demo admin becomes a role flag on the stored user, not `id == "demo-admin"`.
- Seed passwords move to a single `DemoSeed` constant documented as public demo credentials.
- JVM unit tests for each repository using a temp directory store.

Done notes (Task 1.2):
- `OfflineDemo` is gone. New package `com.calaf.app.demo` in `src/demo`: `DemoStore` (one `demo.json` file plus a
  `photos` folder, schema version with a `migrate` hook, atomic write, no Android dependency), `DemoSession`
  (who is signed in), `DemoSeed`, `DemoAuthRepository`, `DemoProfileRepository` (the old `drafts`),
  `DemoCommunityRepository` (reads) with `DemoCommandHandler` (writes), `DemoPhotoRepository`,
  `DemoAdminRepository`, `DemoProfileCodec`. `DemoModule` wires them. Code reformatted to 120 columns.
- Bug fixed: blocking an unmatched member no longer creates a pair or a MATCH notification. Blocks are recorded in
  a separate `blocks` table, hide both members from each other in discovery and refuse likes. An existing pair is
  still closed as BLOCKED. Regression test `blocking an unmatched member creates no pair and no MATCH notification`
  fails if the old behaviour is put back (checked by mutation).
- Demo admin is now a `admin` flag on the stored user. No code compares against `"demo-admin"`. Administrators
  cannot be suspended.
- Public demo credentials live only in `DemoSeed` (`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `MEMBER_PASSWORD`), also used
  by `DemoAppMode`.
- 27 new JVM tests in `app/src/testDemo` using a temp directory store (store 6, auth 5, profile 2, photo 2,
  admin 4, community 8). New test only dependency `org.json:json:20260814` (`testDemoImplementation`) because
  android.jar only has stubs for `org.json`.
- `OfflineDemoTest` (instrumented) was replaced by those JVM tests. Its network permission check lives on in
  `DemoManifestTest`.
- Behaviour change to know about: the demo now stores data in `files/calaf_demo/demo.json` instead of
  SharedPreferences `offline_demo_v1`, so data from a pre 1.2 demo install is not carried over. The `.demo`
  package only exists since Task 1.1, so this affects no released install.
- Verified: `assembleDemoDebug assembleLiveDebug testDemoDebugUnitTest testLiveDebugUnitTest :core:model:test
  :feature:community:testDebugUnitTest`, both `compile*DebugAndroidTestKotlin`, `scripts/check-demo-manifest.sh`
  and `cd backend && npm ci && npm test` (5 of 5) all pass.
- Not verified: instrumented tests (no emulator), the demo on a device, the GitHub Actions run.

### Task 1.3 Feature modules and ViewModels
Status: done | Branch: `task/1.3-feature-modules` | Depends on: 1.2

- Create `core:ui`, `core:testing` (fake repositories, test dispatcher rule) and the feature
  modules listed in CLAUDE.md.
- Split `CommunityViewModel` into `DiscoveryViewModel`, `MatchesViewModel`, `ChatViewModel`,
  `SettingsViewModel`, `GuardianViewModel`, `ActivityViewModel`. Hilt injected.
- Type safe navigation graph with a bottom bar: Discover, Matches, Activity, Profile.
- Real bottom bar icons. Today the bar shows the first letter of each label instead of an icon.
  Add `CalafIcons` in `core:designsystem` as vector drawables (Material Symbols Rounded, Apache 2.0,
  copied as XML, not the deprecated `material-icons-extended` library). Outlined icon when not
  selected, filled when selected: Discover (explore/compass), Matches (favorite/heart),
  Messages (chat bubble), Activity (notifications/bell), Profile (person). Every icon gets a
  content description. Use the same `CalafIcons` set for back, more, report, block, send and
  settings buttons that currently use text only.
- Move existing unit tests to the right modules and add tests for each new ViewModel.
- Behaviour must stay the same. This task is a refactor, not a redesign.

Done notes (Task 1.3):
- New modules: `core:ui` (shared strings, `ActionRunner`/`ActionState`/`Notice`, dialogs, `CalafListScreen`,
  form components), `core:testing` (`FakeCommunityRepository`, `FakeAuthRepository`, `FakeProfileRepository`,
  `MainDispatcherRule`), `feature:auth`, `feature:onboarding`, `feature:discovery`, `feature:matches`,
  `feature:chat`, `feature:settings`, `feature:admin` (demo only dependency of `app`). `feature:community` is gone.
- `CommunityViewModel` is split into `DiscoveryViewModel`, `MatchesViewModel`, `ChatViewModel`, `SettingsViewModel`,
  `GuardianViewModel`, `ActivityViewModel`, plus `PublishViewModel` (publishing the profile). All are
  `@HiltViewModel` with assisted factories taking the member id. State is an immutable data class with a nested
  `ActionState` (busy, error, notice). The chat "left the conversation" signal is a one shot `Channel` event.
- Type safe navigation (`@Serializable` destinations in `app/navigation`) with a bottom bar Discover, Matches,
  Activity, Profile. Activity shows an unread badge. A conversation opens from Matches (the old Messages tab is
  merged into Matches). Settings is a header icon and its pages are destinations.
- `CalafIcons` in `core:designsystem`: Material Symbols Rounded vector drawables copied from
  `google/material-design-icons` (`symbols/android`), outlined and filled for the five tab icons, plus back,
  more, report, block, send and settings. Used for the bottom bar, header settings and back buttons, chat header,
  send button and a "more" menu on match cards (Unmatch only; Report and Block are icon buttons on the card).
  Every icon has a content description.
- Strings were split by use into each module with English and Somali values (shared ones in `core:ui`). A few
  keys exist twice on purpose because the old app and community wording differed (`country`, `safety_body`).
- Tests: 39 JVM tests for the new ViewModels, the shared `ActionRunner` and the moved auth and profile tests, plus
  3 destination serialization tests. Old `AccountFlowTest` and `CommunityViewModelTest` were moved and extended.
- CI and the CLAUDE.md verify commands now run `testDebugUnitTest` (every library module) and `:core:model:test`.
- Verified: `assembleDemoDebug assembleLiveDebug testDemoDebugUnitTest testLiveDebugUnitTest testDebugUnitTest
  :core:model:test`, both `compile*DebugAndroidTestKotlin`, `scripts/check-demo-manifest.sh`, backend `npm test` 5 of 5.
- Not verified: the app running (no emulator), so Hilt and navigation wiring, icon rendering and the new layout are
  only compile and unit tested. Instrumented tests were adjusted but not run. The GitHub Actions run.
- Follow up commits on this branch (owner review of PR 5): Report and Block restored as visible icon buttons on the
  match card; `LocalBackendFlowTest` no longer uses `admin/admin` (it takes a seeded account from instrumentation
  arguments, see `backend/scripts/seed.mjs`); `HiltSmokeTest` (demo) launches `MainActivity` through
  `HiltAndroidRule` and opens every tab, run by the new `hilt-smoke` job in `.github/workflows/android.yml`.
  The instrumented tests that launch `MainActivity` are now `@HiltAndroidTest`, and the runner is `HiltTestRunner`.

### Task 1.4 Code style and backend bug fixes
Status: deferred | Branch: `task/1.4-style-backend-fixes` | Depends on: 1.3
(The backend half can run in parallel with 1.3 if done as a separate PR touching only `backend/`.)

- Add Spotless with ktlint, line length 120, no wildcard imports. Reformat the codebase in one
  commit containing nothing else.
- Backend: split premium from verification. `subscriptions/{uid}` gets `premiumUntil`.
  Verification lives on the profile. Fix `interest()` which currently treats `verifiedUntil`
  as premium. Add domain and integration tests.
- Backend: make discovery return a non empty page or a null cursor (keep reading pages up to a
  bounded limit), and batch the per candidate reads. Full redesign is Task 4.1.

---

## Phase 2: Muzz style profiles and photos

### Task 2.1 Richer profile and onboarding
Status: done | Branch: `task/2.1-onboarding` | Depends on: 1.3

- Add optional fields: marital status (never married, divorced, widowed), has children,
  height, religiosity (very practising, practising, moderately, not practising), prays,
  ethnicity/heritage (free choice from Somali, Somaliland, Ethiopian, Eritrean, Djiboutian,
  Kenyan, Sudanese, other), country of birth, languages, profession, education, marriage
  timeline, willing to relocate. No clan field.
- Explicit consent screen before religion or heritage is saved (special category data).
- Multi step onboarding with progress and a profile completion percentage.
- Update `core:model`, backend `validateProfile`, allowlisted discovery projection, demo store
  migration, and both flavours. Old drafts must still load.

Done notes (Task 2.1):
- `core:model`: new optional fields on `ProfileDraft` (marital status, height in cm, country of birth, religiosity, prayer
  habit, heritage) plus `sensitiveConsent`, appended so old drafts and positional constructors keep working. Existing
  fields already covered has children, languages, profession (`occupation`), education, timeline and relocation. No clan
  field. `ProfileCompletion.percent` (optional faith fields only count after consent), `withConsent`,
  `withoutUnconsentedData`. Validation adds `Height` and `ConsentRequired`.
- Onboarding: five steps (basics, in your words, your life, faith and heritage, review) with a progress bar, "Step n of 5"
  and a live completion percentage. An invalid save jumps to the step that needs fixing. The faith step starts with an
  explicit consent card (off by default). Turning consent off deletes religion, religiosity, prayer and heritage.
- Backend: `validateProfile` accepts the new fields and rejects special category data sent without consent
  (`failed-precondition`). Discovery blanks faith and heritage unless the owner consented, returns the new fields in
  the allowlisted projection (never the consent flag, DOB or email) and compatibility ignores unconsented religion.
  Seed script updated. 5 new domain tests (10 pass).
- Demo store schema version 2: migration gives seeded members consent and clears the religion text of everyone else
  (no consent record exists for it). Drafts never keep faith data without consent. Seed profiles are richer.
  Live local drafts and Firebase restore/payload carry the new fields and drop unconsented data on load.
- Tests: model (3), onboarding ViewModel (4), demo store migration, profile repository and discovery projection (5).
- Not verified: the onboarding screens on a device. Somali for the new `onb_*` strings is English (TRANSLATION_TODO).

### Task 2.2 Photo model and demo photos
Status: done | Branch: `task/2.2-photos-model` | Depends on: 2.1

- `Photo(id, order, isPrimary, state: PENDING|APPROVED|REJECTED, blurForNonMatches)`.
- Profile photo manager UI: add up to 6, reorder by drag, choose primary, delete, see moderation
  state. Coil for loading. Keep EXIF stripping and resizing from `PhotoProcessor`.
- Demo flavour: bundle 8 to 12 generated illustrated avatars (not real people), auto approve
  uploads, and let the demo admin approve or reject photos in `feature:admin`.

Done notes (Task 2.2):
- `core:model`: `Photo(id, order, isPrimary, state PENDING|APPROVED|REJECTED, blurForNonMatches)`, `PublicPhoto`, the new
  `PhotoRepository` (photos, read, add, delete, setPrimary, reorder, setBlur, readMemberPhoto, plus `supportsManagement`
  and `simulatedModeration`) and `PhotoRules` (shared ordering rules, 5 tests). `CommunityProfile.photos` lists approved
  photos only, main first, with the blur choice.
- Demo: ten generated, faceless illustrated avatars in `app/src/demo/res/drawable-nodpi` (made by
  `scripts/generate-demo-avatars.py`, abstract art, no real people). Seeded members get 2 to 3 of them. Uploads are
  approved at once and labelled as demo on the screen. `DemoPhotoRepository` enforces six photos, a single main photo
  and that other members only read approved photos of members they are not blocked with. Store schema version 3
  migrates the old `photo-N.jpg` files into approved photo rows and adds avatars for seeded members.
- Photo manager (`feature:onboarding`): list with Coil thumbnails, main photo badge, moderation state, make main, move
  up and down buttons, long press drag to reorder, blur switch, remove. Keeps `PhotoProcessor` (EXIF stripping and
  resizing). Controls that the backend cannot do are hidden (live).
- Demo admin (`feature:admin`): photo queue with thumbnail, owner, Approve and Reject, recorded in the audit log.
  Rejecting hides the photo from every other member immediately.
- Live: `FirebasePhotoRepository` is an adapter over the existing private slot storage. Photos show as pending and
  management, public reads and blur throw `UnsupportedOperationException` until the deferred live photo pipeline.
- Tests: 5 `PhotoRules`, 5 `PhotoViewModel`, demo photo repository (7), admin queue, store migration and discovery
  projection. `FakePhotoRepository` added to `core:testing`.
- Not verified: the manager screen, dragging and Coil rendering on a device.

### Task 2.3 Live photo pipeline
Status: deferred | Branch: `task/2.3-photos-live` | Depends on: 2.2

- Storage layout: `private/{uid}/{photoId}` (owner only), `public/{uid}/{photoId}_{size}.jpg`
  (readable by signed in active members, written only by Functions).
- Function on upload: validate, create thumbnails, set state PENDING. Callable for moderators
  to approve or reject. Only approved derivatives are copied to `public/`.
- Blur: when `blurForNonMatches` is on, discovery gets a server generated blurred derivative,
  matches get the clear one.
- Storage and Firestore rules plus emulator tests proving private photos cannot be read by
  others and unapproved photos never appear in discovery.
- Resolve the old failing `PhotoRepositoryTest` or replace it with an equivalent JVM test.

---

## Phase 3: discovery experience

### Task 3.1 Swipe card stack
Status: todo | Branch: `task/3.1-swipe-stack` | Depends on: 2.2

- Reusable `SwipeCardStack` composable in `core:ui`: drag with rotation, fling thresholds,
  undo animation hook, 3 cards rendered at most, buttons for pass and like that do the same
  thing (accessibility), TalkBack actions.
- Card shows a photo pager with tap zones, name, age, city, heritage, profession, marriage
  timeline and top compatibility reasons.
- Prefetch the next page when 3 cards remain. Empty and error states.
- Unit test the swipe decision logic separately from the composable.

### Task 3.2 Profile detail and filters
Status: todo | Branch: `task/3.2-profile-detail` | Depends on: 3.1
(Can run in parallel with 3.3.)

- Full profile screen opened from the card: all photos, about, values, deen section,
  compatibility breakdown, report and block.
- Filter sheet covering the Phase 2 fields, saved to preferences.

### Task 3.3 Match moment, likes and compliments
Status: todo | Branch: `task/3.3-match-likes` | Depends on: 3.1
(Can run in parallel with 3.2.)

- Full screen "It's a match" with both photos, send a message or keep browsing.
- "Liked you" list. In live, gate it behind premium (Phase 5). In demo, show it with a demo label.
- Compliment: a like with a short note (max 140 characters) shown on the receiver's card.
  Backend validation and safety warnings apply.

### Task 3.4 Chat polish and guardian view
Status: todo | Branch: `task/3.4-chat-guardian` | Depends on: 3.3

- Chat bubbles, day separators, sent and read state, safety warning banner on flagged messages,
  unmatch and report from the chat menu.
- Guardian (wali): the member can invite a guardian to a specific match. Guardian sees that
  match's conversation read only, only after the member opts in for that match.
- Demo: seeded matches send a scripted, clearly labelled demo reply after a short delay.

### Task 3.5 Demo showcase polish
Status: todo | Branch: `task/3.5-demo-showcase` | Depends on: 3.4

The demo is shown to investors and the community. Make the first five minutes impressive.
- Seed 20 fictional demo profiles (balanced women and men, ages 21 to 45, UK, Somaliland, Somalia,
  Djibouti, Ethiopia, Kenya, US, Canada, Sweden cities), each with complete fields, 2 to 4 bundled
  illustrated avatars (never real people), believable bios. Mark every one as a demo profile.
- First launch: after the intro, a short 3 screen "what Calaf is" walkthrough with a Skip button.
- A "Try the demo" button on the welcome screen that signs into a ready made member who already has
  2 matches, 1 compliment received and a started conversation, so a viewer sees everything in 30 seconds.
- A "Reset demo data" action in demo settings.
- App icon and splash screen with the Calaf name, consistent colours and typography across all screens,
  empty states with friendly illustrations instead of plain text.
- Fill in all Somali strings you can. List the rest in `docs/TRANSLATION_TODO.md`.
- `docs/DEMO_SCRIPT.md`: a 5 minute walkthrough the owner can follow when presenting.

Done when: both flavours build, unit tests pass, demo manifest check passes.

---

## Phase 4: backend that scales

### Task 4.1 Discovery redesign
Status: deferred | Branch: `task/4.1-discovery-backend` | Depends on: 3.2

- Denormalised `discoverable/{uid}` document maintained by Functions with only public fields,
  status, gender, age bucket, country, city bucket, heritage, approved primary photo.
- Firestore composite indexes for the main filters, cursor paging, exclusion of seen and
  blocked pairs, simple ranking (completion, recent activity, compatibility).
- Approximate location by city or geohash prefix. Never exact coordinates.
- Emulator tests: blocked, paused, suspended, under 18 and already seen profiles never return.

### Task 4.2 Push notifications
Status: deferred | Branch: `task/4.2-push` | Depends on: 4.1
(Can run in parallel with 4.3.)

- FCM token registration and removal on logout, Android 13 permission prompt with explainer.
- Functions send pushes for new match, new message, compliment and guardian invite, respecting
  notification settings. Payloads carry ids only, no message text.
- Deep links go through auth and membership checks.

### Task 4.3 Reliable chat sending
Status: deferred | Branch: `task/4.3-chat-outbox` | Depends on: 3.4
(Can run in parallel with 4.2.)

- Room outbox with stable message ids, WorkManager retry with backoff, pending/sent/failed UI.
- Pending sends are cancelled on logout, block or suspension. Server stays idempotent.

---

## Phase 5: going live

### Task 5.1 Deployment readiness
Status: deferred | Branch: `task/5.1-deploy-ready` | Depends on: 4.2, 4.3

- `docs/DEPLOY.md`: Blaze plan steps, budget alerts (50, 80, 100 percent of a GBP budget),
  App Check with Play Integrity, environments (`calaf-development`, later `calaf-production`).
- `scripts/deploy.sh` that the owner runs locally. Cloud sessions never deploy.
- Remove remaining emulator only defaults from the live release path.

### Task 5.2 Moderation and admin roles
Status: deferred | Branch: `task/5.2-moderation` | Depends on: 5.1

- Firebase custom claims for `moderator` and `admin`, set only by a script run by the owner.
- Moderation callables: photo queue, report queue, warn, suspend, ban, restore, with reasons
  and an append only audit log. In app moderator screen in `feature:admin` for the live flavour,
  hidden unless the claim exists.

### Task 5.3 Selfie verification
Status: deferred | Branch: `task/5.3-verification` | Depends on: 5.2

- Member records a selfie with a requested pose. A moderator compares it with profile photos
  and approves or rejects. Badge only after approval. Selfies stored privately and deleted
  after review. Interface ready for a provider later.

### Task 5.4 Premium with Play Billing
Status: deferred | Branch: `task/5.4-billing` | Depends on: 5.1
(Can run in parallel with 5.2 and 5.3.)

- `BillingRepository` abstraction. Live: Play Billing, server side purchase token verification,
  `premiumUntil` set only by the Function. Demo: fake purchase screen labelled demo.
- Premium unlocks: liked you list, unlimited likes, rewind, advanced filters. Safety features stay free.
- Prices are read from Play, never hard coded.

### Task 5.5 Privacy and account lifecycle
Status: deferred | Branch: `task/5.5-privacy` | Depends on: 5.2

- Full data export, real deletion worker covering Auth, Firestore, Storage and FCM tokens,
  consent records for special category data, privacy notice and terms screens with placeholders
  for legal text.

### Task 5.6 Release hardening
Status: deferred | Branch: `task/5.6-release` | Depends on: all above

- R8 rules, signing config read from environment variables, baseline profile, accessibility
  pass (contrast, font scale, TalkBack labels), Somali translation gaps listed, Play listing
  checklist in `docs/RELEASE.md`.

---

## Decisions

Owner decisions. Items below were listed as pending in Task 1.3 and are **approved** (AUTOPILOT, see `docs/AUTOPILOT.md`).

| # | Decision | Where | Status |
| --- | --- | --- | --- |
| 1 | The Messages tab is merged into Matches. A conversation opens from a match card or from Activity. The tab bar is Discover, Matches, Activity, Profile. | `app/.../SignedInApp.kt`, `feature:matches` | approved |
| 2 | Activity is a tab with an unread count badge. The old header "Activity (n)" button is gone. | `app/.../SignedInApp.kt` | approved |
| 3 | Settings is an icon button in the header. Back (on settings pages) pops one screen instead of always returning to Profile. | `app/.../SignedInApp.kt` | approved |
| 4 | On a match card Report and Block are visible icon buttons. Unmatch is in a "more" menu. | `feature:matches` | approved |

## Decisions made on autopilot

Choices made without the owner while following `docs/AUTOPILOT.md`. One line each, newest last.

- The `hilt-smoke` emulator job is `continue-on-error: true`. Only the `android` and `backend` jobs gate merges.
- 2.1: "Prays" is a four option prayer habit (always, usually, sometimes, never). Religion text, religiosity, prayer and
  heritage are all special category data behind one consent switch that is off by default.
- 2.1: Old stored religion text with no consent record is cleared by the demo store migration (seeded fictional members
  keep theirs). The server rejects, rather than silently strips, special category data sent without consent.
- 2.1: New Somali strings are English copies listed in `docs/TRANSLATION_TODO.md`, to be filled in Task 3.5.
- 2.2: The main photo is always the first photo. Choosing a main photo or dragging one to the front are the same thing.
- 2.2: Blur is a per photo owner choice carried in the public projection. The blur itself is drawn by the card in Task 3.1.
- 2.2: Reordering has long press drag plus Move up and Move down buttons so it works with TalkBack and without gestures.
- 2.2: In the live flavour photos all show as pending and the manager hides ordering and blur until Task 2.3 (deferred).

## Found issues

(Sessions add problems they notice outside their task scope here.)

- Maven Central (repo.maven.apache.org) intermittently answers HTTP 429 through the cloud proxy. Gradle then
  disables the repository for the rest of that build, so a cold-cache build can fail several times in a row.
  Each attempt caches more; re-running the same command (use `--max-workers=1 -Dorg.gradle.parallel=false`
  to be gentler) converged after about 3 attempts. Warm cache builds are fine. Not a pinned version problem.
- The post Task 1.4 verify command in CLAUDE.md (`testDemoDebugUnitTest testLiveDebugUnitTest`) does not run
  the library module tests (`:core:model:test`, `:feature:community:testDebugUnitTest`). Add them when
  Task 1.3 or 1.4 touches that line.
- `assembleDemoRelease` and `assembleLiveRelease` (R8) were not tried in Task 1.1. Task 5.6 owns release
  hardening.
- Moved legacy files keep their old long lines and wildcard imports (`AuthScreen`, `ProfileScreen`,
  `PhotoSection`, `FormComponents`, `AdminPanel`, `PhotoSectionTest`). Spotless (Task 1.4, deferred) will fix them.
- Unused strings carried over into `core:ui`: `activity_count`, `empty_messages`, `development_notice`,
  `last_messages`. Safe to delete in a later cleanup.
- Somali for `more_options` is an English copy (listed in `docs/TRANSLATION_TODO.md`).
- The GitHub proxy returns 403 for deleting branches (`DELETE git/refs/heads/...`), so merged task branches stay on the remote.
