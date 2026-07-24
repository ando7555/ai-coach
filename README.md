# PitchMind

PitchMind is a Java/Spring and React football intelligence application. This repository now keeps application code separate from product, roadmap, brand, and publication documentation.

Documentation is published from the dedicated `wiki` branch through GitHub Pages:

- https://ando7555.github.io/ai-coach/
- https://github.com/ando7555/ai-coach/tree/wiki

## Implemented Capabilities

- GraphQL authentication with role-aware mutation access
- Team, player, match, and player-stat management
- Neo4j storage for teams, players, matches, recommendations, analyses, training plans, season plans, and prediction records
- AI-generated or deterministic-fallback tactical analysis, training plans, and season plans
- React intelligence portal for squads, matches, player stats, tactical analysis, training plans, season workload review, prediction workflows, and market evaluation
- Transparent statistical baseline predictor for match probabilities
- Backend market-value evaluation for fair odds, implied probability, expected value, and conservative value classification

## Tech Stack

- Java 17 and Spring Boot 3.5
- Spring GraphQL and GraphiQL
- Neo4j
- Spring AI with the Google Gemini OpenAI-compatible endpoint
- React, Vite, and TypeScript
- Project Reactor and Lombok

## Running Locally

Prerequisites:

- Java 17+
- Node.js and npm
- Neo4j running on `bolt://127.0.0.1:7687`
- Optional `GOOGLE_GEMINI_API_KEY`

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

Open the React portal at:

```text
http://localhost:8080/
```

Open GraphiQL at:

```text
http://localhost:8080/graphiql
```

## Frontend Development

```bash
cd frontend-react
npm install
npm run dev
```

## Testing

```bash
./gradlew test
```

For frontend-only checks:

```bash
cd frontend-react
npm test
npm run build
```
