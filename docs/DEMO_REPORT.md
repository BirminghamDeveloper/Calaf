# Calaf demo report

Written at the end of the autopilot run, 5 November 2026. Source: docs/PROGRESS.md and PLAN.md.
Every task on the demo track (0.1 to 3.5) is merged. Nothing has been tried on a phone yet, only automatic tests and CI.

## What was built, screen by screen

**Start up.** Splash screen in Calaf green, then the Calaf name animation with the idea credit (Ahmed) and development credit (Mustafa). On first launch three tour screens explain the app, with Skip.

**Welcome.** Create account, Log in, and in the demo a Try the demo button. Three promises (intention, culture, privacy), a safety dialog and the 18 and over notice.

**Sign up and profile.** Five short steps with a progress bar and a completion percentage. Marital status, children, height, work, education. Faith and heritage are optional behind a consent switch that is off by default, and turning it off deletes them. There is no clan field and no skin colour filter.

**Photos.** Up to six photos, first is the main one, drag or use Move up and Move down, blur for non matches, approval label on each photo. Ten illustrated avatars, no real people.

**Discover.** A swipe card stack with round buttons, photo pager, blurred photos where chosen, Like with a note (140 characters, safety warnings), Report and Block, a full profile screen and a filter sheet. Friendly empty states.

**Matches.** Matches and Liked you tabs. A full screen It's a match moment. Liked you admirers are simulated and labelled in the demo.

**Chat.** Bubbles, day headings, Sending, Sent and Read, red warnings for links, contact details and money requests, scripted labelled demo replies. Guardian sharing per chat, off by default, read only, and the other person is always told.

**Activity and Profile.** Activity feed with unread count. Settings: preferences, guardian, safety, guidelines, verification, about, and Reset demo data. A demo administrator panel for reports and photo review.

**Showcase data.** 20 fictional members from the UK, Somaliland, Somalia, Djibouti, Ethiopia, Kenya, the US, Canada and Sweden, all named with "(Demo)". A ready made Demo Guest with 2 matches, a started chat, a compliment and 2 people waiting.

## How to install the demo APK
1. Open the GitHub repository, then Actions, and open the latest green run on master.
2. Under Artifacts download "calaf-demo-debug-apk" and unzip it.
3. Copy the .apk to the phone, open it and allow installs from this source.
4. Open "Calaf Demo". The demo has no INTERNET permission, which CI checks.

## Demo logins (public and fictional on purpose)
| Who | Email | Password |
| --- | --- | --- |
| Guest | tap Try the demo | none needed |
| Demo members | hassan, amina, hodan, yusuf at calaf.test | CalafDemo!2026 |
| Administrator | type admin and admin | admin |

## Five minute walkthrough
See docs/DEMO_SCRIPT.md. In short: tour, Try the demo, swipe and filter, like with a note, matches and Liked you, chat with a demo reply and the guardian menu, then admin moderation, and finish with Reset demo data.

## Known issues
- Nothing was tried on a real phone. Visual problems are possible, so test first.
- Somali for several newer labels is still English, and the tour and reset text is a first draft needing a native speaker. See docs/TRANSLATION_TODO.md.
- The chat screen has no empty state picture.
- The hilt-smoke emulator CI job does not block merging, so check its result on the Actions tab.
- GitHub does not let the cloud session delete merged branches, so old task branches remain.
- Release builds (R8) were never tried.

## Decisions made on autopilot
Listed in full in PLAN.md under Decisions made on autopilot. Main ones: hilt-smoke never blocks merging, faith and heritage sit behind one consent switch, guardian sharing is per chat and off by default, risky messages are flagged and never blocked, Liked you stays locked until premium in the live app, swiped cards return if the action fails, blur is done on the phone in the demo.

## Still deferred for the live app
- 1.4 Code style (Spotless) and backend fixes
- 2.3 Live photo pipeline (private and public storage, thumbnails, moderator approval, server blur)
- 4.1 Discovery redesign, 4.2 Push notifications, 4.3 Reliable chat sending
- 5.1 Deployment readiness, 5.2 Moderation and admin roles, 5.3 Selfie verification, 5.4 Premium with Play Billing, 5.5 Privacy and account lifecycle, 5.6 Release hardening

Things only the owner can do: create the Firebase projects and Blaze plan, supply google-services.json and the signing keystore, approve Somali translations, and supply the privacy and terms text.
