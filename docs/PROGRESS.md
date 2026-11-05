# Calaf demo progress
Last updated: 2026-11-05 07:19 UTC, after Task 1.3 (pull request 5, ready to merge)

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

## In progress
Task 1.3 is finished and being merged. Next up: Task 2.1 (not started).

## Still to do
- Task 2.1 Richer profile and onboarding (more profile fields, consent screen for religion and heritage, step by step setup)
- Task 2.2 Photos (up to 6 illustrated demo photos per profile, photo manager, demo admin approves photos)
- Task 3.1 Swipe cards on Discover
- Task 3.2 Full profile screen and filters
- Task 3.3 "It's a match" moment, who liked you, compliments
- Task 3.4 Chat polish and guardian (wali) view
- Task 3.5 Demo showcase polish (20 demo profiles, walkthrough, Try the demo button, reset, icon, Somali, demo script)

## Known issues
- Nothing has been tried on a phone yet. Everything so far is checked by automatic tests only.
- Somali text for several newer labels is still English (listed in docs/TRANSLATION_TODO.md).

## Decisions made without the owner
- None yet beyond the approved UX decisions in PLAN.md.

## How to try the demo
1. Open the GitHub repository, go to the Actions tab and open the latest green run on master.
2. Under Artifacts download "calaf-demo-debug-apk" and unzip it.
3. Copy the .apk to your Android phone, open it and allow installs from this source.
4. Open "Calaf Demo". Demo logins: type admin as the email and admin as the password for the demo administrator. Other demo members: hassan@calaf.test, amina@calaf.test, hodan@calaf.test or yusuf@calaf.test with the public demo password CalafDemo!2026. These logins are public on purpose and every person in the demo is fictional.
