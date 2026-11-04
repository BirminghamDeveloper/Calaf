# Calaf Demo

Install `dist/Calaf-offline-demo.apk` directly on your Android phone and open **Calaf Demo**. This separate app has package `com.calaf.app.demo`; it does not replace the computer-connected Calaf app or use its accounts. No USB setup, Firebase service, email delivery or internet connection is needed.

## Accounts

- Administrator: `admin` / `admin`
- Hassan: `hassan@calaf.test` / `CalafDemo!2026`
- Amina: `amina@calaf.test` / `CalafDemo!2026`
- Hodan: `hodan@calaf.test` / `CalafDemo!2026`
- Yusuf: `yusuf@calaf.test` / `CalafDemo!2026`

Registration creates a member on this device, immediately usable without email verification. Use fictional information. New-member passwords are salted and hashed. There is no email password recovery in the offline build. Accounts, profile drafts, messages and administrative actions survive app restarts. Uninstalling or clearing app storage removes this data. No account or message syncs between phones.

## Administrator

Log in as admin, open Profile, scroll to **Administrator controls**, and tap **Open administrator panel**. View demo members, suspend/restore members and resolve/dismiss reports. Changes require confirmation and are recorded in the local action log. Suspended members cannot log in or appear in discovery. Normal member accounts cannot call these administrator operations. The administrator cannot suspend itself. A fictional sample report is included for trying the panel.

These privileges apply only to this demo. They do not grant Firebase console privileges or production moderation access.

## What can be tried

Profile editing and publishing, private device photo upload/removal, preference filters, discovery, likes/passes, matches, text conversations, activity, block/report/unmatch, privacy and guardian invitations between device-local accounts. Seeded fictional profiles simulate reciprocal likes; created member accounts require both accounts to like each other. Switch between accounts to read their messages. No real person receives anything. Remote delivery/read receipts, real identity verification, payments and email are not simulated as completed services. Export remains partial and deletion remains a recorded request.

## Build

`./gradlew.bat :app:assembleDemoDebug :app:assembleDemoDebugAndroidTest`

The demo flavour manifest removes internet permission and the Firebase initialization provider. The normal debug/release configurations retain their original backend behaviour.

## Verified build - 3 November 2026

Eight account unit tests and five Android 14 device tests passed, including standalone login/registration and administrator restrictions. No physical-phone verification has been performed. See [the detailed handoff](IMPLEMENTATION_STATUS_AND_PLAN.md) for remaining work and reproduction commands.
