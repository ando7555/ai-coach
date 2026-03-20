# TEASER POST (copy-paste into LinkedIn post composer — attach diagram screenshots as carousel images)

Every request to my AI Coach app travels through 11 layers before it becomes a JSON response. Here's exactly what happens — from raw TCP to GraphQL result.

-> HTTP POST hits port 8080 — Tomcat accepts the connection
-> JwtAuthenticationFilter extracts and validates the Bearer token
-> SecurityFilterChain checks URL rules — /graphql is permitAll()
-> DispatcherServlet routes to the GraphQL handler
-> GraphQL engine parses the query against a typed schema
-> @PreAuthorize("isAuthenticated()") blocks anonymous users at the resolver level
-> @Valid triggers bean validation on the input record
-> CoachService fetches data from Neo4j, builds a prompt, calls AI
-> AiClient wraps the blocking call in a reactive Mono on boundedElastic
-> GraphQLExceptionHandler maps domain exceptions to GraphQL ErrorType
-> Response: always HTTP 200, data or errors in JSON body

REST would need 16+ endpoints for this app. GraphQL needs one.

Full breakdown with request lifecycle diagrams, REST vs GraphQL comparison, and production hardening:

The code is open source: github.com/ando7555/ai-coach

#GraphQL #SpringBoot #REST #WebDevelopment #HTTP #SoftwareEngineering #LearningInPublic

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** HTTP, REST vs GraphQL, and the Spring Request Lifecycle — Traced Through a Real App

---

I'm building an AI-powered football coaching app. Every feature — tactical analysis, training plans, season planning — is exposed through a single GraphQL endpoint. No REST routes. No URL versioning. Just one POST to `/graphql`.

This article traces how the web works, why we chose GraphQL over REST, and what happens to every HTTP request as it flows through 11 layers of the Spring MVC stack.

---

## 1. How the Web Works — HTTP Fundamentals

When you send a request to `http://localhost:8080/graphql`, here's what actually happens at the network level:

1. **DNS resolution** — translates the hostname to an IP address (localhost = 127.0.0.1)
2. **TCP handshake** — three-way handshake (SYN, SYN-ACK, ACK) establishes a reliable connection
3. **TLS handshake** — if HTTPS, negotiates encryption (not needed for localhost dev)
4. **HTTP request** — the actual POST with headers and body travels over the established connection

### HTTP Evolution

**HTTP/1.1 (1997)** — text-based protocol, one request per connection (unless keep-alive), the foundation everything still builds on. AI Coach runs on HTTP/1.1 in development.

**HTTP/2 (2015)** — binary framing layer, multiplexing (multiple requests over one connection), header compression (HPACK), server push. Same semantics as HTTP/1.1, much better performance.

**HTTP/3 (2022)** — runs on QUIC over UDP instead of TCP. Eliminates head-of-line blocking entirely. Supports 0-RTT connection establishment for repeat visits. The future of web transport.

### HTTP Methods

GraphQL only uses two HTTP methods:
- **POST** — all queries and mutations (the request body contains the GraphQL query)
- **GET** — optionally for queries (query string in URL, rarely used)

REST uses all of them: GET, POST, PUT, DELETE, PATCH — each mapped to a CRUD operation. GraphQL collapses this into POST + query language.

### Status Codes — GraphQL's Unusual Approach

REST APIs use HTTP status codes extensively:
- **200** OK — success
- **400** Bad Request — invalid input
- **401** Unauthorized — missing/invalid token
- **404** Not Found — resource doesn't exist
- **500** Internal Server Error — server-side failure

GraphQL **always returns HTTP 200**, even when there's an error. Why? Because the HTTP transport succeeded — the GraphQL layer handles errors in the response body:

```json
{
  "data": null,
  "errors": [{
    "message": "Match not found with id: 999",
    "extensions": { "classification": "NOT_FOUND", "entityType": "Match" }
  }]
}
```

This is a fundamental design decision: HTTP is the transport layer, GraphQL is the application layer. They handle errors independently.

[INSERT DIAGRAM 1: "HTTP Evolution Timeline" — diagrams-post-5.html]

---

## 2. REST vs GraphQL — Why We Chose GraphQL

### What REST Would Look Like

If AI Coach used REST, we'd need separate endpoints for every operation:

