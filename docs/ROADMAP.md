# PitchMind Delivery Roadmap

## Goal

Deliver a transparent football intelligence and prediction workflow before expanding into regulated betting integrations, real-time automation, computer vision, or sportsbook operations.

## Current Baseline

The current app already provides the football data and coaching foundation:

- teams, players, matches, and player statistics
- player form and workload calculations
- tactical match analysis
- weekly training plans
- season workload plans
- transparent statistical baseline match prediction
- backend decimal-odds market evaluation
- immutable prediction history
- Angular portal
- GraphQL API
- Neo4j persistence
- deterministic AI fallbacks when Gemini is disabled

The current app does not yet provide a trained prediction model, model evaluation dashboard, bookmaker odds ingestion, persisted market evaluation, watchlists, or bet placement.

## Phase 1 - Product Foundation

- Adopt PitchMind as football intelligence and betting decision-support positioning
- Keep current implemented capabilities clearly separated from planned capabilities
- Update frontend language from generic coaching app toward intelligence and prediction decision support
- Add responsible-product copy and uncertainty disclaimers
- Define target personas: bettor, analyst, coach, academy, club admin, data-driven fan, future regulated operator
- Standardize output language: probability, confidence, uncertainty, evidence, value, risk, and explanation

### Definition of Done

A user can understand what PitchMind does today, what is planned, and why the product does not promise guaranteed betting outcomes.

## Phase 2 - Prediction Data Foundation

- Define prediction entity and immutable audit fields
- Store prediction timestamp, model version, feature version, input-data availability, and missing-data warnings
- Create interfaces before provider-specific football-data implementations
- Separate recorded match data from derived features
- Add feature-engineering services for team form, home/away performance, goal trends, player availability, and workload
- Add tests for feature calculations and missing-data behavior

### Definition of Done

The backend can create a transparent, auditable feature snapshot for a match without using bookmaker odds.

## Phase 3 - Predictor MVP

- Extend baseline beyond the current transparent Poisson model when enough data is available
- Add richer feature snapshots
- Add feature version tracking
- Add out-of-sample evaluation fixture data
- Add basic accuracy and calibration reporting

### Definition of Done

A user can select a match and see model probabilities with confidence, uncertainty, data-quality warnings, and an explanation.

## Phase 4 - Betting Intelligence Workspace

- Create a market-value service separate from the predictor
- Support manual decimal odds entry
- Calculate fair odds from model probability
- Calculate bookmaker-implied probability
- Calculate expected value
- Label outcomes as potential value, weak value, no value detected, insufficient data, or high uncertainty
- Add market filters for home/draw/away first
- Add watchlist storage
- Prevent bookmaker odds from contaminating independent model evaluation

### Definition of Done

A user can compare model probability with bookmaker odds and understand why PitchMind did or did not detect potential value.

## Phase 5 - Model Performance Dashboard

- Prediction history
- Accuracy by competition and market
- Brier score
- Log loss
- Calibration chart
- ROI simulation
- Maximum drawdown
- Training, validation, and true out-of-sample separation
- Model drift and data-quality reporting

### Definition of Done

PitchMind can show whether the prediction process is improving, calibrated, and reliable across markets and competitions.

## Phase 6 - Interactive Match Centre

- Competition and date filters
- Live search
- Sortable matches
- Prediction confidence indicators
- Expandable match cards
- Probability visualisations
- Team-form comparison
- Head-to-head analysis
- Expected goals
- Injuries and availability
- Home/away performance
- Model explanation
- Uncertainty and missing-data warnings

### Definition of Done

A user can scan fixtures, open a match, and understand the data, probabilities, risks, and missing information behind the prediction.

## Phase 7 - Scenario Analysis

- Update expected lineup
- Mark a player unavailable
- Adjust fatigue or team form
- Compare probability changes
- Preserve original predictions for auditing
- Record scenario assumptions separately from observed facts

### Definition of Done

A user can explore "what if" changes without modifying the original audited prediction.

## Phase 8 - Regulated Betting Readiness

This phase is future-only and must not be mixed into the current milestone.

- Licensing and jurisdiction checks
- Age verification
- Privacy and retention controls
- Responsible-gambling controls
- Configurable deposit, stake, and loss limits
- Payment, wallet, and account-risk architecture
- Separate regulated bet-placement integration module
- Legal and compliance review

### Definition of Done

No real-money functionality launches until regulatory, responsible-gambling, payment, privacy, and audit requirements are satisfied.

## Architecture Backlog

### Bounded Contexts

- `football-data`
- `feature-engineering`
- `predictor`
- `model-evaluation`
- `match-intelligence`
- `betting-intelligence`
- `odds-integration`
- `decision-support`
- `user-watchlist`
- `audit`

### GraphQL

- Query match intelligence briefing
- Generate prediction for match
- Query prediction history
- Record prediction outcome
- Query model evaluation metrics
- Evaluate market value from manual odds
- Add and remove watchlist items
- Generate scenario prediction

### Neo4j

- Connect predictions to matches, model versions, feature snapshots, and input data
- Connect market evaluations to immutable predictions
- Store watchlist items separately from predictions
- Preserve provenance for AI-generated explanations
- Store scenario assumptions separately from observed facts

### Quality

- Feature calculation tests
- Prompt contract tests
- GraphQL schema tests
- Prediction validation
- Market-value formula tests
- Missing-data fallback behavior
- Authorization tests for player, club, prediction, and watchlist data
- Responsible-language tests for user-facing copy

## First Release Boundary

The first release should not require:

- live broadcast ingestion
- automated video tracking
- professional event-data licensing
- bookmaker odds provider integration
- real-money deposits
- wallets
- payments
- bet placement
- autonomous betting decisions

A focused football intelligence, transparent prediction, and manual odds-comparison workflow is enough to validate the product before regulated betting expansion.
