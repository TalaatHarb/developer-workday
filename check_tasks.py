import json

with open('project-tasks.json') as f:
    data = json.load(f)

tasks = data['tasks']
remaining = [t for t in tasks if not t.get('passes', False)]

print(f'Total: {len(tasks)}')
print(f'Passing: {len([t for t in tasks if t.get("passes", False)])}')
print(f'Remaining: {len(remaining)}')
print()

if remaining:
    print('Next not-passing task:')
    t = remaining[0]
    print(f'  ID: {t["id"]}')
    print(f'  Title: {t["title"]}')
else:
    print('All tasks are passing!')
