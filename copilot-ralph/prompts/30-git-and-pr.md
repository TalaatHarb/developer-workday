# Git + PR Workflow Rules

## Branching
- Create a working branch off `master`:
  - `feature/project-tasks`

## Clean commits (required)
For each implemented task:
- Ensure tests pass.
- `git status` is clean except intended files.
- Commit message format:
  - `feat(#ID): <task title fragment>`
  - `test(#ID): ...` (if test-only)
  - `chore(#ID): ...` (meta, docs)

## PR to master
When all tasks are complete (`passes=true` for all):
- Rebase or merge `master` (your choice, but keep history readable).
- Run full test suite(s) available.
- Open PR to `master`:
  - Title: `Developer Workday: implement project-tasks.json backlog`
  - Body: checklist of completed task IDs (1..39)
  - Mention key commands used to verify.

## No partial PR
Do not open a PR until the entire list is complete unless explicitly requested.
