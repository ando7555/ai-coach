# TEASER POST (copy-paste into LinkedIn post composer — attach diagram screenshots as carousel images)

I opened the GoF book expecting factory diagrams I'd never use. Then I counted the design patterns in my own Spring Boot app. I found 15. 🔎

Not because I set out to use them — because Spring Boot, Lombok, and modern Java bake them in whether you notice or not. Naming them is what turns "code that works" into "code you understand."

Here are 5 that surprised me most:

-> 🎯 Strategy — AiClient has 3 ChatClients with different system prompts but shares one callClient() method. No explicit Strategy interface — the pattern hides in plain sight.
-> 🔌 Adapter — a dual adapter layer: AiClient translates our domain into Gemini API calls (outgoing), AiResponseParser converts raw JSON back into typed records (incoming).
-> 🧩 Template Method — GraphQLExceptionHandler overrides exactly one method from Spring's abstract class. The framework defines the skeleton; we fill in the blank.
-> 🔒 Sealed Classes — CoachException sealed permits EntityNotFoundException, AiGenerationException. The compiler rejects any other subclass. Modern Java replacing classic Visitor.
-> 🏛️ Facade — SeasonPlanService hides 6 dependencies behind a single generateSeasonPlan() method. The controller never knows about repositories, parsers, or AI clients.

Plus 10 more: Builder, Decorator/Filter Chain, Repository, Factory Method, Records as Value Objects, Singleton, Dependency Injection, DTO, Layered Architecture, and Transactional Script.

Full article in the comments 👇

🔗 The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#DesignPatterns #GoF #SpringBoot #Java #SOLID #SoftwareEngineering #LearningInPublic #CleanCode

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** 15 Design Patterns Hiding in My Spring Boot App — A Pattern Safari Through Real Code

---

I'm building an AI-powered football coaching app called **AI Coach** — Spring Boot 3.5, GraphQL, Neo4j, Google Gemini. After the [previous article](https://www.linkedin.com/pulse/anatomy-architecture-refactoring-15-issues-7-commits-andranik-muradyan-vlq1f) where I refactored the codebase (15 issues, 7 commits, zero behavior changes), I opened the Gang of Four book to study design patterns — and realized they were already in my code.

This article is a **pattern safari**: I walk through the codebase file by file and name every GoF pattern I find. The goal isn't to memorize UML diagrams — it's to see that these patterns are practical tools you already use. Naming them makes you a better engineer because you can:

1. **Communicate precisely** — "That's a Strategy" is faster than explaining the whole structure
2. **Recognize extension points** — knowing the pattern tells you how to add a fourth AI client or a third exception type
3. **Spot misuse** — once you can name the pattern, you can tell when it's being applied wrong

I found **15 patterns** across 4 categories. Five get a deep dive with full code. Five get medium coverage. Five get a one-line summary. Then I trace a single API call (`generateSeasonPlan`) through 11 of them.

[INSERT DIAGRAM 1: "Pattern Classification Map" — diagrams-post-7.html]

---

## 1. The Pattern Catalog

| # | Pattern | Category | Where |
|---|---------|----------|-------|
| 1 | **Strategy** | Behavioral | `AiClient.java` — 3 ChatClients, 1 `callClient()` |
| 2 | **Adapter** | Structural | `AiClient` + `AiResponseParser` — dual adapter |
| 3 | **Builder** | Creational | Lombok `@Builder` + `ChatClient.Builder.clone()` |
| 4 | **Template Method** | Behavioral | `GraphQLExceptionHandler` extends framework class |
| 5 | **Sealed Classes + Enums** | Modern Java | `CoachException sealed permits` + 7 enums |
| 6 | **Facade** | Structural | `SeasonPlanService` — 6 deps, 1 public method |
| 7 | **Decorator / Filter Chain** | Structural | `JwtAuthenticationFilter` extends `OncePerRequestFilter` |
| 8 | **Repository** | Structural | 9 Spring Data Neo4j interfaces |
| 9 | **Factory Method** | Creational | `buildWorkloadSnapshot()`, `buildPrompt()` |
| 10 | **Records as Value Objects** | Modern Java | 8 DTOs as Java records |
| 11 | **Singleton** | Creational | Every `@Component` / `@Service` bean |
| 12 | **Dependency Injection** | Structural | `@RequiredArgsConstructor` + Spring IoC |
| 13 | **DTO** | Structural | Input/output objects crossing layer boundaries |
| 14 | **Layered Architecture** | Architectural | Controller → Service → Repository |
| 15 | **Transactional Script** | Behavioral | `@Transactional` method orchestration |