```
GET    /api/teams                     → list all teams
GET    /api/teams/{id}                → get one team
GET    /api/teams/{id}/players        → players of a team
POST   /api/teams                     → create team
POST   /api/teams/{id}/players        → add player

GET    /api/matches/{id}              → get one match
GET    /api/teams/{id}/matches        → matches by team
POST   /api/matches                   → record match

POST   /api/matches/{id}/analysis     → generate analysis
GET    /api/matches/{id}/analysis     → get analysis

POST   /api/teams/{id}/training-plan  → generate training plan
GET    /api/teams/{id}/training-plans → list training plans

POST   /api/teams/{id}/season-plan   → generate season plan
POST   /api/matches/{id}/stats       → record player stats
GET    /api/players/{id}/stats       → player match stats
GET    /api/players/{id}/trend       → performance trend
```

That's **16+ endpoints**. And the problems compound:

**Over-fetching:** GET `/api/teams/1` returns everything — name, league, formation, all nested data — even if you only need the name.

**Under-fetching:** To show a team's dashboard, you need: GET `/api/teams/1`, then GET `/api/teams/1/players`, then GET `/api/teams/1/matches`, then GET `/api/matches/{id}/analysis` for each match. Four round trips minimum.

**N+1 endpoint problem:** For every new relationship (players → stats, matches → analysis), you add more endpoints, more controllers, more routes.

### What GraphQL Looks Like

One endpoint. One request. Exact fields:

```graphql
mutation {
  generateMatchAnalysis(input: {
    matchId: 1
    focusArea: PRESSING
    style: POSSESSION
    riskLevel: MEDIUM
  }) {
    id
    summary
    keyFactors
    match {
      homeTeam { name formation }
      awayTeam { name }
    }
  }
}
```

The client specifies exactly what it needs. No over-fetching. No under-fetching. One round trip.

### AI Coach Schema at a Glance

```
Schema: schema.graphqls
  Types: Team, Player, Match, MatchAnalysis, TrainingPlan, SeasonPlan,
         TrainingSession, PlayerWorkloadSnapshot, PlayerMatchStat,
         PlayerPerformanceTrend, User, AuthPayload, Recommendation
  Enums: FocusArea, PlayingStyle, RiskLevel, FormIndicator
  Queries: 13 (teams, players, matches, analysis, plans, stats, trends)
  Mutations: 9 (CRUD + AI generation + auth)
  Input types: MatchInput, MatchAnalysisInput, TrainingPlanInput,
               SeasonPlanInput, PlayerMatchStatInput, RecommendationContextInput,
               TacticalContextInput
```

### When to Use Which

**REST is better when:**
- Simple CRUD with flat resources
- HTTP caching matters (GET requests are cacheable by default)
- Public APIs where simplicity = adoption
- File uploads (GraphQL file upload is awkward)

**GraphQL is better when:**
- Complex, deeply nested relationships (team → players → matches → stats → trends)
- Multiple frontend consumers need different fields
- Graph-shaped data (Neo4j is literally a graph database)
- Reducing network round trips matters (mobile clients)

AI Coach is a textbook GraphQL fit: graph database + deeply nested relationships + AI-generated content that varies per request.

[INSERT DIAGRAM 2: "REST vs GraphQL" — diagrams-post-5.html]

---

## 3. The Spring MVC Request Lifecycle — Traced Through AI Coach

Let's trace a real request — the `generateMatchAnalysis` mutation — through every layer of the stack:

```
HTTP POST /graphql
  Content-Type: application/json
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Body: { "query": "mutation { generateMatchAnalysis(input: {...}) { id summary } }" }
```

### Step 1: Tomcat Receives the Request (Port 8080)

Spring Boot's embedded Tomcat servlet container accepts the TCP connection. Configured in `application.yaml`:

```yaml
server:
  port: 8080
```

The `spring-boot-starter-web` dependency brings Tomcat automatically. No WAR file, no external server, no XML configuration.

### Step 2: JwtAuthenticationFilter (Security Filter)

```java
// JwtAuthenticationFilter.java:24-42
protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) {
    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        if (tokenProvider.isValid(token)) {
            String username = tokenProvider.getUsername(token);
            String role = tokenProvider.getRole(token);
            var authority = new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "COACH"));
            var auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    filterChain.doFilter(request, response);
}
```

**Key insight:** This filter runs for EVERY request, but it doesn't block anything. It just extracts the JWT if present and populates the `SecurityContext`. If there's no token, the request continues unauthenticated — the blocking happens later.

### Step 3: SecurityFilterChain Authorization Rules

```java
// SecurityConfig.java:33-38
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/graphql").permitAll()
    .requestMatchers("/graphiql/**").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .anyRequest().authenticated()
)
```

`/graphql` is `permitAll()` — anyone can reach the endpoint. Why? Because authentication happens at the **resolver level** with `@PreAuthorize`, not at the URL level. This lets unauthenticated operations (like `login` and `register`) share the same `/graphql` endpoint as authenticated ones.

