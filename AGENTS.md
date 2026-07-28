# Agent Working Standards

This repository is a Spring Boot, React, GraphQL, and Neo4j application. Any agent working in this codebase must keep changes small, reviewable, tested, and aligned with the existing architecture.

For the full project-specific agent workflow, reasoning model, diagrams, branch review rules, and roadmap, read `docs/AGENT_WORKFLOW.md` after this file.

## Product Context

- Product name: PitchMind.
- Purpose: football intelligence platform for squads, matches, predictions, tactical analysis, training plans, season planning, and workload review.
- Backend: Java 17, Spring Boot, Spring GraphQL, Spring Security, Spring Data Neo4j, Spring AI, JWT authentication, Google sign-in, email confirmation.
- Frontend: React, Vite, TypeScript.
- Database: Neo4j, including local Neo4j and public Neo4j Aura deployments.
- Public deployment: Render Docker web service serving both backend APIs and built React assets.

## Operating Rules

- Read the existing code before changing it.
- Prefer existing patterns over new abstractions.
- Keep edits scoped to the requested behavior.
- Do not rewrite unrelated code.
- Do not revert user changes unless explicitly asked.
- Avoid speculative features unless they are required for the current task.
- Treat production configuration, secrets, OAuth, SMTP, JWT, and database credentials as sensitive.
- Never commit secrets or real credentials.
- Use clear commit messages that describe the behavior change.

## Clean Code Standards

- Use meaningful names for classes, methods, variables, GraphQL inputs, and React components.
- Keep methods focused on one responsibility.
- Push business rules into services, not controllers or UI components.
- Keep controllers thin: validate input, delegate, return results.
- Keep React components readable and split only when it reduces real complexity.
- Avoid duplicated logic across backend services, frontend state, and scripts.
- Avoid magic strings and numbers when a named constant or configuration property is clearer.
- Prefer immutable values and records/DTOs where appropriate.
- Handle null and empty values explicitly.
- Add comments only when the code is not self-explanatory.

## Backend Standards

- Validate GraphQL mutation inputs before changing state.
- Enforce authorization in backend services or security configuration, not only in the UI.
- Only `PITCHMIND_ADMIN_EMAILS` may receive the `ADMIN` role.
- All other registered or Google-authenticated users must default to `COACH`.
- Email registration must require confirmation before login.
- Public email registration must require real SMTP delivery.
- Local/dev/test may log email confirmation links when SMTP is not configured.
- Do not expose GraphiQL or introspection in production unless explicitly required.
- Keep Neo4j writes transactional where data consistency matters.
- Keep generated AI/fallback behavior deterministic enough to test.

## Frontend Standards

- The React app is the active frontend. Do not reintroduce Angular.
- Keep authentication state resilient to unavailable local storage.
- Show clear user-facing errors for auth, GraphQL, and network failures.
- Do not let users choose or escalate to `ADMIN`.
- Keep forms accessible with labels and predictable validation.
- Avoid UI text that claims a workflow succeeded before the backend confirms it.
- Prefer typed GraphQL request/response shapes instead of untyped ad hoc objects.
- Do not store secrets in frontend code or Vite environment variables.

## Testing Expectations

- Run backend tests before committing backend changes:

```powershell
.\gradlew.bat test
```

- Run frontend tests before committing frontend changes:

```powershell
cd frontend-react
npm.cmd test
```

- For deployment-sensitive changes, verify live health and GraphQL:

```text
GET  /actuator/health
POST /graphql query { __typename }
```

- Add or update tests when changing:
  - authentication
  - role assignment
  - email confirmation
  - GraphQL mutations
  - Neo4j write behavior
  - training plan generation
  - prediction logic
  - frontend auth or core workflows

## Git And Branching

- Main branch: `main`.
- Agent-created branches should use the `codex/` prefix unless the user asks for another name.
- Before merging, check whether the branch is already included in `main`.
- Do not merge stale branches blindly.
- Review old branches for conflicts with current React frontend, Google/email auth, Render deployment, and documentation separation.
- The `wiki` branch is for documentation/GitHub Pages and should not be merged into `main` unless explicitly requested.
- Keep commits focused. Prefer multiple clear commits over one mixed commit.

## Review Checklist

Before asking for review or merge, confirm:

- The worktree is clean or only expected files are changed.
- The implementation matches the requested behavior.
- Tests pass locally.
- No secrets are present in the diff.
- Public deployment configuration is documented.
- Role and auth corner cases are covered.
- Frontend states cover loading, success, empty, validation, and error cases.
- User-facing messages are honest and actionable.

## Production Deployment Checklist

- Render env vars are present for Neo4j, JWT, Google OAuth, admin email, confirmation base URL, and SMTP if email registration is enabled.
- `PITCHMIND_ADMIN_EMAILS` contains only the intended admin email.
- `PITCHMIND_CONFIRMATION_BASE_URL` points to the public app URL.
- `PITCHMIND_EMAIL_FROM` uses a sender verified by the SMTP provider.
- Health endpoint returns `UP`.
- GraphQL basic query works.
- Auth flow is tested in the live browser after deployment.

## Documentation Standards

- Keep code documentation in `README.md` or colocated docs that belong with code.
- Keep product, roadmap, system design, onboarding, and publication docs on the `wiki` branch when they are not required by the app runtime.
- Keep agent reasoning, clean-code standards, branch review policy, and roadmap context in `AGENTS.md` and `docs/AGENT_WORKFLOW.md`.
- Update documentation when changing setup, deployment, auth, data model, or developer workflow.