---

## 2. DEEP DIVE: Strategy — AiClient.java

**The GoF definition:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

**The textbook version** has an explicit `Strategy` interface, concrete implementations, and a `Context` class that delegates. Our version is subtler — and arguably cleaner.

```java
@Component
public class AiClient {

    private final ChatClient tacticalClient;
    private final ChatClient seasonPlanClient;
    private final ChatClient trainingPlanClient;

    public AiClient(ChatClient.Builder builder) {
        this.tacticalClient = builder.clone()
                .defaultSystem("You are an expert football/soccer tactical analyst. "
                        + "Provide concise, actionable tactical advice...")
                .build();

        this.seasonPlanClient = builder.clone()
                .defaultSystem("You are an expert football/soccer season planner...")
                .build();

        this.trainingPlanClient = builder.clone()
                .defaultSystem("You are an expert football/soccer fitness and training coach...")
                .build();
    }
```

Three `ChatClient` instances. Same type. Different behavior — because each has a different system prompt. The **system prompt is the strategy**. It controls how the AI interprets every user prompt it receives.

The context is `callClient()`:

```java
    private Mono<String> callClient(ChatClient client, String prompt, String context) {
        return Mono.fromCallable(() -> client.prompt().user(prompt).call().content())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("{} generation failed", context, e))
                .onErrorResume(e -> Mono.empty());
    }
```

`callClient()` doesn't know which strategy it's executing. It receives a `ChatClient`, sends the prompt, handles errors — same algorithm for all three. The public methods select the strategy:

```java
    public Mono<String> generateTacticalAdvice(String prompt) {
        return callClient(tacticalClient, prompt, "Tactical advice");
    }

    public Mono<String> generateSeasonPlan(String prompt) {
        return callClient(seasonPlanClient, prompt, "Season plan");
    }
```

**Why this matters:** If you need a fourth AI capability — say, opponent scouting — you add one field, one `builder.clone()` call, and one public method. The `callClient()` helper, the error handling, the reactive pipeline — nothing changes. That's the Open/Closed Principle delivered by Strategy.

**Why there's no interface:** The classic pattern uses a `Strategy` interface so you can swap implementations at runtime. Here, `ChatClient` already *is* the interface — Spring AI defines its contract. Our three instances are three strategies configured at construction time, not three classes implementing an interface. Same pattern, lighter syntax.

[INSERT DIAGRAM 3: "Strategy: AiClient Internals" — diagrams-post-7.html]

---

## 3. DEEP DIVE: Adapter — Dual Translation Layer

**The GoF definition:** Convert the interface of a class into another interface clients expect.

We have a **dual adapter** — one facing outward (toward Gemini), one facing inward (from Gemini back to our domain). Together they form a translation layer that isolates the entire app from the AI provider.

### Outgoing Adapter: AiClient

`AiClient` adapts our domain language (`generateTacticalAdvice(prompt)`) into the Spring AI / Gemini protocol (`client.prompt().user(prompt).call().content()`). Services call `aiClient.generateSeasonPlan(prompt)` — they never see `ChatClient`, `Mono.fromCallable`, or `Schedulers.boundedElastic`.

If we switch from Gemini to OpenAI or Claude, only `AiClient` changes. The services don't know and don't care.

### Incoming Adapter: AiResponseParser

```java
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public <T> T parseAiResponse(String aiResponse, Class<T> type, T fallback) {
        try {
            String json = stripMarkdownFences(aiResponse);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as {}: {}",
                      type.getSimpleName(), e.getMessage());
            return fallback;
        }
    }

    public String stripMarkdownFences(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.strip();
    }
}
```

Gemini returns raw text — often wrapped in markdown code fences, sometimes with extra whitespace. `AiResponseParser` translates that messy string into typed Java objects: `AiSeasonPlanResponse`, `AiTrainingPlanResponse`, etc.

