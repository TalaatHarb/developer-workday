#!/usr/bin/env bash
set -euo pipefail

# Ralph-style loop runner for developer-workday
# - Tracks progress from ../project-tasks.json
# - Prints a plan (todo/pass counts)
# - Provides helpers to start/finish tasks with clean commits
# - Can launch the standalone GitHub Copilot CLI if installed

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TASKS_JSON="$ROOT_DIR/project-tasks.json"
PROMPT_MAIN="$ROOT_DIR/copilot-ralph/prompts/00-main.md"

usage() {
  cat <<'USAGE'
Usage: ./copilot-ralph/run-loop.sh <command> [args]

Commands:
  plan                 Print overall progress and list remaining tasks
  status               Alias for plan
  next                 Print the next task ID that is not passing
  show <id>            Print task details for a given task ID
  start <id>           Create (or checkout) the working branch and print guidance for task
  loop <id>            Start the interactive Copilot CLI loop for a task (runs `copilot`)
  loop-next            Start Copilot for the next not-passing task
  loop-all             Start one Copilot session that iterates remaining tasks until done
  done <id>            Mark task passes=true (ONLY if truly done) and commit (chore)
  run-copilot          Print suggested Copilot CLI invocation using the main prompt

Notes:
- `start` prints guidance and does NOT automatically run Copilot.
- `loop` will try to run the standalone `copilot` CLI (not `gh copilot`).
- It assumes tasks live in ../project-tasks.json.
USAGE
}

need_python() {
  command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }
}

need_copilot_cli() {
  command -v copilot >/dev/null 2>&1 || {
    echo "Copilot CLI not found in PATH. Install it so `copilot --help` works." >&2
    exit 1
  }
}

plan() {
  need_python
  python3 - <<PY
import json
from pathlib import Path
p = Path(r"$TASKS_JSON")
obj = json.loads(p.read_text())
tasks = obj["tasks"]
passed = [t for t in tasks if t.get("passes") is True]
remaining = [t for t in tasks if not t.get("passes")]
print(f"Tasks: {len(tasks)} | Passing: {len(passed)} | Remaining: {len(remaining)}")
print("\nRemaining (passes=false):")
for t in remaining:
    print(f"  #{t['id']:>2}  {t['title']}")
PY
}

next_id() {
  need_python
  python3 - <<PY
import json
from pathlib import Path
p = Path(r"$TASKS_JSON")
obj = json.loads(p.read_text())
for t in obj["tasks"]:
    if not t.get("passes"):
        print(t["id"])
        raise SystemExit(0)
print("")
PY
}

show_task() {
  local id="${1:-}"
  if [[ -z "$id" ]]; then
    echo "Missing task id" >&2
    exit 1
  fi
  need_python
  python3 - <<PY
import json
from pathlib import Path
p = Path(r"$TASKS_JSON")
obj = json.loads(p.read_text())
_id = int("$id")
for t in obj["tasks"]:
    if t["id"] == _id:
        print(f"# {t['id']}: {t['title']}")
        print(f"passes: {t.get('passes', False)}")
        print("\nDescription:\n" + t.get("description", ""))
        print("\nAcceptance criteria (Gherkin):\n" + t.get("acceptanceCriteria", ""))
        break
else:
    raise SystemExit(f"Task id not found: {_id}")
PY
}

ensure_branch() {
  local branch="feature/project-tasks"

  if ! git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Not a git repo: $ROOT_DIR" >&2
    exit 1
  fi

  # Best-effort fetch (don't fail if offline)
  git -C "$ROOT_DIR" fetch origin master >/dev/null 2>&1 || true

  if git -C "$ROOT_DIR" show-ref --verify --quiet "refs/heads/$branch"; then
    git -C "$ROOT_DIR" checkout "$branch" >/dev/null
  else
    if git -C "$ROOT_DIR" show-ref --verify --quiet refs/heads/master; then
      git -C "$ROOT_DIR" checkout -b "$branch" master >/dev/null
    else
      git -C "$ROOT_DIR" checkout -b "$branch" >/dev/null
    fi
  fi

  echo "$branch"
}

