import json
import sys

task_id = int(sys.argv[1]) if len(sys.argv) > 1 else None
if not task_id:
    print("Usage: python show_task.py <task_id>")
    sys.exit(1)

with open('project-tasks.json', 'r', encoding='utf-8') as f:
    obj = json.load(f)

for t in obj['tasks']:
    if t['id'] == task_id:
        print(f"# {t['id']}: {t['title']}")
        print(f"passes: {t.get('passes', False)}")
        print(f"\nDescription:\n{t.get('description', '')}")
        print(f"\nAcceptance criteria (Gherkin):\n{t.get('acceptanceCriteria', '')}")
        break
else:
    print(f"Task {task_id} not found")
    sys.exit(1)
