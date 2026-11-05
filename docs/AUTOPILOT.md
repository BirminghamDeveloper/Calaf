# Autopilot mode (owner instruction)
These rules override "one task per session" and "wait for the owner" in CLAUDE.md. All other
CLAUDE.md rules (pinned versions, safety and privacy, budget, verification) still apply.

Owner decisions already made:
- All items under "UX decisions pending owner approval" in PLAN.md are APPROVED. Move them to a
  "Decisions" section marked approved.
- No device testing until the end.
- The hilt-smoke emulator CI job must never block progress: set continue-on-error: true on it.
  If it fails, log it under "Found issues" and continue. Only android and backend checks gate merges.

Progress file (docs/PROGRESS.md): the credit may run out at any moment without warning, so this
file must always describe the current state. Update it inside EVERY task PR, before you push, so
it is merged together with the work. Plain English, written for the owner, not for developers.
Format:
  # Calaf demo progress
  Last updated: <date and time>, after <task>
  ## Done so far
  One section per finished task: what the user can now see or do in the app (2 to 4 lines),
  plus anything important behind the scenes (1 line).
  ## In progress
  The task being worked on now and how far it got.
  ## Still to do
  The remaining demo tasks, one line each, in order.
  ## Known issues
  Anything broken, skipped or unverified.
  ## Decisions made without the owner
  One line each, copied from PLAN.md "Decisions made on autopilot".
  ## How to try the demo
  Where to download the latest demo APK (Actions tab, latest green run on master, Artifacts,
  calaf-demo-debug-apk), how to install it, and the demo logins.

The loop, for PR #5 first, then tasks 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5 in that order.
Never start a deferred task.
1. git fetch origin && git checkout -B <task branch> origin/master
2. Do the task as written in PLAN.md. When a choice is open, pick what best fits a Muzz style
   marriage app and the demo goal, log it in one line under "Decisions made on autopilot" in
   PLAN.md, and keep going. Never stop to ask.
3. Run the full verify from CLAUDE.md. Fix until green.
4. Update PLAN.md (status done, short notes) AND docs/PROGRESS.md (move the task to "Done so
   far", set "In progress" to the next task as "not started"), commit, push. Open the PR with
   the REST API:
   gh api repos/BirminghamDeveloper/Calaf/pulls -f base=master -f head=<branch> -f title=... -f body=...
   (GraphQL is blocked, so gh pr create and gh pr merge do not work.)
5. Poll CI every 2 to 3 minutes:
   gh api repos/BirminghamDeveloper/Calaf/commits/<sha>/check-runs --jq '.check_runs[] | "\(.name) \(.status) \(.conclusion)"'
6. When android and backend are green, merge it yourself (master only allows squash):
   gh api -X PUT repos/BirminghamDeveloper/Calaf/pulls/<n>/merge -f merge_method=squash
   then delete the branch: gh api -X DELETE repos/BirminghamDeveloper/Calaf/git/refs/heads/<branch>
7. If CI is red, fix and push, up to 3 attempts. After 3, mark the task blocked in PLAN.md and
   docs/PROGRESS.md with the reason in plain English, leave the PR open and go to step 9,
   because each demo task depends on the last.
8. Next task. Between tasks write only one line saying which task finished.
9. At the end (3.5 merged, or blocked), open and merge one final PR adding docs/DEMO_REPORT.md:
   a full write up built from docs/PROGRESS.md covering what was built screen by screen, how to
   download and install the demo APK, demo logins, a 5 minute demo walkthrough, known issues,
   autopilot decisions, and what is still deferred for the live app. Then finish with a short
   summary.

Keeping cost down: stay inside each task's written scope, no gold plating, --offline Gradle after
the first good build, compile only touched modules while working, full verify once per task,
and for Maven Central 429 errors rerun with --max-workers=1 -Dorg.gradle.parallel=false.
