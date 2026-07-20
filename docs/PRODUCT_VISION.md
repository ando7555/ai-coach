# PitchMind Product Vision

## Vision

Make professional-quality football intelligence, explainable prediction, and responsible betting decision support accessible to analysts, coaches, bettors, clubs, academies, and data-driven football fans.

## Core Positioning

PitchMind transforms football data into explainable match predictions, betting intelligence, and interactive decision support.

PitchMind is not:

- a guaranteed-bet product
- a guaranteed-result predictor
- a black-box tipster
- an automatic bet-placement tool in the current milestone
- a replacement for analyst, coach, or user judgement

The platform should help users understand probability, uncertainty, data quality, and potential market value. It should never claim that a football outcome or betting return is certain.

## Target Users

- football bettors who want transparent reasoning rather than tips
- professional analysts preparing match and market views
- coaches and clubs using team, player, and workload intelligence
- football academies tracking development and availability
- data-driven football fans exploring probabilities and scenarios
- future sportsbook or betting-platform operators, subject to regulation

## Implemented Today

The current application supports:

- team and player management
- match recording
- player match statistics
- player workload and form calculations
- tactical match analysis
- weekly training microcycles
- season planning and workload review
- GraphQL authentication and authorization
- an Angular portal for the above workflows
- a transparent statistical baseline predictor for match probabilities
- backend betting-market value evaluation for decimal odds and expected value
- immutable prediction history for audit

## Partial Prediction Foundation

The product has useful football data foundations and a transparent statistical baseline predictor. It does not yet include a trained machine-learning prediction model.

Partially available foundations:

- historical match records
- home and away teams
- goals scored and conceded
- player participation and performance stats
- player workload snapshots
- AI-generated tactical explanations
- deterministic fallback behavior when AI is disabled
- Poisson-based score matrix for 1X2, BTTS, over/under 2.5, and likely-score output
- deterministic cutoff rules that exclude target and future matches

Missing for production prediction:

- feature-engineering pipeline
- model training pipeline
- feature versioning
- out-of-sample evaluation
- calibration metrics
- market-specific probability outputs
- odds provider integration

## Planned Prediction Output

Production prediction outputs should eventually support:

- home win, draw, and away win probabilities
- double chance
- draw no bet
- over/under goals
- both teams to score
- likely score ranges
- team and player-related markets when sufficient data exists
- confidence level
- uncertainty level
- fair odds calculated from model probability
- bookmaker-implied probability
- expected value
- value-bet indication
- risk rating
- prediction explanation
- factors supporting and opposing the prediction

## Accuracy Policy

PitchMind should guarantee transparency, not winning outcomes.

Every production prediction should include:

- probability
- confidence
- uncertainty
- input-data quality
- missing-input warnings
- model version
- feature version
- prediction timestamp
- immutable prediction record
- historical outcome once available
- evaluation status

Models should be evaluated using historical out-of-sample data. Accuracy, probability calibration, Brier score, log loss, and performance by competition and market should be tracked after completed matches.

## Betting Intelligence Policy

Betting-market evaluation must be separated from football prediction.

The predictor produces probabilities. A betting-intelligence layer compares those probabilities with bookmaker odds and explains whether there may be value.

For decimal odds:

```text
fairOdds = 1 / modelProbability
rawImpliedProbability = 1 / bookmakerOdds
expectedValue = (modelProbability * bookmakerOdds) - 1
```

Allowed labels:

- potential value
- weak value
- no value detected
- insufficient data
- high uncertainty

Disallowed labels:

- sure win
- guaranteed bet
- risk-free
- guaranteed result

## Decision Model

Every recommendation or prediction should distinguish:

- observation: what the system detected
- evidence: the data or relationships supporting it
- probability: the model output, when a prediction model is available
- uncertainty: what may make the output unreliable
- action: the possible user or coach response
- expected impact: the expected tactical, workload, or market implication
- risk: the downside or uncertainty
- confidence: how strongly the available evidence supports it

## Responsible Product Guardrails

PitchMind must:

- display an uncertainty disclaimer
- clearly label incomplete or low-confidence data
- keep facts, calculated metrics, AI interpretation, and market evaluation separate
- avoid gambling-guarantee language
- protect player and user data
- record prediction and recommendation provenance
- require licensing, age verification, jurisdiction checks, privacy controls, and responsible-gambling controls before real-money functionality

## Future Commercial Direction

Initial packages may include:

- analyst workspace for match intelligence and prediction review
- coaching workspace for squad, workload, and training intelligence
- bettor decision-support workspace for odds comparison and watchlists
- club workspace for multi-team development and historical trends
- future regulated sportsbook or betting-platform integrations only after legal, licensing, payments, wallet, and responsible-gambling requirements are satisfied