The `stripMarkdownFences()` method handles Gemini's habit of wrapping JSON in ` ```json ... ``` `. The generic `parseAiResponse()` method handles any response type — a single adapter method for all AI outputs.

**The pattern:** `App domain → AiClient → Gemini API → raw string → AiResponseParser → typed records`. Two adapters forming a clean boundary around an external dependency.

[INSERT DIAGRAM 4: "Adapter: Dual Layer" — diagrams-post-7.html]

---

## 4. DEEP DIVE: Builder — Two Flavors

**The GoF definition:** Separate the construction of a complex object from its representation so that the same construction process can create different representations.

We use Builder in two different ways — Lombok-generated and manual fluent builder.

### Flavor 1: Lombok @Builder (all 11 entities)

```java
@Node
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeasonPlan {

    @Id @GeneratedValue
    private Long id;

    @Relationship(type = "SEASON_PLAN_FOR", direction = Relationship.Direction.OUTGOING)
    private Team team;

    private String season;
    private List<String> objectives;

    @Relationship(type = "HAS_WORKLOAD", direction = Relationship.Direction.OUTGOING)
    private List<PlayerWorkloadSnapshot> workloadSnapshots;

    private String summary;
    private OffsetDateTime createdAt;
}
```

Call sites read like named parameters:

```java
SeasonPlan plan = SeasonPlan.builder()
        .team(team)
        .season(input.season())
        .objectives(parsed.objectives())
        .workloadSnapshots(snapshots)
        .summary(parsed.summary())
        .createdAt(OffsetDateTime.now())
        .build();
```

Compare that to `new SeasonPlan(null, team, input.season(), parsed.objectives(), snapshots, parsed.summary(), OffsetDateTime.now())` — positional arguments where one wrong order silently corrupts data.

Lombok generates the entire builder class at compile time. Same bytecode as writing it by hand, zero maintenance cost.

### Flavor 2: ChatClient.Builder.clone() (AiClient constructor)

```java
public AiClient(ChatClient.Builder builder) {
    this.tacticalClient = builder.clone()
            .defaultSystem("You are an expert football/soccer tactical analyst...")
            .build();

    this.seasonPlanClient = builder.clone()
            .defaultSystem("You are an expert football/soccer season planner...")
            .build();
}
```

This is a **prototype + builder** combo. Spring auto-configures one `ChatClient.Builder` with the API key, model name, and endpoint. We `clone()` it three times and customize each clone with a different system prompt. Without `.clone()`, all three calls would mutate the same builder, and the last system prompt would overwrite the first two.

**Two builders, one principle:** Construct complex objects step-by-step, making the code self-documenting through named methods instead of positional arguments.

---

## 5. DEEP DIVE: Template Method — GraphQLExceptionHandler

**The GoF definition:** Define the skeleton of an algorithm in a superclass, letting subclasses override specific steps without changing the algorithm's structure.

Spring's `DataFetcherExceptionResolverAdapter` defines the skeleton: catch any exception thrown by a GraphQL resolver, convert it to a `GraphQLError`, and return it to the client. The "specific step" we override is `resolveToSingleError()`:

```java
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof EntityNotFoundException e) {
            return buildError(e.getMessage(), ErrorType.NOT_FOUND, env,
                    Map.of("entityType", e.getEntityType(),
                           "entityId", String.valueOf(e.getEntityId())));
        }

        if (ex instanceof ConstraintViolationException e) {
            String message = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return buildError(message, ErrorType.BAD_REQUEST, env, Map.of());
        }

        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            return buildError("Authentication required", ErrorType.UNAUTHORIZED, env, Map.of());
        }

        if (ex instanceof AiGenerationException e) {
            log.error("AI generation error: {}", e.getMessage(), e);
            return buildError(e.getMessage(), ErrorType.INTERNAL_ERROR, env,
                    Map.of("operation", e.getOperation()));
        }

        log.error("Unexpected error in GraphQL resolver", ex);
        return buildError("Internal server error", ErrorType.INTERNAL_ERROR, env, Map.of());
    }
}
```

**What the framework does for us** (the "skeleton"):
1. Intercepts every resolver exception
2. Calls our `resolveToSingleError()` with the exception and the GraphQL environment
3. Wraps the returned `GraphQLError` into the response
4. Handles null returns (fallback to default error)

