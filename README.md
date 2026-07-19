# PitchMind — AI Football Intelligence Platform

PitchMind is an AI-powered decision-support platform for football coaches, analysts, academies, and clubs.

It is not another score-prediction or betting application. The product helps football staff understand **what is happening, why it is happening, and what action to take next** before, during, and after a match.

## Product promise

> Turn match, player, workload, and opponent data into clear tactical decisions.

PitchMind supports questions such as:

- Where is the opponent creating overloads?
- Which pressing trigger is failing?
- What tactical adjustment should be made after a formation change?
- Which player is showing fatigue or injury-risk signals?
- What substitutions are most likely to improve the game state?
- What should the team train before the next fixture?

## Core product areas

### Match Intelligence

- Pre-match opponent analysis
- Tactical strengths, weaknesses, and key matchups
- AI-generated match plans
- Formation and pressing recommendations
- Scenario analysis for different game states

### Live Decision Support

- Match-state snapshots
- Tactical alerts and detected risks
- Suggested substitutions and role changes
- Alternative formations and expected trade-offs
- Prioritized coach actions rather than generic commentary

### Post-match Analysis

- Tactical review and key turning points
- Player-performance trends
- Comparison between match plan and execution
- Training recommendations based on observed problems

### Player and Workload Intelligence

- Player form and contribution trends
- Fatigue and injury-risk indicators
- Rotation recommendations
- Season workload planning

### Training Intelligence

- Weekly microcycles
- Match-specific training sessions
- Development objectives
- Individual and team recommendations

## Target users

1. Amateur and semi-professional clubs that lack a dedicated analytics department
2. Football academies managing player development and workload
3. Professional analysts who need faster preparation and reporting
4. Coaches who want actionable recommendations instead of raw dashboards

## Tech stack

- **Java 17** and **Spring Boot 3.5**
- **GraphQL** with Spring GraphQL and GraphiQL
- **Neo4j** for teams, players, matches, events, and tactical relationships
- **Spring AI** with Google Gemini
- **Angular** coaching portal
- **Project Reactor** and Lombok

## Current capabilities

- Team, player, and match management
- AI-generated match analysis and tactical recommendations
- Season development plans
- Weekly training microcycles
- Player workload and performance trends
- GraphQL authentication and paginated football data

## MVP direction

The first commercial version focuses on one complete workflow:

1. Import or create teams, players, and a fixture
2. Add recent match and player information
3. Generate an opponent report and match plan
4. Record match events and player statistics
5. Generate a post-match review and next-week training plan

The MVP deliberately avoids fan-facing score prediction. The value is in **football decisions**, not guessing results.

## Running locally

### Prerequisites

- Java 17+
- Neo4j running on `localhost:7687`
- Google Gemini API key

```bash
export GOOGLE_GEMINI_API_KEY=your-key-here
./gradlew bootRun
```

Open GraphiQL at:

```text
http://localhost:8080/graphiql
```

## Example: generate a match analysis

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

## Product documentation

- [Product vision](docs/PRODUCT_VISION.md)
- [Delivery roadmap](docs/ROADMAP.md)

## Brand note

**PitchMind** is the working product name. Trademark, domain, and market-conflict checks should be completed before public launch.
