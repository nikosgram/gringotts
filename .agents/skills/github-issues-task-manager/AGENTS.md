# Agent instructions

GitHub Issues is the task manager for every agent in this repository. The issue (body + comments + checklist + native fields) is the source of truth. Do not plan or change code until the work lives in a detailed issue.

`AGENTS.md` is the only policy file. Claude, Gemini, and Copilot entry paths are **symlinks** to this file. The Claude skill path is a **symlink** to `.cursor/skills/github-issues-task-manager/SKILL.md`. Edit those two files only.

| Agent | What it loads |
| --- | --- |
| Codex | `AGENTS.md` |
| Cursor | `AGENTS.md`, `.cursor/rules/github-issues-task-manager.mdc`, `.cursor/skills/github-issues-task-manager/SKILL.md` |
| Claude Code | `CLAUDE.md` → `AGENTS.md`, `.claude/skills/github-issues-task-manager/SKILL.md` → Cursor skill |
| GitHub Copilot | `AGENTS.md` and `.github/copilot-instructions.md` → `AGENTS.md` |
| Gemini CLI | `GEMINI.md` → `AGENTS.md` |

## Before any work

1. Search **open** and **recently closed** issues (`gh issue list`, `gh issue list --state closed --limit 20`, `gh search issues`).
2. **Duplicate or near-duplicate:** do not open a new issue. Comment on the existing one that it was requested again, summarize the new request, and continue from that issue if work should proceed. Add `duplicate` only when closing a mistaken extra issue.
3. **Similar with small differences:** comment on the existing issue with the requested deltas. Open a new issue only if the difference is a distinct deliverable; link both issues and set **blocked by** / **blocking** when one must wait on the other.
4. Otherwise create a **root issue** (and **sub-issues** when the work splits). Then create the development branch through GitHub so the branch is natively linked to the issue.

## Issue quality

Every issue must include: summary/why, duplicate-check result, scope (in/out), implementation details, testing, acceptance criteria, and a checklist. Put every detail the next agent needs in the issue — not only in chat.

Classify with **native GitHub features**, not stacked classification labels:

| Need | Use |
| --- | --- |
| Kind of work | Issue **type**: `Bug`, `Feature`, or `Task` |
| Urgency | Issue field **Priority**: Urgent / High / Medium / Low |
| Size | Issue field **Effort**: High / Medium / Low |
| Parent / child | **Sub-issues** (`parentIssueId` or sub-issues API) |
| This cannot start yet | **Blocked by** those issues |
| Others wait on this | **Blocking** those issues (they proceed after this one) |
| Dates | **Start date** / **Target date** when useful |
| Area | Label `area:*` that matches this repo (suggested: `area:api` `area:ui` `area:db` `area:i18n` `area:infra` `area:dx`) |
| Agent-run | Label `agent` |

Do **not** add `type:*`, `difficulty:*`, `priority:*`, or `status:*` on new issues. `status:blocked` is replaced by **blocked by**. In-progress is a comment + assignee, not a label.

If the host org is missing issue types or Priority / Effort, look them up, use what exists, and say so in the issue. Do not invent stacked classification labels as a substitute unless the user asks.

Assign the current GitHub user. Commands: `.cursor/skills/github-issues-task-manager/SKILL.md`.

## Branch, implement, update

1. Create and check out the linked development branch from `main` with GitHub CLI: `gh issue develop <root-number> --name issue-<root-number>-short-slug --base main --checkout`. This GitHub operation is required; a branch name containing the issue number is not a substitute for the native issue-to-branch link. If the branch already exists, inspect `gh issue develop --list <root-number>` before reusing it and preserve or repair the native link through GitHub.
2. Read the **full issue body, all comments, type, fields, sub-issues, and dependencies** before continuing.
3. Implement only what the issue specifies. Do not include unrelated local files.
4. When an issue or sub-issue is finished: **commit, push the branch, update the issue** (comment + checklist), then close it if acceptance criteria are met. Closing a blocker unblocks issues that were **blocked by** it.
5. Post progress comments as you go. If a checklist exists, keep it current.
6. In repositories that use this skill, finishing an issue/sub-issue **includes commit and push**.

## Testing

Before opening or updating a PR, **run every item** in the issue Testing section and the PR Test plan.

