# TEASER POST (copy-paste into LinkedIn post composer — attach diagram screenshots as carousel images)

I deleted a 355-line God class and my app still works exactly the same. 🔪

Here's the anatomy of an architecture refactoring — 7 commits, 15 issues fixed, zero behavior changes.

-> 🐛 SecurityConfig had duplicate .csrf() calls + unreachable dead code after .build()
-> 🔄 4 entities had manual getters/setters, 7 used Lombok — now all 11 use the same pattern
-> 🔄 6 classes had manual constructors, 7 used @RequiredArgsConstructor — unified to one style
-> 🛡️ "fatigueLevel" was a String — typo "TRED" instead of "TIRED" compiles fine. Now it's an enum.
-> 📦 Controller called PlayerRepository directly — extracted PlayerService
-> ✂️ CoachService was 355 lines with 3 unrelated responsibilities — split into 3 focused services
-> 🧹 AiClient had 3 copy-pasted methods — extracted a helper, each is now a one-liner

Before: inconsistent patterns everywhere. After: unified architecture, same external behavior.

Full article in the comments 👇

🔗 The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#Refactoring #SOLID #CleanCode #SpringBoot #DesignPatterns #SoftwareEngineering #LearningInPublic

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** Anatomy of an Architecture Refactoring — 15 Issues, 7 Commits, Zero Behavior Changes

---

I'm building an AI-powered football coaching app called **AI Coach**. After shipping 5 features across multiple PRs, the codebase had grown inconsistently — mixed patterns, a God class, stringly-typed fields, and copy-pasted methods. This article walks through the complete refactoring: what was wrong, why it matters, and how each fix works.

The golden rule: **refactoring changes structure, never behavior.** Every GraphQL query and mutation works exactly the same before and after.

---

## 1. The Problem — Death by a Thousand Inconsistencies

When you build feature-by-feature, each PR focuses on making one thing work. You don't go back and unify patterns across the whole codebase. Over time, this creates "architectural drift":

| Issue | Example | Why It Matters |
|-------|---------|---------------|
| Mixed entity styles | 4 entities manual, 7 Lombok | Cognitive overhead — two patterns for the same thing |
| Mixed DI styles | 7 `@RequiredArgsConstructor`, 6 manual constructors | Same problem — readers have to parse two idioms |
| God class | `CoachService` = 355 lines, 3 responsibilities | Hard to test, hard to modify, violates Single Responsibility |
| Stringly-typed fields | `fatigueLevel = "TRED"` compiles fine | Bugs hide until runtime — no compile-time safety |
| Copy-pasted methods | `AiClient` has 3 identical methods | DRY violation — change one, forget the others |
| Controller calls repository | `TeamGraphQLController` → `PlayerRepository` | Breaks layered architecture — controllers should use services |

None of these are bugs. The app works fine. But each one makes the codebase harder to work with, and they compound over time.

[INSERT DIAGRAM 1: "Before vs After Architecture" — diagrams-post-6.html]

---

## 2. Phase 1: Quick Fixes (Zero Risk)

### SecurityConfig — Duplicate Calls + Dead Code

The original `SecurityConfig` had this:

```java
return http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())           // duplicate!
    .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))  // duplicate!
    .authorizeHttpRequests(auth -> auth ...)
    .addFilterBefore(jwtFilter, ...)
    .build();
    // Everything below here is dead code — .build() already returned
    .requestMatchers("/graphql").permitAll()
    ...
    .build();
```

Spring Security's `HttpSecurity` is a **mutable builder** — calling `.csrf()` twice just overwrites the first call with the same value. It's not harmful, but it's confusing. And the code after `.build()` is completely unreachable.

**Fix:** Remove duplicates, delete dead code. One clean chain.

### Package-Private Enums

```java
// Before: only visible within domain.entity package
enum FocusArea { PRESSING, BUILD_UP, DEFENCE }

// After: visible to DTOs, services, controllers
public enum FocusArea { PRESSING, BUILD_UP, DEFENCE }
```

Java's default access modifier is **package-private** — the class is only visible within its own package. Since DTOs and services live in different packages, they couldn't reference these enums. Adding `public` fixes the visibility.

