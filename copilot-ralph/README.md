# Copilot CLI + Ralph Loops (developer-workday)

This folder contains a **Ralph-style “loops” prompt pack** to drive GitHub Copilot CLI through implementing the backlog in `../project-tasks.json`.

Goals:
- Implement each feature with the existing stack.
- **Check existing implementation first** (don’t duplicate).
- **Use mock server data** and mocks to verify behavior end-to-end.
- **Verify acceptance criteria** (Gherkin in `project-tasks.json`) and **build tests**.
- **One clean commit per task**.

## Runner script (progress/plan)

Use `run-loop.sh` to keep a lightweight Ralph-style execution loop and track progress:

```bash
chmod +x copilot-ralph/run-loop.sh

# show progress and remaining tasks
./copilot-ralph/run-loop.sh plan

# show next task id not passing
./copilot-ralph/run-loop.sh next

# show a task details (description + Gherkin)
./copilot-ralph/run-loop.sh show 14

# create/switch to the working branch and print task guidance
./copilot-ralph/run-loop.sh start 14

# mark a task as passing (ONLY if it truly passes) and commit
./copilot-ralph/run-loop.sh done 14
```

## Prompts

- `prompts/00-main.md` — entrypoint prompt (the full loop runner)
- `prompts/10-rules.md` — global rules (quality gates, mocking, verification)
- `prompts/20-feature-loop.md` — per-task loop template
- `prompts/30-git-and-pr.md` — branching/commits/PR workflow

## Copilot CLI usage

If Copilot CLI is available, the runner can print a suggested command:

```bash
./copilot-ralph/run-loop.sh run-copilot
```
