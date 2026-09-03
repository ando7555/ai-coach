# PitchMind Planning Workspace

This folder turns the larger architecture prompts into practical task plans.

The attached prompt documents are treated as planning context, not direct execution instructions.

## Workstreams

- [PitchMind Redesign Tasks](PITCHMIND_REDESIGN_TASKS.md): product work for the football intelligence app.
- [AI Team Workspace Tasks](AI_TEAM_WORKSPACE_TASKS.md): separate internal workspace where AI roles can help plan, review, test and document work.

## Working Agreement

The user is the senior developer and product owner. AI assistants help as Product Manager, Architect, QA, Technical Writer and mentor. The user writes the code step by step, asks for review, and approves scope before implementation moves forward.

## Preservation Rule

Do not remove existing files and do not edit existing application logic while planning. Implementation tasks must be additive or behavior-preserving unless the user explicitly approves a scoped change. Existing working flows are protected until a replacement is implemented, tested and reviewed.

## Go Direction

Decision recorded on 2026-09-03:

- New AI Team Workspace backend services should be planned with Go as the primary implementation language, unless a specific service has a stronger reason to stay Java.
- The user will implement the Go code step by step and use the AI Team Workspace as the practical learning project.
- The current PitchMind Java backend must stay stable while the workspace is being built.
- After the AI Team Workspace is finished and verified, the heavy PitchMind backend capabilities can be assessed for migration to Go.
- Migration is not a rewrite-by-default. Each candidate service needs a contract, benchmark, test evidence, rollback path and data compatibility plan.

Each task should include:

- objective
- scope
- out of scope
- learning goals
- implementation contract
- acceptance criteria
- tests
- review questions