### Delete Unused Code

`TacticalContextInput` was created early on but superseded by `RecommendationContextInput`. Dead code = noise. Delete it.

---

## 3. Phase 2: Entity Lombok Unification

### The Pattern

Before, 4 entities had manual boilerplate:

```java
// 49 lines for Team.java — manual getters, setters, constructors
@Node
public class Team {
    @Id @GeneratedValue
    private Long id;
    private String name;

    public Team() {}
    public Team(String name, ...) { this.name = name; ... }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... 8 more methods
}
```

After, all entities use the same 5-annotation pattern:

```java
// 24 lines for Team.java — Lombok generates everything
@Node
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String league;
    private String formation;

    @Relationship(type = "HAS_PLAYER")
    @Builder.Default
    private Set<Player> players = new HashSet<>();
}
```

### Why @Builder.Default Matters

Without `@Builder.Default`, calling `Team.builder().name("Arsenal").build()` sets `players` to `null` (not `new HashSet<>()`). The `@Builder.Default` annotation tells Lombok to use the field initializer as the default value when building.

### Updating Call Sites

Every `new Entity(...)` becomes `.builder()...build()`:

```java
// Before
Team team = new Team(name, league, formation);

// After
Team team = Team.builder()
    .name(name)
    .league(league)
    .formation(formation)
    .build();
```

Builders are more readable (named parameters), more flexible (optional fields), and consistent with the rest of the codebase.

[INSERT DIAGRAM 2: "Entity Pattern — Before vs After" — diagrams-post-6.html]

---

## 4. Phase 3: DI & Naming Consistency

### Constructor Injection — Two Idioms, One Purpose

```java
// Before: manual constructor (6 classes)
@Service
public class TeamService {
    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }
}

// After: Lombok generates the same constructor
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
}
```

`@RequiredArgsConstructor` generates a constructor for all `final` fields. The compiled bytecode is identical — this is purely a source-level simplification.

### The Exception: AiClient

```java
@Component
public class AiClient {
    // Manual constructor: each ChatClient needs a distinct system prompt via builder.clone()
    public AiClient(ChatClient.Builder builder) {
        this.tacticalClient = builder.clone()
            .defaultSystem("You are an expert football tactical analyst...")
            .build();
        this.seasonPlanClient = builder.clone()
            .defaultSystem("You are an expert season planner...")
            .build();
    }
}
```

This constructor does **real initialization work** — it clones a builder 3 times with different system prompts. `@RequiredArgsConstructor` can only assign parameters to fields. A comment explains why the exception exists.

### Naming Consistency

`CoachGraphqlController` → `CoachGraphQLController` — matching the `*GraphQLController` convention used by all 5 other controllers.

---

## 5. Phase 4: Type-Safe Enums

### The String Problem

```java
// Before: any string compiles
private String fatigueLevel;  // "FRESH", "TRED", "asdf", "" — all valid
private String injuryRisk;    // "LOW", "MEDIUM", "HIGH", "maybe" — all valid
private String intensity;     // same problem
```

```java
// After: only valid values compile
private FatigueLevel fatigueLevel;  // FRESH, MODERATE, TIRED, EXHAUSTED
private InjuryRisk injuryRisk;      // LOW, MEDIUM, HIGH
private TrainingIntensity intensity; // LOW, MEDIUM, HIGH
```

### Why This Matters

With strings, a typo like `"TRED"` instead of `"TIRED"` compiles fine and silently corrupts data. With enums:
- **Compile-time safety** — `FatigueLevel.TRED` doesn't compile
- **IDE autocomplete** — shows you the valid options
- **GraphQL validation** — the schema rejects invalid values before they reach Java
- **Switch exhaustiveness** — the compiler warns if you miss a case

### Neo4j Compatibility

Neo4j SDN stores enums as their `.name()` string — `FatigueLevel.FRESH` is stored as `"FRESH"` in the database. Existing data already uses these exact strings, so no migration is needed.

### DTO Cleanup

`MatchAnalysisInput` also moved from strings to enums:

