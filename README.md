# AI Coach Backend

AI-powered football coaching assistant that generates tactical advice, season plans, and training programs using LLM intelligence.

## Tech Stack

- **Java 17** + **Spring Boot 3.5**
- **GraphQL** (Spring GraphQL + GraphiQL)
- **Neo4j** — graph database for teams, players, matches, and relationships
- **Spring AI** — LLM integration via Google Gemini (free tier)
- **Lombok** + **Project Reactor**

## Features

- Manage teams, players, and match records
- Generate AI-powered match analysis with tactical recommendations
- Create season-long development plans
- Design weekly training microcycles
- All data stored as a graph in Neo4j

## Prerequisites

- Java 17+
- Neo4j (running on `localhost:7687`)
- Google Gemini API key (free from [AI Studio](https://aistudio.google.com/app/apikey))

## Running

```bash
export GOOGLE_GEMINI_API_KEY=your-key-here
./gradlew bootRun
```

Open **http://localhost:8080/graphiql** to interact with the API.

## Example Mutation

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