start_task() {
  local id="${1:-}"
  if [[ -z "$id" ]]; then
    echo "Missing task id" >&2
    exit 1
  fi

  local branch
  branch="$(ensure_branch)"

  echo "Branch: $branch"
  echo
  show_task "$id"
  echo
  cat <<EOF
Next steps:
- Follow: copilot-ralph/prompts/20-feature-loop.md
- Build tests to verify the acceptance criteria.
- Commit once the task is done: feat(#$id): <short title>
- Flip passes=true for task #$id in project-tasks.json in the same commit.

To launch Copilot CLI for this task:
  ./copilot-ralph/run-loop.sh loop $id
EOF
}

loop_task() {
  local id="${1:-}"
  if [[ -z "$id" ]]; then
    echo "Missing task id" >&2
    exit 1
  fi

  ensure_branch >/dev/null
  need_copilot_cli

  # Build a compact “task context” file the agent can read.
  local tmp
  tmp="$(mktemp -t developer-workday-task-${id}-XXXXXX.md)"
  {
    echo "# Developer Workday task #$id"
    echo
    show_task "$id"
    echo
    echo "---"
    echo
    echo "## Repo pointers"
    echo "- Main loop prompt: $PROMPT_MAIN"
    echo "- Per-task loop:    $ROOT_DIR/copilot-ralph/prompts/20-feature-loop.md"
    echo "- Git & PR rules:   $ROOT_DIR/copilot-ralph/prompts/30-git-and-pr.md"
    echo
    echo "## Hard requirements"
    echo "- Check existing implementation first; don't duplicate."
    echo "- Verify the acceptance criteria (Gherkin) and add/extend automated tests."
    echo "- Make ONE clean commit for the feature once complete."
  } >"$tmp"

  echo "Launching Copilot CLI with task context: $tmp"
  echo "(Exit Copilot when done; the temp file will remain for reference.)"

  # The standalone `copilot` CLI expects prompts via -i (interactive) or -p (non-interactive),
  # not as positional arguments.
  local combined_prompt
  combined_prompt=$(cat <<EOF
You are working in the Developer Workday monorepo at: $ROOT_DIR

Read and follow the main loop instructions in:
$PROMPT_MAIN

Also read the per-task loop template:
$ROOT_DIR/copilot-ralph/prompts/20-feature-loop.md

Now open and follow the task context:
$tmp

Start by:
1) Searching the repo for existing implementation/tests for this task.
2) Listing what is missing vs acceptance criteria.
3) Implementing features, and adding tests.
4) Running tests.
5) Making a clean commit: feat(#$id): <short title> (and flipping passes=true only if truly passing).

When task #$id is complete (acceptance criteria met, tests pass, commit created, and passes=true flipped), EXIT Copilot immediately.
EOF
)

  copilot \
    --allow-all-tools \
    --allow-all-paths \
    --add-dir "$ROOT_DIR" \
    --add-dir "$(dirname "$tmp")" \
    -i "$combined_prompt"
}

loop_next() {
  local id
  id="$(next_id)"
  if [[ -z "$id" ]]; then
    echo "No remaining tasks (all passing)."
    exit 0
  fi
  loop_task "$id"
}

loop_all() {
  ensure_branch >/dev/null
  need_copilot_cli

  local combined_prompt
  combined_prompt=$(cat <<EOF
You are working in the Developer Workday monorepo at: $ROOT_DIR

Read and follow the main loop instructions in:
$PROMPT_MAIN

Also read the per-task loop template:
$ROOT_DIR/copilot-ralph/prompts/20-feature-loop.md

Now run a continuous Ralph loop:
1) Determine the next not-passing task id by running: ./copilot-ralph/run-loop.sh next
2) Print the task with: ./copilot-ralph/run-loop.sh show <id>
3) Implement it (FXML + Controller + Facade + Service + Mapper + Repository + Model + Event + Config + Utils as needed) and add/update tests.
4) Run relevant tests.
5) Make ONE clean commit for that task and flip passes=true for that task in project-tasks.json in the same commit.
6) Repeat from step (1) until ./copilot-ralph/run-loop.sh next prints nothing.

When all tasks are passing, EXIT Copilot.
EOF
)

  copilot \
    --allow-all-tools \
    --allow-all-paths \
    --add-dir "$ROOT_DIR" \
    -i "$combined_prompt"
}

done_task() {
  local id="${1:-}"
  if [[ -z "$id" ]]; then
    echo "Missing task id" >&2
    exit 1
  fi

  need_python
  ensure_branch >/dev/null

  python3 - <<PY
import json
from pathlib import Path
p = Path(r"$TASKS_JSON")
obj = json.loads(p.read_text())
_id = int("$id")
for t in obj["tasks"]:
    if t["id"] == _id:
        t["passes"] = True
        break
else:
    raise SystemExit(f"Task id not found: {_id}")

p.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")
PY

  git -C "$ROOT_DIR" add "$TASKS_JSON"
  git -C "$ROOT_DIR" commit -m "chore(tasks): mark task #$id as passing" || {
    echo "Nothing to commit (already passing?)" >&2
    exit 1
  }

  echo "Marked task #$id as passing and committed."
}

run_copilot() {
  if command -v copilot >/dev/null 2>&1; then
    cat <<EOF
Copilot CLI detected.

Suggested invocation:

  ./copilot-ralph/run-loop.sh loop "\$(./copilot-ralph/run-loop.sh next)"

Main prompt file:
  $PROMPT_MAIN
EOF
  else
    cat <<EOF
Copilot CLI not detected.

Use the prompts manually:
- $PROMPT_MAIN
- $ROOT_DIR/copilot-ralph/prompts/20-feature-loop.md
EOF
  fi
}

cmd="${1:-}"
shift || true

case "$cmd" in
  plan|status)
    plan
    ;;
  next)
    next_id
    ;;
  show)
    show_task "$@"
    ;;
  start)
    start_task "$@"
    ;;
  loop)
    loop_task "$@"
    ;;
  loop-next)
    loop_next
    ;;
  loop-all)
    loop_all
    ;;
  done)
    done_task "$@"
    ;;
  run-copilot)
    run_copilot
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    usage
    exit 1
    ;;
esac