```java
// Before
public record MatchAnalysisInput(
    @NotBlank String focusArea,   // any string
    @NotBlank String style,       // any string
    @NotBlank String riskLevel    // any string
) {}

// After
public record MatchAnalysisInput(
    @NotNull FocusArea focusArea,     // PRESSING, BUILD_UP, DEFENCE
    @NotNull TacticalStyle style,     // POSSESSION, DIRECT, BALANCED
    @NotNull RiskLevel riskLevel      // LOW, MEDIUM, HIGH
) {}
```

And `RecommendationContextInput` was moved from `domain.entity` to `domain.dto` — it's an input DTO, not an entity.

[INSERT DIAGRAM 3: "Type Safety — String vs Enum" — diagrams-post-6.html]

---

## 6. Phase 5: Extract PlayerService

### The Layered Architecture Violation

```java
// Before: controller directly calls repository
@Controller
public class TeamGraphQLController {
    private final PlayerRepository playerRepository;  // repository in controller!

    public Player createPlayer(...) {
        Player player = Player.builder()...build();
        return playerRepository.save(player);  // business logic in controller
    }
}
```

Controllers should **delegate** to services, not implement business logic. Even if the logic is trivial today, it won't stay trivial.

```java
// After: controller delegates to service
@Controller
@RequiredArgsConstructor
public class TeamGraphQLController {
    private final PlayerService playerService;  // service, not repository

    public Player createPlayer(...) {
        Player player = Player.builder()...build();
        return playerService.createPlayer(player);
    }
}
```

The `PlayerService` is thin today — just `playerRepository.save(player)`. But when you add validation, audit logging, or event publishing, the service layer is already in place.

---

## 7. Phase 6: Split the God Class

### What Was CoachService?

355 lines. 3 unrelated responsibilities:

1. **Match analysis** — build prompt, call AI, parse response, save to Neo4j
2. **Training plans** — build prompt, call AI, parse JSON response, create sessions, save
3. **Season plans** — compute player workload snapshots, build prompt, call AI, parse JSON, save

Plus shared utilities: `stripMarkdownFences()`, `normalizeIntensity()`, `parseTrainingPlanResponse()`, `parseSeasonPlanResponse()`.

### The Split

```
CoachService.java (355 lines, deleted)
    ├── MatchAnalysisService.java  (73 lines)  — match analysis generation
    ├── TrainingPlanService.java   (129 lines) — training plan generation
    ├── SeasonPlanService.java     (159 lines) — season plan + workload snapshots
    └── AiResponseParser.java     (38 lines)  — shared JSON parsing utility
```

### AiResponseParser — DRY the Duplicate Parsing

Before, the parsing logic was duplicated:

```java
// In CoachService — duplicated for training plans AND season plans
private AiTrainingPlanResponse parseTrainingPlanResponse(String aiResponse) {
    try {
        String json = stripMarkdownFences(aiResponse);
        return objectMapper.readValue(json, AiTrainingPlanResponse.class);
    } catch (Exception e) {
        log.warn("Failed to parse...");
        return fallback;
    }
}
```

After, a generic method handles any response type:

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
            log.warn("Failed to parse AI response as {}: {}", type.getSimpleName(), e.getMessage());
            return fallback;
        }
    }
}
```

### ObjectMapper as Spring Bean

Before: `private static final ObjectMapper objectMapper = new ObjectMapper();`

After: `private final ObjectMapper objectMapper;` — injected by Spring.

Why? Spring Boot auto-configures an `ObjectMapper` with sensible defaults (ISO dates, null handling, etc.). A static instance misses all that configuration and can't be mocked in tests.

[INSERT DIAGRAM 4: "God Class Split" — diagrams-post-6.html]

---

## 8. Phase 7: DRY AiClient

### Before: 3 Copy-Pasted Methods

```java
public Mono<String> generateTacticalAdvice(String prompt) {
    return Mono.fromCallable(() -> tacticalClient.prompt().user(prompt).call().content())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Tactical advice generation failed", e))
            .onErrorResume(e -> Mono.empty());
}