**What we do** (the "override"):
- Map each exception type to a specific error classification and message
- Include domain-specific extensions (`entityType`, `operation`)
- Log unexpected errors

We never call `resolveToSingleError()` ourselves. Spring calls it when an exception happens. That's the Template Method contract: **the framework controls the flow, we supply the behavior**.

**Why not just a try-catch?** Because every GraphQL resolver would need the same try-catch wrapping. The Template Method centralizes error mapping in one place while Spring handles the plumbing.

---

## 6. DEEP DIVE: Sealed Classes + Enums — Compiler-Enforced Hierarchies

**The context:** Java 17 introduced `sealed` classes, and our exception hierarchy uses them alongside 7 type-safe enums. Together, they replace what the classic GoF would handle with Visitor pattern — with zero boilerplate.

### Sealed Exception Hierarchy

```java
public sealed class CoachException extends RuntimeException
        permits EntityNotFoundException, AiGenerationException {

    protected CoachException(String message) {
        super(message);
    }

    protected CoachException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
public final class EntityNotFoundException extends CoachException {
    private final String entityType;
    private final Object entityId;

    public EntityNotFoundException(String entityType, Object entityId) {
        super("%s not found with id: %s".formatted(entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
```

```java
public final class AiGenerationException extends CoachException {
    private final String operation;

    public AiGenerationException(String operation, Throwable cause) {
        super("AI generation failed for: %s".formatted(operation), cause);
        this.operation = operation;
    }
}
```

**What `sealed` gives you:**

1. **Compiler-enforced completeness** — if you switch on `CoachException`, the compiler knows the only subtypes are `EntityNotFoundException` and `AiGenerationException`. Future Java versions will warn if you miss a case.
2. **Intentional design** — `sealed` says "this hierarchy is closed on purpose." A rogue `class HackException extends CoachException` won't compile unless you add it to the `permits` clause.
3. **Documentation as code** — reading `sealed ... permits A, B` tells you the exact scope of domain exceptions without digging through the codebase.

### 7 Type-Safe Enums

```java
public enum FatigueLevel  { FRESH, MODERATE, TIRED, EXHAUSTED }
public enum InjuryRisk    { LOW, MEDIUM, HIGH }
public enum TrainingIntensity { LOW, MEDIUM, HIGH }
public enum FocusArea     { PRESSING, BUILD_UP, DEFENCE }
public enum TacticalStyle { POSSESSION, DIRECT, BALANCED }
public enum RiskLevel     { LOW, MEDIUM, HIGH }
public enum FormIndicator { IMPROVING, DECLINING, STABLE }
```

Each one replaces a `String` field — meaning `"TRED"` (a typo of `"TIRED"`) no longer compiles. The `computeInjuryRisk()` method in `SeasonPlanService` shows switch expressions with enums:

```java
private InjuryRisk computeInjuryRisk(FatigueLevel fatigueLevel, int matches) {
    if (matches >= 6) return InjuryRisk.HIGH;
    return switch (fatigueLevel) {
        case EXHAUSTED -> InjuryRisk.HIGH;
        case TIRED     -> InjuryRisk.MEDIUM;
        default        -> InjuryRisk.LOW;
    };
}
```

If you add `CRITICAL` to `FatigueLevel`, the compiler flags every switch that doesn't handle it. With strings, you'd silently fall into `default` and never know.

[INSERT DIAGRAM 5: "Sealed Class Hierarchy" — diagrams-post-7.html]

---

## 7. Medium-Coverage Patterns

### Facade — SeasonPlanService

`SeasonPlanService` has **6 dependencies**:

```java
@Service
@RequiredArgsConstructor
public class SeasonPlanService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final SeasonPlanRepository seasonPlanRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
```

The controller calls **one method**: `generateSeasonPlan(input)`. Behind that single entry point, the service orchestrates:

1. Fetch team from `TeamRepository`
2. Fetch players from `PlayerRepository`
3. Fetch match stats from `PlayerMatchStatRepository`
4. Compute workload snapshots (fatigue + injury risk)
5. Build prompt and call `AiClient`
6. Parse response via `AiResponseParser`
7. Save plan to `SeasonPlanRepository`

The controller has zero knowledge of repositories, AI clients, or parsing. That's a textbook Facade — a simplified interface to a complex subsystem.

