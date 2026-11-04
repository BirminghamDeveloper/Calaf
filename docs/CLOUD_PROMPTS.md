# Prompts for Claude Code cloud sessions

Use one new session per task. Model: Sonnet 5.5. Environment: `calaf-android` (see `docs/CLOUD_SETUP.md`).

## 1. Task prompt (use for every task, change only the number)

```
Read CLAUDE.md and PLAN.md in full before doing anything.

Do Task 0.1 from PLAN.md and nothing else.

Follow CLAUDE.md exactly: pinned versions, safety rules, budget rules, the branch name from
the task, and the verification commands. Update the task status in PLAN.md, then open one
pull request against master.

If something blocks you (a blocked network host, a version that will not resolve, an unclear
requirement), stop, write what blocked you in PLAN.md and the PR description, and finish.
Do not downgrade, skip tests or invent workarounds.
```

## 2. Fix prompt (same session, when CI fails or you spot a problem on your phone)

```
The PR for this task has a problem: <paste the CI error or describe what you saw on the phone>.
Fix only this, inside the current task scope, rerun the verification commands and push to the
same branch.
```

## 3. Resume prompt (new session, when a task stopped half way)

```
Read CLAUDE.md and PLAN.md. Task <number> was started on branch <branch> but not finished.
Check out that branch, read the PR description and the task notes in PLAN.md, finish the
remaining work only, verify, and update the same PR.
```

## 4. Review prompt (optional, before merging a big PR)

```
Review PR #<number> against CLAUDE.md and its task in PLAN.md. Check scope creep, safety and
privacy rules, missing tests and anything that would break the demo flavour. Do not change code.
Reply with a short list of problems, most serious first.
```
