# PitchMind - AI Football Intelligence and Betting Decision Support

PitchMind transforms football data into explainable match predictions, betting intelligence, and interactive decision support.

The current application is still an early product. It already supports football data management, AI-assisted coaching analysis, workload review, and training-plan generation. Match-prediction and betting intelligence are product-direction work for the next milestones unless explicitly called out as implemented below.

PitchMind must never promise guaranteed winning bets or guaranteed match results. Football contains uncertainty. The product guarantee is methodological transparency: probabilities, confidence, uncertainty, historical evaluation, input provenance, and clear explanations.

## Product Positioning

PitchMind is an interactive AI football intelligence, match-prediction, and betting decision-support platform for:

- football bettors
- professional analysts
- coaches and clubs
- football academies
- data-driven football fans
- future sportsbook or betting-platform operators

In the future, PitchMind may expand into a regulated betting product, subject to licensing and jurisdiction-specific legal requirements. Real-money betting, deposits, wallets, payments, and bet placement are not part of the current milestone.

## Implemented Capabilities

These capabilities are present in the code today:

- GraphQL authentication with role-aware mutation access
- Team, player, match, and player-stat management
- Neo4j storage for teams, players, matches, recommendations, analyses, training plans, and season plans
- AI-generated or deterministic-fallback tactical match analysis
- AI-generated or deterministic-fallback weekly training microcycles
- AI-generated or deterministic-fallback season plans with workload snapshots
- Player workload and performance trend calculations
- Angular coaching portal for squads, matches, player stats, tactical analysis, training plans, and season workload review
- Transparent statistical baseline predictor using completed pre-match history
- Backend market-value evaluation for fair odds, implied probability, expected value, and conservative value classification
- Immutable prediction history in Neo4j

## Partially Implemented Prediction Functionality

The current code does not contain a trained football prediction model. It now includes a transparent statistical baseline predictor, exposed through GraphQL and the Angular Prediction Lab, so users can inspect probability calculations without claiming machine-learning accuracy.

Current support:

- recorded match and player data that can later feed prediction features
- player form and workload calculations that can later become model features
- baseline 1X2, over/under 2.5, BTTS and most-likely-score probabilities from completed historical matches
- backend decimal-odds formulas:
  - `fairOdds = 1 / modelProbability`
  - `rawImpliedProbability = 1 / bookmakerOdds`
  - `expectedValue = (modelProbability * bookmakerOdds) - 1`

Not implemented yet:

- trained home/draw/away probability model
- out-of-sample prediction evaluation
- model calibration reports
- bookmaker odds ingestion
- betting market watchlists
- real-money betting integrations

## Transparent Statistical Baseline Predictor

The v1 predictor is not a trained AI or machine-learning model. It is a deterministic Poisson baseline that uses only data available before prediction generation.

Inputs:

- target match ID, home team, away team, and match date
- completed matches before the target match date
- goals scored and conceded
- home/away splits when sample size is sufficient
- recent form within a configurable recent-match window

Cutoff rules:

- the target match result is excluded
- matches on or after the target match date are excluded
- incomplete matches with missing goals or dates are excluded
- bookmaker odds are never used as prediction features

Baseline formulas:

- expected goals blend team attack, opponent defence, recent form, and global scoring baseline
- a Poisson score matrix is calculated up to configurable `pitchmind.predictor.max-goals`
- truncated score-matrix probabilities are normalized
- home/draw/away probabilities are validated to sum to 1 within `pitchmind.predictor.normalization-tolerance`
- over/under 2.5, BTTS, and most likely score are derived from the same score matrix

Minimum sample requirements are controlled by:

- `pitchmind.predictor.min-team-matches`
- `pitchmind.predictor.min-global-matches`
- `pitchmind.predictor.min-venue-matches`

If mandatory history is missing, PitchMind returns `INSUFFICIENT` data quality and does not emit false probability precision. Future ML models should implement the same `MatchPredictor` interface and preserve the same audit and market-evaluation separation.

## Planned Capabilities

