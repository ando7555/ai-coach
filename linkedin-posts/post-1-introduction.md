TITLE: I'm Building an AI-Powered Football Coaching App — Here's How It Works

---

What if a football coach could have an AI assistant that analyzes matches, designs training sessions, plans entire seasons, and flags players at risk of injury — all from one system?

That's what I'm building. It's called AI Coach.

[INSERT DIAGRAM: Features — "What the AI Coach Can Do"]

---

THE IDEA

Football coaching involves hundreds of decisions: Who starts? What formation? How hard do we train on Wednesday if we play Saturday? When do we rotate the squad?

Most of these decisions rely on gut feeling and experience. AI Coach adds data and intelligence on top of that instinct.

The system has four core capabilities:

Match Day Intelligence — Before a match, the coach describes the opponent and selects a focus (pressing, build-up, or defence) and a style (possession, direct, or balanced). The AI generates tactical recommendations: defensive shape, attacking patterns, pressing triggers, and phase-by-phase adjustments.

Training Design — The AI creates weekly micro-cycles: day-by-day training sessions with intensity levels (low, medium, high), focus areas, duration, and coaching notes. It balances load so players aren't overtrained before match day.

Season Planning — Given the squad and schedule, the AI designs periodisation phases, rotation strategies, and long-term objectives. It considers player workload across the entire season.

Player Monitoring — This is where it gets interesting.

---

HOW THE APP TRACKS PLAYER FORM

After every match, the system records each player's stats: goals, assists, minutes played, cards, and a performance rating.

To detect whether a player is improving, declining, or stable, the app uses a composite scoring algorithm:

[INSERT DIAGRAM: Form Detection Algorithm]

The algorithm takes a player's recent match stats, splits them in half (older games vs newer games), and calculates a composite score for each half:

Composite Score = (goals × 3) + (assists × 2) + (rating ÷ 10)

Goals are weighted heaviest because they're the hardest to produce. Assists are next. Rating provides a baseline even in games without goal contributions.

If the newer half scores 15% or more above the older half, the player is marked as IMPROVING. If 15% or more below — DECLINING. Otherwise — STABLE.

This gives coaches a clear, data-driven signal about who's trending up and who might need extra support or rotation.

---

HOW THE APP MANAGES FATIGUE

Overplaying fatigued players is the leading cause of soft-tissue injuries. The app calculates fatigue based on minutes played over the last 28 days:

[INSERT DIAGRAM: Fatigue Levels bar]

Combined with match density (how many games per week), this feeds into an injury risk score: LOW, MEDIUM, or HIGH. The coach sees at a glance who needs rest before it becomes an injury.

---

THE ARCHITECTURE

[INSERT DIAGRAM: System Architecture]

The system is built as a layered Spring Boot application.

At the top, the client sends GraphQL queries. Right now it's GraphiQL (a built-in playground). A full frontend is coming.

The Spring Boot layer handles three concerns: security (JWT authentication with COACH and ADMIN roles), the GraphQL API (a single endpoint that handles all queries and mutations), and four core services that contain the business logic.

The AI layer uses Spring AI's ChatClient to talk to Google Gemini. What makes this powerful is that the app has three specialized AI personas — each with a different system prompt that shapes its expertise. The tactical persona thinks like a UEFA Pro Licensed coach. The training persona designs periodised micro-cycles. The season planning persona thinks about long-term squad management.

Because Spring AI uses an OpenAI-compatible interface, the entire AI layer can switch from Gemini to OpenAI, Azure, or a local model by changing one URL in the config. Zero code changes.

At the bottom, Neo4j stores the data as a graph.

---

WHY A GRAPH DATABASE?

[INSERT DIAGRAM: Neo4j Graph Data Model]

Coaching data is fundamentally about relationships. Teams have players. Players have performance records. Coaches manage teams.

In a traditional relational database (like PostgreSQL), getting a team's players with their recent performances means joining three tables. The database scans indexes and matches foreign keys. As data grows, these joins slow down.

In Neo4j, relationships are stored as direct pointers between nodes. Traversing from Team to Player to Performance is like following links — constant time per hop, regardless of how much data exists.

This matters now for performance queries. It will matter even more as the app grows to include player transfers between teams, tactical matchups, and player chemistry analysis — queries that are painful with table joins but natural with graph traversal.

---

WHY GRAPHQL INSTEAD OF REST?

[INSERT DIAGRAM: REST vs GraphQL comparison]

A coaching dashboard needs nested data all at once: team info, the full player list, each player's recent form, their workload status.

With REST, that's a separate API call for each piece of data. Four round trips at minimum — multiplied by the number of players.

With GraphQL, you describe the exact shape of data you need in a single request. One query. One response. You get exactly the fields you asked for — nothing more, nothing less.

For a data-rich coaching application, this isn't just convenient. It's the difference between a snappy dashboard and one that makes the coach wait.

---

THE TECH STACK

[INSERT DIAGRAM: Tech Stack grid]

Every technology choice serves a purpose:

Java 17 — battle-tested language with modern features, long-term support, and the richest ecosystem for enterprise applications.

Spring Boot 3.5 — handles security, dependency injection, database integration, and AI client configuration with minimal boilerplate.

GraphQL — one flexible API endpoint instead of dozens of REST routes. The client decides what data it needs.

Neo4j 5 — graph database that stores relationships as first-class citizens. Built for the kind of connected data coaching produces.

Google Gemini — powerful AI model accessible through a free tier. Connected via Spring AI's OpenAI-compatible interface.

Docker — multi-stage build keeps the production image lean. Docker Compose runs the app and Neo4j together.

GitHub Actions — automated CI/CD pipeline that builds, tests, generates coverage reports, and runs code quality analysis on every push.

---

WHAT'S COMING NEXT

[INSERT DIAGRAM: Series Roadmap]

This project isn't just about shipping features. I'm using it as a vehicle to deeply learn software engineering — and share that learning publicly.

Over the next weeks, I'll publish a post for each topic in the series. Every post will tie the theory back to real code in this app. No abstract examples. Everything grounded in a working system.

Follow along if you want to learn with me.

The code is open source: https://github.com/ando7555/ai-coach