### Step 4: DispatcherServlet → GraphQL Handler

Spring MVC's `DispatcherServlet` receives the request and routes it to the GraphQL handler. The handler:
1. Reads the JSON body: `{ "query": "mutation { generateMatchAnalysis(...) { ... } }", "variables": {...} }`
2. Passes it to the GraphQL engine

### Step 5: GraphQL Engine Parses the Query

The `graphql-java` engine validates the query against `schema.graphqls`:
- Is `generateMatchAnalysis` a valid mutation? Yes (line 221)
- Does the input match `MatchAnalysisInput`? Validates types and non-null constraints
- Are the requested return fields (`id`, `summary`) valid on `MatchAnalysis`? Yes

### Step 6: @PreAuthorize Check

```java
// CoachGraphqlController.java:48
@MutationMapping
@PreAuthorize("isAuthenticated()")
public MatchAnalysis generateMatchAnalysis(@Argument @Valid MatchAnalysisInput input) {
    return coachService.generateMatchAnalysis(input);
}
```

Spring Security's method-level security intercepts the call. If the `SecurityContext` has no authenticated user (no valid JWT was sent in step 2), this throws `AccessDeniedException` — caught by the exception handler in step 10.

### Step 7: @Valid Bean Validation

The `@Valid` annotation triggers Jakarta Bean Validation on the `MatchAnalysisInput` record:

```java
public record MatchAnalysisInput(
    @NotNull Long matchId,
    @NotBlank String focusArea,
    @NotBlank String style,
    @NotBlank String riskLevel
) {}
```

If any constraint fails, a `ConstraintViolationException` is thrown — also caught by the exception handler.

### Step 8: CoachService Business Logic

```java
// CoachService.java:42-65
Match match = matchRepository.findById(input.matchId())
        .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

String prompt = buildMatchAnalysisPrompt(match, input);

String aiResponse = aiClient.generateTacticalAdvice(prompt)
        .blockOptional()
        .orElse("No analysis generated.");
```

1. Fetch the match from Neo4j (or throw `EntityNotFoundException`)
2. Build a text block prompt with match data
3. Call the AI through the reactive `AiClient`
4. Save the result to Neo4j

### Step 9: AiClient Reactive Pattern

```java
// AiClient.java:36-41
public Mono<String> generateTacticalAdvice(String prompt) {
    return Mono.fromCallable(() -> tacticalClient.prompt().user(prompt).call().content())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Tactical advice generation failed", e))
            .onErrorResume(e -> Mono.empty());
}
```

`Mono.fromCallable()` wraps the blocking AI SDK call. `Schedulers.boundedElastic()` runs it on a thread pool designed for blocking I/O — keeping Tomcat's request threads free.

### Step 10: GraphQLExceptionHandler (If Error)

```java
// GraphQLExceptionHandler.java:21-51
if (ex instanceof EntityNotFoundException e) {
    return buildError(e.getMessage(), ErrorType.NOT_FOUND, env,
            Map.of("entityType", e.getEntityType(), "entityId", String.valueOf(e.getEntityId())));
}

if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
    return buildError("Authentication required", ErrorType.UNAUTHORIZED, env, Map.of());
}
```

The handler maps domain exceptions to GraphQL `ErrorType` values with rich extensions. The response is **still HTTP 200** — the error lives in the JSON body.

### Step 11: HTTP 200 Response

```json
{
  "data": {
    "generateMatchAnalysis": {
      "id": "42",
      "summary": "Pressing high against their build-up weakness...",
      "keyFactors": ["High press effectiveness", "Wing overloads"],
      "match": {
        "homeTeam": { "name": "Arsenal", "formation": "4-3-3" },
        "awayTeam": { "name": "Chelsea" }
      }
    }
  }
}
```

Always HTTP 200. Always `Content-Type: application/json`. The GraphQL response contains either `data`, `errors`, or both.

[INSERT DIAGRAM 3: "Spring MVC Request Lifecycle" — diagrams-post-5.html]

---

## 4. Security in Web Applications

### JWT Structure