[INSERT DIAGRAM 6: "Facade: SeasonPlanService" — diagrams-post-7.html]

### Decorator / Filter Chain — JwtAuthenticationFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
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
}
```

The servlet **Filter Chain** is a variant of the Decorator pattern. Each filter wraps the next one: it can modify the request before forwarding, modify the response after, or short-circuit the chain entirely. Our JWT filter adds authentication context, then calls `filterChain.doFilter()` to pass control to the next decorator in line.

Spring Security chains multiple filters — CORS, CSRF, session management, our JWT filter, authorization — each one adding a layer of behavior without modifying the others.

### Repository — Spring Data Neo4j Interfaces

```java
public interface TeamRepository extends Neo4jRepository<Team, Long> {}

public interface PlayerRepository extends Neo4jRepository<Player, Long> {
    List<Player> findByTeamId(Long teamId);
}

public interface PlayerMatchStatRepository extends Neo4jRepository<PlayerMatchStat, Long> {
    List<PlayerMatchStat> findByPlayerIdAndMatchDateAfter(Long playerId, LocalDate date);
}
```

The GoF Repository pattern mediates between the domain and data mapping layers using a collection-like interface. Spring Data takes this further — you declare method signatures, and the framework **generates the implementation** at runtime from method names.

`findByPlayerIdAndMatchDateAfter` — no SQL, no Cypher, no implementation code. Spring Data parses the method name, builds the query, executes it, and maps the results. Nine repositories, zero implementation classes.

### Factory Method — buildWorkloadSnapshot() and buildPrompt()

```java
private PlayerWorkloadSnapshot buildWorkloadSnapshot(Player player, LocalDate cutoff) {
    List<PlayerMatchStat> recentStats =
            playerMatchStatRepository.findByPlayerIdAndMatchDateAfter(player.getId(), cutoff);

    int matches = recentStats.size();
    int minutes = recentStats.stream().mapToInt(PlayerMatchStat::getMinutesPlayed).sum();
    FatigueLevel fatigue = computeFatigueLevel(minutes);
    InjuryRisk injuryRisk = computeInjuryRisk(fatigue, matches);

    return PlayerWorkloadSnapshot.builder()
            .player(player)
            .matchesLast28Days(matches)
            .minutesLast28Days(minutes)
            .fatigueLevel(fatigue)
            .injuryRisk(injuryRisk)
            .comment("%d matches, %d min in last 28 days".formatted(matches, minutes))
            .createdAt(OffsetDateTime.now())
            .build();
}
```

These `build*()` methods are Factory Methods — they encapsulate complex object creation logic. The caller says "give me a workload snapshot for this player" without knowing about stat aggregation, fatigue computation, or injury risk calculation. The factory hides the recipe.

### Records as Value Objects

```java
public record SeasonPlanInput(
    @NotNull Long teamId,
    @NotBlank String season,
    String priority
) {}

public record AiSeasonPlanResponse(
    String summary,
    List<String> objectives
) {}

