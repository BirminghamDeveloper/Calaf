# Calaf build plan

Each task is one cloud session and one pull request. Do them in order unless a task says it can
run in parallel. A task may start only when every task in "Depends on" has been merged to `master`.

Status values: `todo`, `in progress`, `done`, `blocked`.

---

## Phase 0: make the project buildable anywhere

### Task 0.1 Cloud build bootstrap
Status: todo | Branch: `task/0.1-cloud-bootstrap` | Depends on: none

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

---

## Phase 1: clean architecture

### Task 1.1 Flavours and Hilt
Status: todo | Branch: `task/1.1-flavours-hilt` | Depends on: 0.1

- Replace the `offline` build type with product flavours `demo` (applicationIdSuffix `.demo`,
  app name "Calaf Demo", no INTERNET permission, no Firebase init) and `live`.
- Add Hilt. Bind `AuthRepository`, `ProfileRepository`, `CommunityRepository`,
  `PhotoRepository` in `src/demo` and `src/live` Hilt modules.
- Delete the lazy wiring in `CalafApplication` and every `BuildConfig.OFFLINE_DEMO` branch in
  shared code. The demo banner comes from a flavour resource or a bound `AppMode`.
- Move the `admin/admin` shortcut into the demo source set only.
- Update the CLAUDE.md verify commands if task names change.

Done when: both flavours build, the demo APK has no INTERNET permission (check the merged
manifest in a unit test or script), existing unit tests pass.

### Task 1.2 Split the demo backend
Status: todo | Branch: `task/1.2-demo-backend` | Depends on: 1.1

- Break `OfflineDemo` into `DemoStore` (one JSON file store with a schema version and
  migration hook), `DemoAuthRepository`, `DemoCommunityRepository`, `DemoPhotoRepository`,
  `DemoAdminRepository`, `DemoSeed`.
- Fix: blocking an unmatched member must not create a pair or a MATCH notification. Add a
  regression test.
- Demo admin becomes a role flag on the stored user, not `id == "demo-admin"`.
- Seed passwords move to a single `DemoSeed` constant documented as public demo credentials.
- JVM unit tests for each repository using a temp directory store.

### Task 1.3 Feature modules and ViewModels
Status: todo | Branch: `task/1.3-feature-modules` | Depends on: 1.2

- Create `core:ui`, `core:testing` (fake repositories, test dispatcher rule) and the feature
  modules listed in CLAUDE.md.
- Split `CommunityViewModel` into `DiscoveryViewModel`, `MatchesViewModel`, `ChatViewModel`,
  `SettingsViewModel`, `GuardianViewModel`, `ActivityViewModel`. Hilt injected.
- Type safe navigation graph with a bottom bar: Discover, Matches, Activity, Profile.
- Move existing unit tests to the right modules and add tests for each new ViewModel.
- Behaviour must stay the same. This task is a refactor, not a redesign.

### Task 1.4 Code style and backend bug fixes
Status: todo | Branch: `task/1.4-style-backend-fixes` | Depends on: 1.3
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
Status: todo | Branch: `task/2.1-onboarding` | Depends on: 1.4

- Add optional fields: marital status (never married, divorced, widowed), has children,
  height, religiosity (very practising, practising, moderately, not practising), prays,
  ethnicity/heritage (free choice from Somali, Somaliland, Ethiopian, Eritrean, Djiboutian,
  Kenyan, Sudanese, other), country of birth, languages, profession, education, marriage
  timeline, willing to relocate. No clan field.
- Explicit consent screen before religion or heritage is saved (special category data).
- Multi step onboarding with progress and a profile completion percentage.
- Update `core:model`, backend `validateProfile`, allowlisted discovery projection, demo store
  migration, and both flavours. Old drafts must still load.

### Task 2.2 Photo model and demo photos
Status: todo | Branch: `task/2.2-photos-model` | Depends on: 2.1

- `Photo(id, order, isPrimary, state: PENDING|APPROVED|REJECTED, blurForNonMatches)`.
- Profile photo manager UI: add up to 6, reorder by drag, choose primary, delete, see moderation
  state. Coil for loading. Keep EXIF stripping and resizing from `PhotoProcessor`.