Planned prediction outputs:

- home win, draw, and away win probabilities
- double chance
- draw no bet
- over/under goals
- both teams to score
- likely score ranges
- team and player-related markets when sufficient data exists
- confidence and uncertainty levels
- fair odds calculated from model probability
- bookmaker-implied probability
- expected value
- value-bet indication
- risk rating
- prediction explanation
- factors supporting and opposing the prediction

Planned interactive product areas:

- interactive match centre with competition/date filters, search, sortable matches, confidence indicators, and expandable match cards
- match intelligence dashboard with probability visuals, team-form comparison, head-to-head context, expected goals, injuries, availability, home/away performance, explanations, and missing-data warnings
- betting intelligence workspace for odds entry/import, implied probability comparison, market/risk/confidence filtering, and watchlists
- model-performance dashboard with prediction history, accuracy by competition and market, Brier score, log loss, calibration, ROI simulation, drawdown, and true out-of-sample separation
- scenario analysis for lineup, availability, fatigue, form, and probability-change comparison while preserving the original audited prediction

## Accuracy Policy

PitchMind must not use labels such as "sure win", "guaranteed bet", "risk-free", or "guaranteed result".

Allowed labels include:

- potential value
- weak value
- no value detected
- insufficient data
- high uncertainty

Every production prediction should record:

- prediction timestamp
- model version
- feature version
- available input data
- missing or low-quality inputs
- probabilities
- confidence and uncertainty
- explanation
- immutable prediction record
- post-match outcome and evaluation metrics after completion

## Architecture Direction

The architecture should keep football prediction separate from betting-market evaluation.

- The predictor produces probabilities, not betting instructions.
- A market-value service compares model probabilities with bookmaker odds.
- Bookmaker odds should not be sent into the independent prediction model unless a separate market-aware model is explicitly built.
- Bookmaker odds must not contaminate out-of-sample model evaluation.
- Provider-specific data and odds integrations should sit behind interfaces.
- Future bet placement must be isolated behind a separate regulated integration module.

Suggested bounded contexts:

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

## Responsible Product Requirements

Before any real-money functionality, PitchMind must treat these as mandatory prerequisites:

- uncertainty disclaimer
- responsible-gambling controls
- configurable deposit, stake, and loss limits
- age verification
- licensing and jurisdiction checks
- privacy controls
- auditability
- clear separation between prediction, market evaluation, and user decision

## Tech Stack

- Java 17 and Spring Boot 3.5
- GraphQL with Spring GraphQL and GraphiQL
- Neo4j for teams, players, matches, stats, plans, and recommendation entities
- Spring AI with the Google Gemini OpenAI-compatible endpoint
- Angular coaching portal
- Project Reactor and Lombok

## Running Locally

### Prerequisites

- Java 17+
- Node.js and npm for Angular builds
- Neo4j running on `bolt://127.0.0.1:7687`
- Optional Google Gemini API key

The app can run without a Gemini key. If `GOOGLE_GEMINI_API_KEY` is missing or set to `disabled`, AI endpoints use deterministic fallback outputs.

```bash
export GOOGLE_GEMINI_API_KEY=your-key-here
./gradlew bootRun
```

On Windows PowerShell:

```powershell
$env:GOOGLE_GEMINI_API_KEY="your-key-here"
.\gradlew.bat bootRun
```

Open the Angular portal at:

```text
http://localhost:8080/
```

Open GraphiQL at:

```text
http://localhost:8080/graphiql
```

## Example: Generate Match Analysis

```graphql
mutation {
  generateMatchAnalysis(input: {
    matchId: "1"
    focusArea: PRESSING
    style: POSSESSION
    riskLevel: MEDIUM
  }) {
    summary
    keyFactors
  }
}
```

## Product Documentation

- [Product vision](docs/PRODUCT_VISION.md)
- [Delivery roadmap](docs/ROADMAP.md)

## Brand Note

PitchMind is the working product name. Trademark, domain, licensing, gambling-regulation, and market-conflict checks must be completed before any public or regulated launch.