public Mono<String> generateSeasonPlan(String prompt) {
    return Mono.fromCallable(() -> seasonPlanClient.prompt().user(prompt).call().content())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Season plan generation failed", e))
            .onErrorResume(e -> Mono.empty());
}

// ... and generateTrainingPlan — identical structure
```

### After: One Helper, Three One-Liners

```java
public Mono<String> generateTacticalAdvice(String prompt) {
    return callClient(tacticalClient, prompt, "Tactical advice");
}

public Mono<String> generateSeasonPlan(String prompt) {
    return callClient(seasonPlanClient, prompt, "Season plan");
}

public Mono<String> generateTrainingPlan(String prompt) {
    return callClient(trainingPlanClient, prompt, "Training plan");
}

private Mono<String> callClient(ChatClient client, String prompt, String context) {
    return Mono.fromCallable(() -> client.prompt().user(prompt).call().content())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("{} generation failed", context, e))
            .onErrorResume(e -> Mono.empty());
}
```

The difference: when you need to add retry logic, timeout, or metrics, you change **one method** instead of three.

[INSERT DIAGRAM 5: "AiClient DRY Refactoring" — diagrams-post-6.html]

---

## 9. The Final Architecture

```
controller/          6 controllers, all @RequiredArgsConstructor
  Auth, Team, Match, Coach, PlayerMatchStat, Recommendation

service/             10 services, all @RequiredArgsConstructor (except AiClient)
  Auth, Team, Player (NEW), Match
  MatchAnalysis (NEW), TrainingPlan (NEW), SeasonPlan (NEW)
  PlayerMatchStat, Recommendation
  AiClient (refactored), AiResponseParser (NEW)

domain/entity/       All entities use Lombok, all enums public
  + FatigueLevel, InjuryRisk, TrainingIntensity (NEW enums)

domain/dto/          All input DTOs here (including RecommendationContextInput)
  MatchAnalysisInput uses enum types
```

### Stats

- **7 commits**, each builds independently
- **576 lines added, 575 removed** — same total, completely restructured
- **15 issues fixed**, 0 behavior changes
- **34 files touched**

---

## 10. Principles Applied

1. **Single Responsibility Principle** — one class, one reason to change. CoachService → 3 focused services.
2. **DRY (Don't Repeat Yourself)** — AiClient helper, AiResponseParser generic method.
3. **Layered Architecture** — controllers → services → repositories. No shortcuts.
4. **Type Safety** — enums over strings. Catch errors at compile time, not runtime.
5. **Consistency** — one way to do each thing. One entity pattern, one DI style, one naming convention.
6. **Behavioral Preservation** — refactoring changes structure, never behavior.

---

## Comprehension Check

**1. Why does @Builder.Default matter on Team.players?**

Without it, `Team.builder().name("Arsenal").build()` sets `players` to `null` instead of `new HashSet<>()`. Lombok's `@Builder` doesn't use field initializers by default — `@Builder.Default` tells it to.

**2. Why keep AiClient's manual constructor instead of @RequiredArgsConstructor?**

The constructor does real work: it clones a `ChatClient.Builder` three times with different system prompts. `@RequiredArgsConstructor` can only generate simple field assignments, not builder pattern calls.

**3. Why is a static ObjectMapper worse than an injected one?**

Spring Boot auto-configures an `ObjectMapper` with project-specific settings (date format, null handling, module registration). A static instance uses Jackson defaults, missing all that. It also can't be mocked in unit tests.

**4. What makes CoachService a "God class"?**

It has 3 unrelated responsibilities (match analysis, training plans, season plans) that change for different reasons. If the AI prompt format for training plans changes, you're editing the same file as season plan workload computation. The Single Responsibility Principle says each class should have one reason to change.

**5. How does Neo4j handle the String→enum migration?**

Neo4j SDN stores Java enums as their `.name()` string. `FatigueLevel.FRESH` is stored as `"FRESH"`. The existing data already uses these exact strings, so the enum fields deserialize correctly without any data migration.

---

The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#Refactoring #SpringBoot #CleanCode #DesignPatterns #SOLID #SoftwareEngineering #LearningInPublic
