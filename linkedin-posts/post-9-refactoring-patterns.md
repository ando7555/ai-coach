# TEASER POST (copy-paste into LinkedIn post composer — attach diagram screenshots as carousel images)

I ran Martin Fowler's code smell checklist against my Spring Boot app. Found 13 smells across 15+ files. Every fix maps to a named refactoring pattern from his book.

3 PRs. Zero behavior changes (except one: AI errors now fail properly instead of being silently swallowed).

Here's the smell safari:

-> Duplicated Code: cursor pagination copy-pasted verbatim across 2 services. Extract Class into a generic CursorPaginator eliminated ~40 lines.
-> Feature Envy: a controller was building entities, fetching teams, doing the service's job. Move Method + Introduce Parameter Object put logic where it belongs.
-> Primitive Obsession: User.role was a raw String validated with Set<String>. Replace Type Code with Class gave us a UserRole enum with compile-time safety.
-> Large Class: one controller handled 3 unrelated domains. Extract Class split it into 3 focused controllers — one reason to change each.
-> Lazy Class: AiGenerationException existed but was never thrown. Meanwhile, errors were silently swallowed. Activated the exception, removed the dead code.
-> Magic Numbers: "28", "Bearer ", substring(7), "Balanced" — all replaced with named constants.
-> Inconsistent Pattern: 2 of 3 AI services used structured JSON parsing. The third split on \n. Fixed the outlier.

Every smell has a Fowler name. Every fix has a named refactoring pattern. Full article with before/after code in the comments.

The code is open source: https://github.com/ando7555/ai-coach

#Refactoring #CodeSmells #CleanCode #MartinFowler #SpringBoot #Java #SoftwareEngineering #LearningInPublic

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** AI Coach Post 9: Martin Fowler's Refactoring Patterns — A Code Smell Safari

---

I'm building an AI-powered football coaching app called **AI Coach** — Spring Boot 3.5, GraphQL, Neo4j, Google Gemini. After the [previous article](https://www.linkedin.com/pulse/algorithms-data-structures-system-design-andranik-muradyan) on algorithmic performance, I turned the lens from *performance* to *structure*.

Post 8 fixed how fast the code runs. Post 9 fixes how well the code *reads*.

I walked through every service, controller, and config file with Martin Fowler's *Refactoring* smell catalog open beside me. I found 13 named smells across 15+ files. Each fix maps to a named Fowler refactoring pattern. Every change is a behavioral no-op — same inputs, same outputs — except one intentional improvement to error handling.

**3 PRs. 13 smells. 13 named refactoring patterns. Zero broken tests.**

[INSERT DIAGRAM 1: "The Smell Catalog" — diagrams-post-9.html]

---

## 1. The Smell Catalog

| # | Fowler Smell | Fowler Refactoring | Where |
|---|---|---|---|
| A1 | Duplicated Code, Shotgun Surgery | Extract Class | CursorPaginator |
| A2 | Duplicated Code, Data Clumps | Extract Method (generic) | EnumParser |
| A3 | Magic Number | Replace Magic Number with Constant | SeasonPlanService |
| A4 | Magic Strings | Replace Magic String with Constant | AiClient prompts |
| A5 | Magic String/Number | Replace Magic Number with Constant | JWT filter |
| B1 | Large Class, Divergent Change | Extract Class | Split CoachGraphQLController |
| B2 | Feature Envy | Move Method + Introduce Parameter Object | createPlayer |
| B3 | Feature Envy | Move Class | StatAccumulator to domain |
| B4 | Lazy Class, Speculative Generality | Remove Dead Code + Activate | AiGenerationException |
| C1 | Primitive Obsession | Replace Type Code with Class | UserRole enum |
| C2 | Inconsistent Pattern | Introduce Consistent Pattern | AiMatchAnalysisResponse |
| C3 | Inappropriate Intimacy | Remove Inappropriate Intimacy | Match ID in prompt |
| C4 | Magic Strings, Config as Code | Externalize Configuration | CORS origins |

Let me walk through the four most interesting patterns in detail.

---

## 2. Deep Dive: Extract Class — CursorPaginator (A1)

**Smell:** Duplicated Code + Shotgun Surgery

`MatchService` and `PlayerMatchStatService` both had identical code:

```java
// BEFORE — duplicated in both services
private static final int DEFAULT_PAGE_SIZE = 20;

static String encodeCursor(Long id) {
    return Base64.getEncoder()
        .encodeToString(("cursor:" + id).getBytes());
}

static Long decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) return null;
    String decoded = new String(Base64.getDecoder().decode(cursor));
    return Long.valueOf(decoded.substring("cursor:".length()));
}

// + identical cursor-index loop + page slicing
```

The pagination loop was also copy-pasted: find the cursor position, slice the list, check `hasNextPage`, build edges. ~40 lines duplicated verbatim.

