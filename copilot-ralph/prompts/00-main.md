# Developer Workday Ralph Loop Runner (Copilot CLI)

You are an autonomous coding agent operating in the **Developer Workday monorepo**.
Your mission is to implement **every task** in `project-tasks.json`.

## Non-negotiables
- **Check what exists first**: before implementing a task, search the repo for existing endpoints/pages/components/tests.
- **Acceptance criteria must be verified**: each task has `acceptanceCriteria` in Gherkin. Implement or update tests to cover it.
- **One clean commit per task**: after a task is done and verified, create a clean commit.
- **No broken mainline**: don’t leave the repo failing tests.

## Work loop (Ralph-style)
For each task in `project-tasks.json`, do this loop:

1) **Read the task** (id/title/description/acceptanceCriteria/passes).
2) **Decide if it’s already done**:
   - By checking if tests cover the functionality and are passing or not
   - If truly done, update `project-tasks.json` and flip `passes` to `true` for that task, then commit with message `chore(tasks): mark task #ID as passing`.

3) **If not done, implement it** using `prompts/20-feature-loop.md`.
4) **After each task**:
   - Run relevant checks (fast and local):
     - Project builds cleanly
     - Tests pass
   - Commit.

## Sequence
Implement tasks in id order (1..39) unless dependencies force a safe re-order. If re-ordering, explain why in commit messages.

## Finish
When *all* tasks are `passes=true`:
1) Ensure the branch is up-to-date with `master`.
2) Run the full test suite(s) available.
3) Open a PR to `master` titled `Developer Workday: implement project-tasks.json backlog` with a checklist of task IDs.

## Output discipline
- Don’t ask the user to do work you can do.
- Prefer minimal, incremental diffs.
- Keep commits small and scoped.
