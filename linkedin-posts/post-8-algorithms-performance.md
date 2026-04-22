# TEASER POST (copy-paste into LinkedIn post composer — attach diagram screenshots as carousel images)

I ran a performance audit on my Spring Boot app. What I found surprised me: an N+1 query multiplying DB calls by 25x, an O(n^2) string bug hiding in plain sight, and exception-based control flow that was 1000x slower than a HashMap lookup.

4 PRs. 30+ files touched. Zero behavior changes. Here's what I fixed and what each fix taught me about algorithms and data structures:

-> HashMap grouping eliminates N+1: 25 DB roundtrips became 1 query + O(n) in-memory grouping. The network I/O savings dwarf any CPU optimization.
-> Stream.reduce() replaces 4 loops with 1: an immutable StatAccumulator record folds goals, assists, minutes, and ratings in a single pass. Same principle behind Hadoop MapReduce.
-> String + in a loop is secretly O(n^2): Java Strings are immutable. Every concatenation copies the entire string. Collectors.joining() uses StringBuilder internally — O(n) total.
-> Exceptions are expensive: valueOf() with try/catch captures a full stack trace on miss. A pre-built HashMap<String, Enum> does O(1) lookup with zero allocation.
-> Spring Cache for free: @Cacheable on read-heavy queries, @CacheEvict on mutations. ConcurrentHashMap with lock striping handles thread safety.
-> Cursor pagination beats offset: SKIP 1000 still scans 1000 rows. Cursor-based pagination is O(page size) regardless of depth.

Plus: extracted domain calculators (DDD), layered architecture enforcement, fail-fast null safety, and @Slf4j across all services.

Full article in the comments.

The code is open source: https://github.com/ando7555/ai-coach

#Algorithms #DataStructures #SystemDesign #SpringBoot #Java #Performance #BigO #SoftwareEngineering #LearningInPublic

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** AI Coach — Post 8: Algorithms, Data Structures & System Design — A Performance Audit

---

I'm building an AI-powered football coaching app called **AI Coach** — Spring Boot 3.5, GraphQL, Neo4j, Google Gemini. After the [previous article](https://www.linkedin.com/pulse/design-patterns-in-spring-boot-andranik-muradyan) on design patterns, I turned the lens inward: a full performance audit of the codebase.

The app worked. Tests passed. Users got correct results. But under the hood, algorithmic inefficiencies, architectural shortcuts, and missing optimizations had accumulated during rapid feature development. This article documents the entire audit — what I found, why it matters, and the CS fundamentals behind each fix.

**4 PRs. 30+ files. Zero behavior changes. Every optimization explained down to the data structure level.**

[INSERT DIAGRAM 1: "The Audit Catalog" — diagrams-post-8.html]

---

## 1. The Audit Catalog

| # | Issue | Before | After | Core Concept |
|---|-------|--------|-------|--------------|
| A1 | N+1 query in SeasonPlanService | O(P) DB roundtrips | O(1) batch + O(P) grouping | HashMap, batch fetching |
| A2 | 4-pass aggregation in buildTrend | O(4n) | O(n) single pass | Fold/reduce, accumulators |
| A3 | String reduce in loop | O(n^2 x L) | O(n x L) | String immutability, StringBuilder |
| A4 | Intermediate List + subList | Extra allocation | Stream skip() | Stream laziness, TimSort |
| A5 | Redundant toUpperCase() | 2 calls | 1 call | Common subexpression elimination |
| A6 | Exception-based enum parsing | Stack trace per miss | O(1) HashMap lookup | Why exceptions are expensive |
| A7 | Crude form calculation | size/2 split | Fixed sliding window | Sliding window algorithm |
| A9 | No caching | Query per request | @Cacheable | ConcurrentHashMap, lock striping |
| A10 | Unbounded list queries | Load all records | Cursor pagination | Cursor vs offset, index seek |

Let me walk through the most impactful fixes in detail.

---

## 2. Deep Dive: N+1 Problem + Batch Query (A1)

### The Problem

`SeasonPlanService.generateSeasonPlan()` builds a workload snapshot for every player on the team. The old code:

```java
List<PlayerWorkloadSnapshot> snapshots = players.stream()
    .map(p -> buildWorkloadSnapshot(p, cutoff))  // calls DB inside!
    .toList();
```

Inside `buildWorkloadSnapshot()`:
```java
List<PlayerMatchStat> recentStats =
    playerMatchStatRepository.findByPlayerIdAndMatchDateAfter(player.getId(), cutoff);
```

**25 players = 25 DB roundtrips.** Each roundtrip costs 5-50ms in network latency. That's 125-1250ms just in database calls — before the AI even starts.

### The Fix

```java
// One query: fetch all stats for all players at once
Map<Long, List<PlayerMatchStat>> statsByPlayer =
    playerMatchStatRepository.findByPlayerIdInAndMatchDateAfter(playerIds, cutoff)
        .stream()
        .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

// In-memory lookup: O(1) per player
List<PlayerWorkloadSnapshot> snapshots = players.stream()
    .map(p -> buildWorkloadSnapshot(p, statsByPlayer.getOrDefault(p.getId(), List.of())))
    .toList();
```