Every authenticated request carries a JWT in the `Authorization` header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjb2FjaDEiLCJyb2xlIjoiQ09BQ0giLCJpYXQiOjE3MTEwMDAwMDAsImV4cCI6MTcxMTA4NjQwMH0.signature
```

A JWT has three parts, base64-encoded and dot-separated:
- **Header:** `{"alg":"HS256"}` — the signing algorithm
- **Payload:** `{"sub":"coach1","role":"COACH","iat":...,"exp":...}` — claims about the user
- **Signature:** HMAC-SHA256 of header + payload using the server's secret key

```java
// JwtTokenProvider.java:26-34
public String generateToken(String username, String role) {
    Date now = new Date();
    return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key)
            .compact();
}
```

### Why CSRF Is Disabled

```java
// SecurityConfig.java:33
.csrf(csrf -> csrf.disable())
```

CSRF (Cross-Site Request Forgery) protection is for **cookie-based** authentication. The attack: a malicious site submits a form to your API, and the browser automatically attaches cookies. With JWT in the `Authorization` header, the browser never automatically sends credentials — the frontend must explicitly set the header. No automatic credential attachment = no CSRF risk.

**When to enable CSRF:** if you switch to cookie-based sessions (traditional server-rendered apps).

### CORS — Why It Matters

```java
// SecurityConfig.java — new CORS configuration
config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

CORS (Cross-Origin Resource Sharing) controls which domains can call your API from a browser. Without it, a React app at `localhost:3000` cannot call the API at `localhost:8080` — the browser blocks the request before it even leaves.

The browser sends a **preflight OPTIONS request** first: "Can I POST to /graphql with an Authorization header?" The server responds with Access-Control headers saying yes. Only then does the actual POST proceed.

### Method-Level vs URL-Level Security

AI Coach uses both:
- **URL-level:** `permitAll()` on `/graphql` — lets everyone reach the endpoint
- **Method-level:** `@PreAuthorize("isAuthenticated()")` on mutations — blocks anonymous users per operation
- **Role-based:** `@PreAuthorize("hasRole('ADMIN')")` on `createTeam`/`createPlayer` — only admins

This is necessary because GraphQL has one URL for all operations. You can't secure `/graphql` and still allow `login`/`register`.

[INSERT DIAGRAM 4: "JWT Authentication Flow" — diagrams-post-5.html]

---

## 5. GraphQL Deep Dive — Schema, Types, and Operations

### Schema-First Design

AI Coach uses **schema-first** design: you write the schema in `.graphqls` files, and Spring maps it to Java methods. The alternative is **code-first**, where you annotate Java classes and the schema is generated.

Schema-first means the contract is explicit and language-agnostic. Any frontend developer can read `schema.graphqls` without knowing Java.

### How Spring Maps Schema to Code

```graphql
# schema.graphqls:221
extend type Mutation {
  generateMatchAnalysis(input: MatchAnalysisInput!): MatchAnalysis!
}
```

```java
// CoachGraphqlController.java:49
@MutationMapping
public MatchAnalysis generateMatchAnalysis(@Argument @Valid MatchAnalysisInput input) {
```

The mapping is by **method name**. `@MutationMapping` + method name `generateMatchAnalysis` = schema field `generateMatchAnalysis`. The `@Argument` annotation maps the GraphQL argument to the Java parameter.

### Nullability

In the GraphQL schema, `!` means non-null:
- `String!` — must be present, cannot be null
- `[Team!]!` — non-null list of non-null teams (can be empty, but not null)
- `String` (no `!`) — nullable, might be absent

This maps directly to Java: `@NotNull` for required fields, `Optional` for nullable return values.

### Schema Extension

AI Coach uses `extend type Query` and `extend type Mutation` to add operations without modifying the original block:

```graphql
# Lines 48-58: Original queries
type Query {
  teams: [Team!]!
  team(id: ID!): Team
  match(id: ID!): Match
  ...
}

# Lines 196-204: Extended queries (added later)
extend type Query {
  matchAnalysis(matchId: ID!): [MatchAnalysis!]!
  trainingPlansByTeam(teamId: ID!): [TrainingPlan!]!
  playerTrendByLastMatches(playerId: ID!, lastN: Int!): PlayerPerformanceTrend!
  ...
}
```

This is useful for organizing a growing schema — each feature set extends the base types.

[INSERT DIAGRAM 5: "GraphQL Error vs REST Error" — diagrams-post-5.html]

---

## 6. Error Handling — GraphQL vs REST

### REST Errors

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{ "error": "Match not found with id: 999" }
```

The error is in the **HTTP status line**. Clients check `response.status` first.

### GraphQL Errors

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": null,
  "errors": [{
    "message": "Match not found with id: 999",
    "path": ["generateMatchAnalysis"],
    "locations": [{"line": 2, "column": 3}],
    "extensions": {
      "classification": "NOT_FOUND",
      "entityType": "Match",
      "entityId": "999"
    }
  }]
}
```

The error is in the **response body**. HTTP status is always 200. Clients check `response.data.errors` array.

### AI Coach's Error Mapping