**Why it's dangerous:** If we needed to change cursor format (say, from Base64 to URL-safe encoding), we'd have to find and fix it in both places. That's **Shotgun Surgery** — one logical change requires editing multiple classes.

**Fix: Extract Class**

```java
// AFTER — single utility
public final class CursorPaginator {
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static <T> Page<T> paginate(
            List<T> items,
            Function<T, Long> idGetter,
            Integer first, String after) {
        // cursor logic lives here once
    }

    public record Page<T>(
        List<T> items, PageInfo pageInfo, int totalCount
    ) {}
}
```

Both services now delegate in a single line:

```java
CursorPaginator.Page<Match> page =
    CursorPaginator.paginate(allMatches, Match::getId, first, after);
```

**Why it works:** Extract Class gathers scattered responsibilities into a cohesive unit. The `Function<T, Long>` parameter makes it generic — any entity with a Long ID can use it. The DRY principle isn't about avoiding repetition for its own sake; it's about ensuring every piece of knowledge has a single, authoritative source.

[INSERT DIAGRAM 2: "Extract Class — Before vs After" — diagrams-post-9.html]

---

## 3. Deep Dive: Replace Type Code with Class — UserRole (C1)

**Smell:** Primitive Obsession

`User.role` was a raw `String`. The validation chain was manual and scattered:

```java
// AuthService — manual validation
private static final Set<String> VALID_ROLES = Set.of("COACH", "ADMIN");
String normalizedRole = role != null ? role.toUpperCase() : null;
String assignedRole = normalizedRole != null
    && VALID_ROLES.contains(normalizedRole)
    ? normalizedRole : "COACH";

// JwtAuthenticationFilter — manual fallback
var authority = new SimpleGrantedAuthority(
    "ROLE_" + (role != null ? role : "COACH"));
```

The string `"COACH"` appeared as a magic default in **two separate files**. A typo like `"COCH"` would compile fine but create a user with no valid permissions.

**Fix: Replace Type Code with Class**

```java
// The enum — 4 lines
public enum UserRole { COACH, ADMIN }

// AuthService — one line replaces Set + ternary
UserRole assignedRole = EnumParser.parse(
    UserRole.class, role, UserRole.COACH);

// JwtAuthenticationFilter — type-safe throughout
UserRole role = EnumParser.parse(
    UserRole.class, tokenProvider.getRole(token), UserRole.COACH);
var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
```

**Why it works:** Enums make invalid states unrepresentable at compile time. You can't create a `UserRole` of `"COCH"` — the compiler catches it. The `EnumParser` utility (from A2) gives us a clean fallback pattern for parsing external input (like JWT claims) where the type boundary exists.

Notice how the `EnumParser` we extracted for one smell (Data Clumps in `TrainingPlanService`) immediately found a second use here. Good abstractions attract reuse.

[INSERT DIAGRAM 3: "Primitive Obsession — String vs Enum" — diagrams-post-9.html]

---

## 4. Deep Dive: Feature Envy — createPlayer (B2)

**Smell:** Feature Envy + Missing Parameter Object

The controller was doing the service's job:

```java
// BEFORE — controller envies the service layer
@MutationMapping
public Player createPlayer(@Argument Long teamId,
                           @Argument String name,
                           @Argument String position,
                           @Argument Double rating) {
    Team team = teamService.getTeam(teamId);  // controller fetches entities
    Player player = Player.builder()           // controller builds entities
            .name(name)
            .position(position)
            .rating(rating)
            .team(team)
            .build();
    return playerService.createPlayer(player); // then hands off to service
}
```

Three problems:
1. **Feature Envy** — the controller reaches into the service layer's responsibilities
2. **Missing Parameter Object** — 4 loose parameters instead of a validated DTO
3. **Inconsistency** — every other mutation uses an input type, but this one takes raw arguments

**Fix: Introduce Parameter Object + Move Method**

```java
// New validated DTO
public record CreatePlayerInput(
    @NotNull Long teamId,
    @NotNull String name,
    @NotNull String position,
    Double rating
) {}

// Controller — thin delegate
@MutationMapping
public Player createPlayer(@Argument @Valid CreatePlayerInput input) {
    return playerService.createPlayer(input);
}

// Service — owns entity construction
@Transactional
public Player createPlayer(CreatePlayerInput input) {
    Team team = teamRepository.findById(input.teamId())
        .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));
    return playerRepository.save(Player.builder()
        .name(input.name())
        .position(input.position())
        .rating(input.rating())
        .team(team)
        .build());
}
```

**Why it works:** Controllers should be thin delegates — validate input, call service, return result. Entity construction and repository lookups belong in the transactional service layer. Introduce Parameter Object bundles related data into a cohesive DTO that can carry its own validation rules (`@NotNull`).

