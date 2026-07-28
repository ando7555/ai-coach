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
- Google Identity Services for account sign-in
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

Google sign-in requires a web OAuth client ID. Email/password registration is also supported; new email accounts must confirm a token link before sign-in.

```bash
export GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
export VITE_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
export PITCHMIND_ADMIN_EMAILS=your-admin@gmail.com
export PITCHMIND_CONFIRMATION_BASE_URL=http://localhost:8080/
```

Only emails listed in `PITCHMIND_ADMIN_EMAILS` receive the `ADMIN` role. Every other Google account is created as `COACH`.
If SMTP is not configured, confirmation links are written to backend logs only for local/dev/test runs or localhost confirmation URLs. Public deployments reject email registration until SMTP is configured; users should use Google sign-in until mail delivery is ready.

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

## Demo Data

With Neo4j and the backend running, seed a reusable demo dataset:

```bash
cd frontend-react
npm run demo:seed
```

The walkthrough is in `frontend-react/scripts/demo-walkthrough.md`.

## Public Deployment

The simplest full public deployment is:

1. Create a Neo4j AuraDB database and copy its Bolt URI, username, and password.
2. Deploy this repository as a Docker web service using `render.yaml`.
3. Set these environment variables on the web service:

```text
NEO4J_URI=neo4j+s://your-aura-host.databases.neo4j.io
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=your-aura-password
JWT_SECRET=generated-long-random-secret
GOOGLE_GEMINI_API_KEY=disabled
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
VITE_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
PITCHMIND_ADMIN_EMAILS=your-admin@gmail.com
PITCHMIND_CONFIRMATION_BASE_URL=https://your-public-app-url/
SPRING_PROFILES_ACTIVE=prod
```

The Docker image builds the React frontend and serves it from Spring Boot, so the public backend URL is also the public app URL.

For real email delivery, configure Spring Mail on the deployed service, for example:

```text
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-smtp-user
SPRING_MAIL_PASSWORD=your-smtp-password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

If deploying the frontend separately on Vercel, set `VITE_GRAPHQL_ENDPOINT` to the public backend GraphQL URL, for example:

```text
VITE_GRAPHQL_ENDPOINT=https://pitchmind.onrender.com/graphql
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
PITCHMIND_AUTH_TOKEN=your-admin-jwt npm run smoke:e2e
```

The demo seed and smoke scripts no longer create password users. Provide either `PITCHMIND_AUTH_TOKEN` or `PITCHMIND_GOOGLE_ID_TOKEN` when running authenticated demo flows.