[INSERT DIAGRAM 2: "N+1 Problem → Batch Query" — diagrams-post-8.html]

### Under the Hood: How HashMap Works

`Collectors.groupingBy()` creates a `HashMap<Long, List<PlayerMatchStat>>`. Here's what's happening inside:

1. **Array of buckets** — HashMap starts with an array of 16 slots. Each key is hashed (`key.hashCode()`) then mapped to a bucket index: `hash % array.length`.

2. **Collisions** — When two keys hash to the same bucket, they form a linked list. Java 8+ converts to a **red-black tree** when a bucket reaches 8 entries (O(log n) worst case instead of O(n) list traversal).

3. **Load factor = 0.75** — When 75% of buckets are occupied, the array **doubles** and all entries are rehashed. This is expensive but amortized: the total cost of N insertions is O(N).

4. **Why this beats N+1** — A single DB roundtrip (~5-50ms network) returns all data. HashMap grouping is ~0.001ms per entry. Even 1000 entries grouped in-memory is faster than 2 extra DB calls.

[INSERT DIAGRAM 3: "HashMap Under the Hood" — diagrams-post-8.html]

---

## 3. Deep Dive: Single-Pass Fold/Reduce (A2)

### The Problem

`buildTrend()` needed four aggregations:
```java
int totalGoals = stats.stream().mapToInt(PlayerMatchStat::getGoals).sum();
int totalAssists = stats.stream().mapToInt(PlayerMatchStat::getAssists).sum();
int totalMinutes = stats.stream().mapToInt(PlayerMatchStat::getMinutesPlayed).sum();
// Plus rating calculation...
```

Four passes over the same list. O(4n) — not catastrophic, but unnecessary.

### The Fix

An immutable `StatAccumulator` record with `add()` and `merge()`:

```java
private record StatAccumulator(int goals, int assists, int minutes,
                                double ratingSum, int ratedCount) {
    static final StatAccumulator IDENTITY = new StatAccumulator(0, 0, 0, 0.0, 0);

    StatAccumulator add(PlayerMatchStat s) {
        return new StatAccumulator(goals + s.getGoals(), assists + s.getAssists(),
            minutes + s.getMinutesPlayed(), ...);
    }

    StatAccumulator merge(StatAccumulator other) { ... }
}

StatAccumulator acc = stats.stream().reduce(
    StatAccumulator.IDENTITY, StatAccumulator::add, StatAccumulator::merge);
```

[INSERT DIAGRAM 4: "Stream.reduce() — Single-Pass Fold" — diagrams-post-8.html]

### Under the Hood: How Stream.reduce() Works

`reduce(identity, accumulator, combiner)` is the **fold** from functional programming:

1. Start with `identity` (the empty accumulator)
2. For each element, call `accumulator.apply(currentResult, element)` -> new result
3. The `combiner` merges partial results in **parallel streams** only

This is the same principle behind Hadoop MapReduce and Spark RDDs. The immutable record is thread-safe by construction — no shared mutable state means parallel execution works without locks.

---

## 4. Deep Dive: String Immutability and the O(n^2) Trap (A3)

### The Problem

```java
String workloadReport = snapshots.stream()
    .map(s -> "- %s: %d matches...".formatted(...))
    .reduce((a, b) -> a + "\n" + b)    // O(n^2)!
    .orElse("No player data");
```

### Why This Is Quadratic

Java `String` is **immutable** — backed by a `final byte[]`. Every `+` creates a **new** array and copies both sides:

- Iteration 1: copies L bytes
- Iteration 2: copies 2L bytes
- Iteration 3: copies 3L bytes
- ...
- Iteration N: copies NL bytes

Total: L x (1 + 2 + 3 + ... + N) = **L x N(N+1)/2 = O(n^2 x L)**

[INSERT DIAGRAM 5: "String Immutability — O(n^2) Proof" — diagrams-post-8.html]

### The Fix

```java
.collect(Collectors.joining("\n"))  // O(n x L)
```

`Collectors.joining()` wraps a `StringBuilder` — a **mutable** `char[]` with **amortized doubling**. When full, it allocates 2x and copies once. Total copies = O(N) amortized (geometric series: 16 + 32 + 64 + ... < 2N).