public record PlayerPerformanceTrend(
    Player player,
    long matchCount,
    int totalGoals, int totalAssists,
    double goalsPerMatch, double assistsPerMatch,
    FormIndicator form,
    List<PlayerMatchStat> matches
) {}
```

Java records are **immutable value objects** by design — `final` fields, auto-generated `equals()`, `hashCode()`, `toString()`, no setters. They're the GoF Value Object pattern enforced by the language itself. Our 8 DTOs are records: data in, data out, no mutation.

---

## 8. Quick Reference Table

| Pattern | Where | One-Line Explanation |
|---------|-------|---------------------|
| **Singleton** | Every `@Component`, `@Service`, `@Controller` | Spring creates one instance per bean by default — container-managed Singleton |
| **Dependency Injection** | `@RequiredArgsConstructor` + `final` fields | The container decides *which* implementation to inject — Inversion of Control |
| **DTO** | All `*Input` and `*Response` records | Data transfer objects decouple the API contract from the domain model |
| **Layered Architecture** | Controller → Service → Repository | Each layer depends only on the layer below — changes are isolated |
| **Transactional Script** | `@Transactional` on `generateSeasonPlan()` | A method that orchestrates a business operation within a single transaction boundary |

---

## 9. Patterns Working Together — Tracing generateSeasonPlan()

A single API call activates **11 patterns** in sequence. Here's the trace:

```
GraphQL request: mutation { generateSeasonPlan(input: {...}) }
```

| Step | What Happens | Pattern(s) |
|------|-------------|------------|
| 1 | Request hits `JwtAuthenticationFilter` | **Filter Chain / Decorator** |
| 2 | Spring routes to `CoachGraphQLController` | **Singleton**, **Layered Architecture** |
| 3 | Controller calls `seasonPlanService.generateSeasonPlan(input)` | **Facade**, **Dependency Injection** |
| 4 | Service calls `teamRepository.findById()` | **Repository** |
| 5 | Service calls `buildWorkloadSnapshot()` for each player | **Factory Method** |
| 6 | `computeFatigueLevel()` / `computeInjuryRisk()` use **enum switch** | **Sealed Classes + Enums** |
| 7 | Service calls `buildPrompt()` | **Factory Method** |
| 8 | Service calls `aiClient.generateSeasonPlan(prompt)` | **Strategy** (selects `seasonPlanClient`) |
| 9 | `callClient()` sends prompt to Gemini | **Adapter** (outgoing) |
| 10 | `aiResponseParser.parseAiResponse()` converts JSON to typed record | **Adapter** (incoming) |
| 11 | Service calls `SeasonPlan.builder()...build()` | **Builder** |
| 12 | `seasonPlanRepository.save(plan)` persists to Neo4j | **Repository**, **Transactional Script** |
| 13 | If anything throws, `GraphQLExceptionHandler` catches it | **Template Method** |

That's **11 distinct patterns** in a single request. Not because we over-engineered it — because Spring Boot, Lombok, and modern Java compose naturally into pattern-rich architectures.

[INSERT DIAGRAM 2: "Request Flow" — diagrams-post-7.html]

---

## 10. Comprehension Check

**1. Why is AiClient a Strategy pattern even without a Strategy interface?**

Because `ChatClient` already *is* the interface — it defines the contract (`.prompt().user().call().content()`). The three instances are three strategies configured via different system prompts. `callClient()` is the context that delegates to whichever strategy it receives. The GoF pattern doesn't require a new interface when the existing type already provides polymorphism.

**2. Why does AiResponseParser qualify as an Adapter, not just a utility class?**

An Adapter converts one interface to another. `AiResponseParser` converts Gemini's raw string output (potentially wrapped in markdown fences) into typed Java records (`AiSeasonPlanResponse`, `AiTrainingPlanResponse`). It's specifically translating between two incompatible interfaces — the AI provider's response format and our domain's type system. A utility class would be something like `StringUtils` — general-purpose, not bridging two specific systems.

**3. What would break if AiClient's constructor used the same builder without .clone()?**

All three `ChatClient` instances would share the same builder. Each `.defaultSystem()` call would overwrite the previous one, so all three clients would end up with the last system prompt (training coach). You'd get training-coach-style responses even when asking for tactical advice or season plans.

**4. How does sealed differ from final?**

`final` says "no one can extend this class." `sealed` says "only these specific classes can extend this class." `CoachException` is sealed — it allows exactly `EntityNotFoundException` and `AiGenerationException`. Those two subclasses are `final` — no one can extend them further. Sealed gives you a **controlled, complete hierarchy** instead of a binary open/closed choice.

**5. Why is the Filter Chain a Decorator variant, not just the Chain of Responsibility?**

Both patterns chain handlers. The difference: Chain of Responsibility passes requests until one handler processes it (and usually stops). Decorator wraps behavior — each filter adds functionality and always forwards to the next filter. Our JWT filter *always* calls `filterChain.doFilter()` regardless of whether it found a valid token. It decorates the request with authentication context, then passes it on.

**6. What happens when you add a new enum value to FatigueLevel?**

Every `switch` expression that handles `FatigueLevel` gets a compiler warning if it doesn't cover the new case (assuming no `default`). Even with a `default` branch, your IDE will highlight it. This is the same exhaustiveness check that sealed classes provide — the compiler knows all possible values and flags incomplete handling.

---

The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#DesignPatterns #SpringBoot #Java #SoftwareEngineering #LearningInPublic #CleanCode