[INSERT DIAGRAM 4: "Feature Envy — Before vs After" — diagrams-post-9.html]

---

## 5. Deep Dive: Activate Dead Code — AiGenerationException (B4)

**Smell:** Lazy Class + Speculative Generality + Silent Failure

This was the most interesting find. Three things existed:
1. `AiGenerationException` — a sealed exception class
2. `GraphQLExceptionHandler` — with a handler for `AiGenerationException`
3. `AiClient.onErrorResume(e -> Mono.empty())` — which **silently swallowed all AI errors**

The exception class was designed. The handler was written. But the exception was never thrown. Instead, AI failures silently returned `Mono.empty()`, which became the string `"No analysis generated."` — a generic fallback that masked API key problems, rate limits, and network errors.

```java
// BEFORE — silent swallow
private Mono<String> callClient(ChatClient client, String prompt, String context) {
    return Mono.fromCallable(() -> client.prompt().user(prompt).call().content())
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("{} generation failed", context, e))
        .onErrorResume(e -> Mono.empty());  // error goes here to die
}
```

**Fix: Remove Dead Code + Activate the Exception**

```java
// AFTER — fail-fast with proper exception
.onErrorMap(e -> new AiGenerationException(context, e));
```

One line change. Now AI failures propagate as structured GraphQL errors with the operation name, the original cause, and proper error classification.

**Why it works:** Fail-fast beats silent fallback. The `onErrorResume(Mono.empty())` was **dead code** in disguise — it looked like error handling but actually prevented error handling. The exception class was **speculative generality** — built for a future that never came because the error path was blocked. Removing the swallow and activating the exception completes a circuit that was always meant to exist.

[INSERT DIAGRAM 5: "Dead Code Activation — Error Flow" — diagrams-post-9.html]

---

## 6. The Supporting Cast

The four deep dives cover the most educational patterns, but the remaining 9 refactorings are equally important for day-to-day code quality:

**Extract Method — EnumParser (A2):** Two parallel `Map<String, Enum>` structures in `TrainingPlanService` collapsed into a single generic `EnumParser.parse(Class<E>, String, E)`. Java's `Class.getEnumConstants()` eliminates pre-built lookup maps.

**Magic Numbers & Strings (A3, A4, A5):** `28` became `RECENT_WINDOW_DAYS`. Three inline system prompts became named constants. `"Bearer "` + `substring(7)` became `BEARER_PREFIX` + `BEARER_PREFIX.length()`.

**Large Class — Split CoachGraphQLController (B1):** One controller handling match analysis, training plans, and season plans split into three focused controllers. Each has one service dependency and one reason to change.

**Move Class — StatAggregator (B3):** A private inner record whose methods exclusively accessed `PlayerMatchStat` getters moved to the `domain` package alongside `FormCalculator` and `WorkloadCalculator`.

**Consistent Pattern — AiMatchAnalysisResponse (C2):** The one AI service that split responses on `\n` instead of using structured JSON parsing aligned with the pattern used by the other two services.

**Inappropriate Intimacy (C3):** Internal Neo4j database ID removed from the AI prompt. Replaced with match date — context the AI can actually use.

**Externalize Configuration (C4):** Hardcoded CORS origins moved to `application.yaml` with environment variable override. Deploy to production without a code change.

[INSERT DIAGRAM 6: "The Complete Refactoring Map" — diagrams-post-9.html]

---

## 7. What I Learned

**Name the smell before you fix it.** Fowler's taxonomy isn't academic — it's diagnostic. When you can say "this is Feature Envy," the refactoring pattern suggests itself. When you can say "this is Primitive Obsession," you know the fix is Replace Type Code with Class. The names are the tool.

**Behavioral no-ops build trust.** Every change in these 3 PRs produces identical outputs for identical inputs (except B4, where silent failure became proper error reporting). This makes code review easy: reviewers can focus on structural improvement without worrying about regression.

**Good abstractions attract reuse.** The `EnumParser` extracted for `TrainingPlanService` (A2) immediately found a second use in `AuthService` (C1). The `CursorPaginator` (A1) replaced duplicated code in two services. Abstractions that solve real problems get reused; abstractions designed speculatively (like the unused `AiGenerationException`) gather dust until someone activates them.

**Consistency is a design principle.** The most impactful fix might be the least dramatic: making `MatchAnalysisService` parse AI responses the same way as `TrainingPlanService` and `SeasonPlanService` (C2). No algorithm changed. No performance improved. But the next developer who reads the codebase will understand all three services by understanding one.

---

*Previously: [Post 8 — Algorithms, Data Structures & System Design](https://www.linkedin.com/pulse/algorithms-data-structures-system-design-andranik-muradyan) | Next: Post 10 — Testing & CI/CD*

*The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)*
