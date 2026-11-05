# Calaf demo progress
Last updated: 2026-11-05 08:11 UTC, after Task 3.2

## Done so far

### Task 0.1 Cloud build bootstrap
The project now builds and tests on any machine, and GitHub checks every change automatically.
Behind the scenes: setup script fixed, CI added, old docs moved to docs/history.

### Task 1.1 Flavours and Hilt
There are now two versions of the app. "Calaf Demo" works fully offline with no internet permission. "Calaf" is the real one.
Behind the scenes: dependency injection (Hilt) added, the demo login shortcut lives only in the demo version.

### Task 1.2 Demo backend split
The demo's fake database is now organised properly and every part has tests. Fixed a bug where blocking someone you never matched with created a fake match notification.
Behind the scenes: the demo admin is now a role on the account, and all demo passwords are in one documented place.

### Task 1.3 Feature modules and ViewModels
The app now has a real bottom bar with icons: Discover, Matches, Activity, Profile. Activity shows how many new things you have.
Report and Block are one tap away on each match. Messages are reached from Matches. Settings is the gear icon at the top.
Behind the scenes: the app is split into separate modules, each screen has its own tested logic, and a smoke test opens every tab on an emulator in CI.

### Task 2.1 Richer profile and onboarding
Setting up a profile is now five short steps with a progress bar and a "% complete" score: the basics, in your words, your life (marital status, height, children, work, education), faith and heritage, then review.
Faith and heritage are optional and hidden behind a clear consent switch that is off by default. Turning it off deletes those details. Other members only ever see them if you agreed.
Behind the scenes: the server refuses faith or heritage data without consent, the demo database upgrades old data safely, and there is still no clan field.

### Task 2.2 Photos
Your profile now has a photo manager: up to six photos, the first is your main photo, drag to reorder (or use Move up and Move down), make any photo the main one, blur a photo for people you have not matched with, and remove photos. Each photo shows whether it is approved.
Ten illustrated demo avatars come with the app (abstract art, no real people), and every demo member has two or three. In the demo new photos are approved straight away. The demo administrator has a photo review list on the Profile tab to approve or reject any photo, and a rejected photo disappears for everyone else.
Behind the scenes: photos have a proper model (approved, pending, rejected), the old demo photo files upgrade automatically, and the real app only shows your own pending photos until the live photo pipeline is built.

### Task 3.1 Swipe cards
Discover is now a stack of profile cards like a dating app. Drag a card right to like or left to pass, or use the round buttons under it, which do exactly the same. Report and Block sit next to those buttons.
Each card shows a big photo (tap the right or left side to see the next or previous photo), name, age, city, heritage, job, marriage timeline and the answers you share. Photos the owner chose to blur are blurred.
More people load automatically before the stack runs out, and there are friendly loading and "that is everyone for now" screens. TalkBack users get Like and Pass actions on the card.
Behind the scenes: the swipe rules (how far, how fast) are separate and tested, and a swiped card returns if the request fails.

### Task 3.2 Full profile screen and filters
Tap the info button (or the name area) on a card to open that person's full profile: all their photos you can swipe through, about, the basics, values and interests, a faith section if they chose to share it, and a breakdown of what you share and where you differ. You can still pass, like, report or block from there, and the phone's Back button returns to the cards.
The new Filters button on Discover opens a sheet: age, language, non smokers, marital status, children, height, marriage timeline, willing to relocate, country of birth, profession, education, and (only for people who shared it) how practising, prayer and heritage. The button shows how many filters are on. Show results saves them and reloads the cards.
Behind the scenes: one filter rule is shared and tested, the server mirrors it, and a filter never matches someone who left that field blank.

## In progress
Task 3.2 is finished and being merged. Next up: Task 3.3 Match moment, likes and compliments (not started).

## Still to do
- Task 3.3 "It's a match" moment, who liked you, compliments
- Task 3.4 Chat polish and guardian (wali) view
- Task 3.5 Demo showcase polish (20 demo profiles, walkthrough, Try the demo button, reset, icon, Somali, demo script)

## Known issues
- Nothing has been tried on a phone yet. Everything so far is checked by automatic tests only.
- Somali text for several newer labels is still English (listed in docs/TRANSLATION_TODO.md).
- GitHub does not let me delete merged branches, so old task branches stay on the repository (harmless).

## Decisions made without the owner
- hilt-smoke emulator CI job never blocks merging.
- "Prays" is a four option prayer habit (always, usually, sometimes, never).
- Religion, how practising, prayer and heritage all sit behind one consent switch that is off by default.
- Old religion text without a consent record is cleared when the demo database upgrades (fictional seeded members keep theirs). The server rejects such data instead of silently dropping it.
- New Somali strings are English copies for now (filled in at Task 3.5).
- The main photo is always the first photo, so choosing a main photo and dragging to the front are the same thing.
- Blur is chosen per photo by the owner. The card that actually draws the blur arrives with the swipe cards (Task 3.1).
- The full profile opens inside Discover and closes with the Back button. A filter never matches someone who left that field blank. Heritage and faith filters only match people who chose to share them.
- A swiped card disappears immediately and comes back if the action fails. More cards load when three are left.
- Blurred photos are blurred on the phone by shrinking and enlarging them, so the sharp image is never drawn. The real app will need blurred copies made by the server.
- Photo reordering works by long press drag and by Move up and Move down buttons.

## How to try the demo
1. Open the GitHub repository, go to the Actions tab and open the latest green run on master.
2. Under Artifacts download "calaf-demo-debug-apk" and unzip it.
3. Copy the .apk to your Android phone, open it and allow installs from this source.
4. Open "Calaf Demo". Demo logins: type admin as the email and admin as the password for the demo administrator. Other demo members: hassan@calaf.test, amina@calaf.test, hodan@calaf.test or yusuf@calaf.test with the public demo password CalafDemo!2026. These logins are public on purpose and every person in the demo is fictional.
