# Per-Feature Loop Template

Use this template for implementing a single task from `project-tasks.json`.

## 0) Inputs
- Task ID: <ID>
- Title: <TITLE>
- Acceptance criteria (Gherkin):
  <PASTE>

## 1) Inspect existing implementation
- Search UI folders (FXML files and Controllers) pages/components/services related to the feature.
- Make sure that facade -> service -> repository actions are implemented and not just empty implementation.
- Search unit tests for existing scenarios.

## 2) Decide approach and API contract
Write a tiny contract:
- Endpoint(s) (method + path)
- Request/response JSON shape
- Auth/RBAC rules
- Error cases

Keep it consistent between BE and mock.

## 3) Write/Update tests first (minimum)
- Add/extend unit test file(s) when the criteria aren't UI-focused.
- Add integration tests for complex logic and UI-focused features.

## 4) Implement incrementally
- Implement features using MVC best practices and in a clear hierarchy (UI Controllers with basic validation rules -> Facade for the wiring together services and mapping their output for controllers-> (mappers from entities to DTOs/ViewModels + services handling entities) -> Repositories for hiding MapDB interactions) .

## 5) Verify acceptance criteria using mock data
- Run tests relevant to the feature.

## 6) Quality gates
- Format/lint checks if present.
- unit and integration tests.
- Any project build checks.

## 7) Commit
Make a single clean commit:
- Message: `feat(#<ID>): <short title>` or `chore(#<ID>): ...` if non-feature.
- Include only relevant files.

## 8) Flip task to passing
Set `passes=true` for the task in `project-tasks.json` and include that in the same commit.