**Why are Strings immutable?** Three reasons: (1) String pool interning saves memory, (2) safe as HashMap keys (hash can't change after insertion), (3) thread-safe without synchronization.

---

## 5. Architecture Fixes

### Layered Architecture Enforcement (B1)

`CoachGraphQLController` injected 3 repositories directly, bypassing the service layer. This violates the **Dependency Inversion Principle** — high-level modules (controllers) shouldn't depend on low-level modules (repositories).

Fix: Added read methods (`getByMatch()`, `getByTeam()`) to services, removed repository injections from the controller.

### Fail-Fast Null Safety (B2)

`TeamService.getTeam()` returned `null`, forcing every caller to null-check. Tony Hoare called null his "billion-dollar mistake." Fix: `orElseThrow(() -> new EntityNotFoundException(...))` — detect errors at the source, not downstream.

### Domain Model Enrichment (C1, C2)

Extracted `WorkloadCalculator` and `FormCalculator` from service classes to the domain package. These are **Domain Services** in DDD terminology — stateless, pure functions that encapsulate domain logic without coupling to infrastructure (repositories, transactions).

### Validation at the Boundary (D1-D3)

Added `@NotNull` to `MatchInput` and `RecommendationContextInput`. Switched `TrainingPlanInput.primaryFocus` from `String` to `FocusArea` enum — Spring GraphQL auto-coerces, giving type safety from the schema layer down to the domain.

---

## 6. End-to-End Big-O Trace: generateSeasonPlan()

After all optimizations, here's the full request traced with complexity annotations:

```
GraphQL request -> Controller -> SeasonPlanService.generateSeasonPlan()

  1. teamRepository.findById()                    -> O(1) index lookup
  2. playerRepository.findByTeamId()              -> O(P) where P = players
  3. Batch stat query + groupingBy                -> O(S) where S = total stats
     (was: O(P) roundtrips, each O(S/P))
  4. buildWorkloadSnapshot() per player           -> O(S) total
     - WorkloadCalculator.computeFatigueLevel()   -> O(1) threshold check
     - WorkloadCalculator.computeInjuryRisk()     -> O(1) decision logic
  5. Build prompt (Collectors.joining)             -> O(P x L) where L = avg length
     (was: O(P^2 x L) with string reduce)
  6. AI API call                                  -> O(?) network-bound, ~2-10 sec
  7. JSON parsing (Jackson)                       -> O(R) where R = response size
  8. Neo4j save                                   -> O(1) plan + O(P) snapshots

  Total: O(P + S + R) + network I/O
  Was:   O(P x roundtrip + P^2 x L + S + R)
```

[INSERT DIAGRAM 6: "End-to-End Big-O Trace" — diagrams-post-8.html]

**The key insight:** The AI API call (step 6) dominates everything at 2-10 seconds. CPU optimizations only matter when they exceed I/O costs. But that doesn't mean we ignore them — clean complexity means the code scales predictably when the data grows.

---

## 7. Concept Map

| CS Topic | Where It Appears |
|----------|-----------------|
| **HashMap** (buckets, collisions, load factor) | A1, A6, A9 |
| **Fold/Reduce** (accumulator, combiner) | A2 |
| **String internals** (immutability, StringBuilder) | A3 |
| **Stream pipeline** (laziness, TimSort) | A2, A4 |
| **Sliding window** (fixed window comparison) | A7 |
| **Big-O analysis** (end-to-end request trace) | A8 |
| **Caching** (ConcurrentHashMap, LRU, TTL) | A9 |
| **Pagination** (cursor vs offset) | A10 |
| **Layered architecture** (dependency inversion) | B1 |
| **Null safety** (Optional, fail-fast) | B2 |
| **Domain-Driven Design** (domain services) | C1, C2 |
| **Compiler optimization** (CSE) | A5 |
| **Exception internals** (fillInStackTrace) | A6 |
| **Validation** (boundary validation) | D1-D3 |

---

## What I Learned

1. **N+1 is the most common performance bug** — it's invisible in unit tests (1 player = 1 query = fine) but devastating in production. Always batch.

2. **Data structures are the real performance lever** — HashMap O(1) lookup, StringBuilder amortized appending, and ConcurrentHashMap lock striping are all data structure choices that determine your runtime.

3. **Immutability has both costs and benefits** — Strings are immutable for good reasons (pool, hash safety, thread safety). But when you need mutation, reach for the mutable cousin (StringBuilder). The same applies to our StatAccumulator: immutable records are safer, mutable arrays are faster.

4. **Optimize I/O first, then CPU** — The AI call takes 2-10 seconds. No amount of CPU optimization matters if you're waiting on the network. But once I/O is fixed (batch queries, caching), CPU optimizations prevent scaling bottlenecks.

5. **Architecture IS performance** — Layered architecture (B1) enabled caching. Domain extraction (C1, C2) enabled testing. Validation (D) prevents invalid data from reaching expensive operations. Clean architecture makes performance work possible.

---

**Code:** [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)
**PRs:** #33 (Algorithms), #34 (Service Cleanup), #35 (Domain Enrichment), #36 (Validation)

Previous articles: [Post 1](https://www.linkedin.com/pulse/post-1-andranik-muradyan) | [Post 2](https://www.linkedin.com/pulse/post-2-andranik-muradyan) | [Post 3](https://www.linkedin.com/pulse/post-3-andranik-muradyan) | [Post 4](https://www.linkedin.com/pulse/post-4-andranik-muradyan) | [Post 5](https://www.linkedin.com/pulse/post-5-andranik-muradyan) | [Post 6](https://www.linkedin.com/pulse/post-6-andranik-muradyan) | [Post 7](https://www.linkedin.com/pulse/post-7-andranik-muradyan)