- Check off each item the agent ran. Comment on the issue or PR with the command and the result.
- If a test cannot be run (GitHub UI-only, another product, missing secrets, no deploy, hardware, and so on), do **not** skip it silently. Add a **Manual tests** section on the PR with step-by-step instructions the user can follow, and leave that checkbox unchecked (or mark it `manual`).
- Prefer writing Testing items that an agent can execute (`gh`, git, unit/integration commands). Mark human-only checks as such when filing the issue.

## CI pipelines are collaborators

Discover repository workflows and check the current branch or PR pipeline before claiming work is complete. After every push, monitor the resulting checks to a terminal state. A pending check is not a failure, and a locally passing test does not override a failed required check.

When a check fails:

1. Read the failing job, step, annotations, and available logs before editing.
2. Classify it as caused by the change, an unrelated baseline failure, flaky/transient, or infrastructure/external. Record the evidence; do not guess from the check name alone.
3. Fix change-caused failures within the issue scope, run the smallest relevant local reproduction plus the required test plan, commit, push, and monitor the replacement run.
4. Rerun a job without a code change only when evidence supports a transient failure. Never disable, bypass, or weaken a required check.
5. If the failure is unrelated or fixing it would expand scope, report it on the PR and root issue with the check/job URL and the next action or blocker. Create/link a separate issue when follow-up work is required.

Post concise PR and issue comments for material transitions: failure diagnosed, remediation pushed, checks recovered, or an external blocker confirmed. Include check names, links, relevant error evidence, commit SHA, and verification commands. Do not post a comment for every poll. For non-GitHub providers, follow the details URL exposed by the commit status/check run and use the provider's available read or retry interface; do not invent provider-specific commands.

## After the root issue

Open a PR into `main`. Fill `.github/pull_request_template.md`, include the native closing reference `Closes #<root>` (or the appropriate GitHub closing keyword) in the PR body, and verify the created PR shows the issue in its linked issues / Development context. A plain URL, branch name, or unrelated mention is not enough. Assign the PR. Add `agent` if an agent opened it, plus `area:*` when useful. Do not restack type/priority labels on the PR. Do not merge unless asked.

The PR Test plan must show what the agent ran. Any item the agent could not run must have step-by-step **Manual tests** instructions on that same PR.

Creating the PR does **not** finish the workflow. Immediately run this self-review and remediation loop:

Before self-review, inspect the PR checks. Continue useful review work while checks run, but do not publish the final completion report until required checks reach a successful terminal state or an external blocker is explicitly documented.

1. Read the complete PR diff and changed files. Review correctness, security, regressions, tests, accessibility, maintainability, and compliance with the root issue.
2. Publish every actionable finding as an inline PR review comment on the changed line that needs work. Use one thread per finding and explain the problem, impact, and expected correction. Do not create findings for praise or optional preferences.
3. After publishing all findings, create one detailed sub-issue of the root issue per actionable review thread. Link the PR, inline comment/thread, and affected path/line; include scope, implementation, testing, acceptance criteria, and a checklist. Comment on the root issue with the review result and sub-issue list.
4. Fix sub-issues one at a time. Before each fix, comment on its sub-issue that work started. Run its tests, make one focused commit, push immediately, then comment with the commit SHA, test evidence, and PR thread URL. Keep its checklist current.
5. Re-read the pushed diff and verify the finding is corrected. Resolve the associated PR review thread only after verification, then comment on and close the sub-issue. If it is not corrected, leave both open and continue the same sub-issue.
6. Repeat until every actionable finding sub-issue is closed and every associated review thread is resolved. Run the complete issue and PR test plans again when cumulative changes could interact.
7. Perform a final self-review of the entire updated PR. New actionable findings restart steps 2–6; do not declare completion while any actionable finding, finding sub-issue, or self-review thread remains open.
8. Comment on the PR and root issue with the final review result, commits, tests, closed sub-issues, and any manual tests still required. Update checklists. Leave the PR open and unmerged unless the user asked to merge.

If an actionable finding spans multiple files, anchor the inline comment to the most relevant changed line and describe the full scope there. GitHub only permits inline comments on the PR diff; if no changed line can truthfully anchor the finding, post a PR-level review comment, create the same required sub-issue, and explicitly record why an inline location was unavailable.
