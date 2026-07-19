# PitchMind Delivery Roadmap

## Goal

Deliver a usable coach workflow before expanding into real-time automation, computer vision, or broad scouting functionality.

## Phase 1 — Product foundation

- Adopt PitchMind working brand and action-first positioning
- Define coach, analyst, and club-admin personas
- Standardize recommendation output: observation, evidence, action, impact, risk, confidence
- Add clear separation between facts, calculated metrics, and AI interpretation
- Establish product analytics for generated and reviewed recommendations

## Phase 2 — Match preparation MVP

- Opponent profile generated from recent matches
- Squad availability and workload snapshot
- Tactical plan with formation, pressing, build-up, and defensive recommendations
- Key matchup analysis
- Scenario plans for leading, drawing, and trailing
- Exportable pre-match briefing

### Definition of done

A coach can create a fixture and receive a concise, evidence-backed match plan without manually assembling multiple reports.

## Phase 3 — Post-match learning loop

- Record match events and player statistics
- Identify turning points and tactical problems
- Compare the planned approach with execution
- Capture coach feedback on recommendations
- Generate next-week training priorities

### Definition of done

Every completed fixture produces reusable learning for the next fixture rather than becoming an isolated report.

## Phase 4 — Live decision support

- Game-state snapshots
- Tactical alerts
- Substitution and role-change suggestions
- Formation alternatives with risks and trade-offs
- Recommendation timeline and coach decision audit

### Important constraint

Start with manual or semi-automated event entry. Real-time provider integrations should follow only after the decision workflow has been validated with coaches.

## Phase 5 — Club intelligence

- Multi-team workspace
- Academy player-development tracking
- Rotation and workload planning
- Tactical trend comparison across fixtures
- Shared reports and permissions

## Phase 6 — Advanced intelligence

- Event-data provider integrations
- Video and computer-vision analysis
- Automated tactical pattern detection
- Scouting and recruitment fit
- Bench or voice assistant interfaces

## Suggested engineering backlog

### Domain

- `TacticalObservation`
- `EvidenceItem`
- `CoachAction`
- `ExpectedImpact`
- `RecommendationRisk`
- `ConfidenceScore`
- `CoachDecision`
- `MatchGameState`

### GraphQL

- Query a match intelligence briefing
- Generate tactical options for a game state
- Record coach feedback on a recommendation
- Query the recommendation decision history
- Generate a post-match learning report

### Neo4j

- Connect recommendations to evidence and match states
- Relate player workload to availability and tactical role
- Store coach decisions and outcomes
- Preserve provenance for generated analysis

### Quality

- Prompt contract tests
- GraphQL schema tests
- Recommendation validation
- Low-confidence fallback behavior
- Authorization tests for player and club data

## First release boundary

The first release should not require:

- live broadcast ingestion;
- automated video tracking;
- professional event-data licensing;
- betting odds;
- exact score prediction;
- autonomous coaching decisions.

A focused preparation and post-match workflow is enough to test whether coaches repeatedly use and trust the product.