- Demo flavour: bundle 8 to 12 generated illustrated avatars (not real people), auto approve
  uploads, and let the demo admin approve or reject photos in `feature:admin`.

### Task 2.3 Live photo pipeline
Status: todo | Branch: `task/2.3-photos-live` | Depends on: 2.2

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

---

## Phase 4: backend that scales

### Task 4.1 Discovery redesign
Status: todo | Branch: `task/4.1-discovery-backend` | Depends on: 3.2

- Denormalised `discoverable/{uid}` document maintained by Functions with only public fields,
  status, gender, age bucket, country, city bucket, heritage, approved primary photo.
- Firestore composite indexes for the main filters, cursor paging, exclusion of seen and
  blocked pairs, simple ranking (completion, recent activity, compatibility).
- Approximate location by city or geohash prefix. Never exact coordinates.
- Emulator tests: blocked, paused, suspended, under 18 and already seen profiles never return.

### Task 4.2 Push notifications
Status: todo | Branch: `task/4.2-push` | Depends on: 4.1
(Can run in parallel with 4.3.)

- FCM token registration and removal on logout, Android 13 permission prompt with explainer.
- Functions send pushes for new match, new message, compliment and guardian invite, respecting
  notification settings. Payloads carry ids only, no message text.
- Deep links go through auth and membership checks.

### Task 4.3 Reliable chat sending
Status: todo | Branch: `task/4.3-chat-outbox` | Depends on: 3.4
(Can run in parallel with 4.2.)

- Room outbox with stable message ids, WorkManager retry with backoff, pending/sent/failed UI.
- Pending sends are cancelled on logout, block or suspension. Server stays idempotent.

---

## Phase 5: going live

### Task 5.1 Deployment readiness
Status: todo | Branch: `task/5.1-deploy-ready` | Depends on: 4.2, 4.3

- `docs/DEPLOY.md`: Blaze plan steps, budget alerts (50, 80, 100 percent of a GBP budget),
  App Check with Play Integrity, environments (`calaf-development`, later `calaf-production`).
- `scripts/deploy.sh` that the owner runs locally. Cloud sessions never deploy.
- Remove remaining emulator only defaults from the live release path.

### Task 5.2 Moderation and admin roles
Status: todo | Branch: `task/5.2-moderation` | Depends on: 5.1

- Firebase custom claims for `moderator` and `admin`, set only by a script run by the owner.
- Moderation callables: photo queue, report queue, warn, suspend, ban, restore, with reasons
  and an append only audit log. In app moderator screen in `feature:admin` for the live flavour,
  hidden unless the claim exists.

### Task 5.3 Selfie verification
Status: todo | Branch: `task/5.3-verification` | Depends on: 5.2

- Member records a selfie with a requested pose. A moderator compares it with profile photos
  and approves or rejects. Badge only after approval. Selfies stored privately and deleted
  after review. Interface ready for a provider later.

### Task 5.4 Premium with Play Billing
Status: todo | Branch: `task/5.4-billing` | Depends on: 5.1
(Can run in parallel with 5.2 and 5.3.)

- `BillingRepository` abstraction. Live: Play Billing, server side purchase token verification,
  `premiumUntil` set only by the Function. Demo: fake purchase screen labelled demo.
- Premium unlocks: liked you list, unlimited likes, rewind, advanced filters. Safety features stay free.
- Prices are read from Play, never hard coded.

### Task 5.5 Privacy and account lifecycle
Status: todo | Branch: `task/5.5-privacy` | Depends on: 5.2

- Full data export, real deletion worker covering Auth, Firestore, Storage and FCM tokens,
  consent records for special category data, privacy notice and terms screens with placeholders
  for legal text.

### Task 5.6 Release hardening
Status: todo | Branch: `task/5.6-release` | Depends on: all above

- R8 rules, signing config read from environment variables, baseline profile, accessibility
  pass (contrast, font scale, TalkBack labels), Somali translation gaps listed, Play listing
  checklist in `docs/RELEASE.md`.

---

## Found issues

(Sessions add problems they notice outside their task scope here.)
