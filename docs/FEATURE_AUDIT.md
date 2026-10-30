# Calaf implementation audit

Product: a social marriage platform for Somali and East African people.
Credits: app idea by Ahmed; developed by Mustafa.

This tracks the supplied requirements, not a claim that a screen means a service
is finished. The supplied brief ends at section 49's heading.

| Requirement | Starting state | Acceptance needed |
| --- | --- | --- |
| Build toolchain (1–3) | Configured; debug builds | Release build/signing and CI |
| Purpose, design, accessibility (4–7) | Theme, credits, English/draft Somali | Full accessibility/localisation review |
| Architecture/modules/models (8–10) | Three modules, small account models | Service boundaries, domain models, Hilt, feature modules |
| Authentication (11) | Email login/register/reset/verification/session/logout | Google, phone, recent-auth deletion, revoke sessions |
| Firebase and data design (12–13) | Auth SDK only | Functions, rules, indexes, App Check and emulator tests |
| Onboarding/profile/photos (14–16) | Local text draft | Cloud profile, expanded fields, photo processing/moderation |
| Preferences/discovery/compatibility (17–19) | Missing | Filtered discovery, explanations, server eligibility |
| Likes/matches (20) | Missing | Transactional mutual matches, deduplication, quotas, unmatch |
| Chat/safety (21–22) | Missing | Matched-only messages, pagination, states, warnings |
| Guardian (23) | Missing | Consent-based invite/accept/revoke; no implicit chat access |
| Verification (24) | Email only | Phone, selfie/manual review; real ID provider unavailable |
| Safety/location/privacy (25–27) | Safety text only | Stored reports, blocks, approximate location, privacy enforcement |
| Free/premium/billing (28–30) | Missing | Play products and backend-verified entitlements |
| Notifications (31) | Missing | FCM, permission, categories, preferences, deep links |
| Localisation/navigation/settings (32–35) | Basic routes, English/draft Somali | Four main destinations and settings flows |
| Security/secrets/offline/errors (36–40) | No secrets in code, backup excluded, auth errors | Rules, retries, queued work, secure release, cache policy |
| Validation/age/moderation (41–43) | Client validation only | Server eligibility, moderation workflow and audit trail |
| Analytics/crashes (44–45) | Missing | Consent-aware diagnostics without sensitive parameters |
| Tests/quality/performance (46–48) | 9 unit tests; 3 UI/repository tests | Resolve UI clock timing, backend and expanded UI tests |

## External release dependencies

- Firebase Cloud Functions/Storage deployment requires the project's supported billing
  plan and deploy credentials. No deployment has been performed by this project.
- Play Console subscription products, publisher API access and test accounts are not
  configured. Premium must stay locked until verified by the backend.
- Government-ID verification needs a contracted provider. Never issue a genuine ID
  badge from a local mock or developer account.
- Production moderation operations, privacy policy, terms, support contacts and
  release signing must be supplied before launch.

Implementation and validation evidence will be recorded as each phase completes.


## Development implementation update — 29 October 2026

The table above describes the initial audit, not the current finished feature set.
Implemented locally since that audit: server profile publication and adult eligibility,
safe discovery projections, partner filters and transparent compatibility, transactional
mutual matching, matched text messaging, safety warnings, blocking/unmatching/reports,
guardian invite/accept/revoke, privacy controls, session revocation, partial export and
deletion-request recording. These server features are not deployed to live Firebase.

This iteration fixes local login setup feedback, duplicate-email feedback, password
confirmation, token refresh after verification, explicit debug-only emulator verification,
and graceful emulator account persistence. It adds older chat history, privacy-controlled
visible-message read receipts, an in-app activity inbox, a children preference filter,
and profile-first entry after sign-in. Push notifications and actual erasure remain absent.
Validation for this iteration is limited to a debug build and targeted local connection
checks, per the request to prioritize feature implementation over test-suite expansion.


## Verification and demo access update — 29 October 2026

Added local-only admin/admin demo-member login and a seeded mutual match. Corrected
local Firebase option formats after device testing exposed a callable-request failure.
Added 124 draft Somali community translations and fixed debug network configuration.
The complete local verification command now passes: 16 backend tests, 17 JVM tests,
6 Android 14 device tests; debug APK builds and both app/community lint pass.
See VERIFICATION.md for coverage and limitations, and PROGRESS.md for the next-work checkpoint.