```java
// GraphQLExceptionHandler.java — domain exceptions to GraphQL errors
EntityNotFoundException     → ErrorType.NOT_FOUND
ConstraintViolationException → ErrorType.BAD_REQUEST
IllegalArgumentException    → ErrorType.BAD_REQUEST
AccessDeniedException       → ErrorType.UNAUTHORIZED
AiGenerationException       → ErrorType.INTERNAL_ERROR
```

The `extensions` map carries extra metadata — `entityType`, `entityId`, `operation` — so the frontend can give users specific, actionable error messages instead of generic "something went wrong."

---

## 7. Production Readiness — What We Added

Four improvements to harden the app for production:

### 1. CORS Configuration (SecurityConfig.java)

```java
config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);
```

Without CORS, no browser-based frontend can call the API. The `maxAge` caches the preflight response for 1 hour so the browser doesn't send an OPTIONS request before every POST.

### 2. Spring Actuator Health Endpoint (build.gradle + application.yaml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

`/actuator/health` returns the app's health status. Docker and Kubernetes use this for:
- **Liveness probe:** "Is the process alive?" — restart if not
- **Readiness probe:** "Can it handle requests?" — remove from load balancer if not

### 3. Profile-Specific GraphiQL (application-prod.yaml)

```yaml
# application-prod.yaml
spring:
  graphql:
    graphiql:
      enabled: false
    schema:
      introspection:
        enabled: false
```

GraphiQL is an interactive IDE for exploring the API — invaluable in development, dangerous in production. Introspection lets anyone discover your entire schema. Activated with `--spring.profiles.active=prod` or `SPRING_PROFILES_ACTIVE=prod`.

### 4. GraphQL Query Depth Limit (application.yaml)

```yaml
spring:
  graphql:
    execution:
      max-depth: 10
```

Without a depth limit, a malicious client can send: `{ teams { players { team { players { team { players { ... } } } } } } }` — a deeply nested query that consumes exponential server resources. A depth limit of 10 is generous for legitimate queries but blocks abuse.

[INSERT DIAGRAM 6: "AI Coach API Map" — diagrams-post-5.html]

---

## 8. Summary

Four layers, each with a distinct responsibility:

- **HTTP** is the transport layer — POST to `/graphql` on port 8080
- **GraphQL** is the query language — typed schema, single endpoint, client chooses fields
- **Spring MVC** is the framework — DispatcherServlet → filters → resolvers → services
- **Security** is layered — filter chain → URL rules → method-level `@PreAuthorize`

Every request flows through all four layers in sequence. Understanding each layer separately makes the whole stack predictable.

---

## Comprehension Check

**1. Why does GraphQL always return HTTP 200, even when there's an error?**

Because HTTP is the transport layer and GraphQL is the application layer. The HTTP request succeeded — the server received the query, parsed it, and returned a response. The GraphQL error is an application-level concern, communicated in the response body. Mixing HTTP status codes with GraphQL errors would be conflating two different abstraction layers.

**2. Why is CSRF disabled in SecurityConfig, and when would you enable it?**

CSRF attacks exploit automatic credential attachment — browsers automatically send cookies with every request to a domain. AI Coach uses JWT in the Authorization header, which the browser never sends automatically. No automatic credentials = no CSRF risk. Enable CSRF if you switch to cookie-based session authentication.

**3. What's the difference between HTTP/2 multiplexing and HTTP/3 QUIC?**

HTTP/2 multiplexes requests over a single TCP connection, but TCP's head-of-line blocking means one lost packet stalls ALL streams. HTTP/3 uses QUIC over UDP, where each stream is independent — a lost packet in one stream doesn't block others. HTTP/3 also supports 0-RTT connection establishment for repeat connections.

**4. In the request lifecycle, why does the JWT filter run BEFORE the DispatcherServlet?**

Servlet filters execute in a chain before the request reaches any servlet. Security must be established before business logic runs. If the filter ran after the DispatcherServlet, the GraphQL resolver could execute without knowing who the user is — `@PreAuthorize` would have no SecurityContext to check.

**5. Why does AI Coach use `permitAll()` on `/graphql` instead of requiring authentication at the URL level?**

Because GraphQL serves all operations through one URL. Some operations (`login`, `register`, `teams` query) are public; others (`generateMatchAnalysis`, `createTeam`) require authentication. URL-level security can't distinguish between them — it's the same URL. Method-level `@PreAuthorize` on individual resolvers gives fine-grained control per operation.

---

The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#GraphQL #SpringBoot #REST #HTTP #WebDevelopment #SpringSecurity #JWT #SoftwareEngineering #LearningInPublic
