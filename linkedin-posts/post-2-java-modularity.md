# TEASER POST (copy-paste into LinkedIn post composer)

I just tried to add Java 9 modularity to my Spring Boot app.

It broke the build in three increasingly painful rounds.

Round 1 → jjwt has no module descriptor
Round 2 → Dozens of transitive deps lack module info
Round 3 → Lombok, Servlet API splits, and Spring AI all collapse

Here's the thing most guides won't tell you — JPMS adoption isn't all-or-nothing. There are three levels:

↳ Level 1: Automatic-Module-Name (one line in your manifest)
↳ Level 2: Full module-info.java (strict compile-time enforcement)
↳ Level 3: Strong encapsulation (minimum API surface)

Most Spring Boot apps today should sit at Level 1. That's not a compromise — it's pragmatic engineering.

I wrote up the full breakdown: what failed, why, and the three-level strategy every Java developer should know.

📎 Full article in the comments 👇

The code is open source: github.com/ando7555/ai-coach

#Java #SpringBoot #JPMS #Modularity #SoftwareEngineering #LearningInPublic

---
---

# FULL LINKEDIN ARTICLE (paste into LinkedIn Article editor)

**Title:** Java 9 Modules — Why Most Spring Boot Apps Can't Fully Use Them (And What To Do Instead)

---

I just added Java 9 modularity to my AI Coach app. It broke the build immediately.

Here's what happened, what I learned, and the three-level strategy every Java developer should know.

---

## What Is Java 9 Modularity?

Before Java 9, every public class was visible to every other class on the classpath. Your internal helper classes? Public to the world. Your carefully designed API? Impossible to enforce.

Java 9 introduced the Java Platform Module System (JPMS). It adds a new file — `module-info.java` — that lets you declare three things:

- What you **depend on** (`requires`)
- What you **expose** to others (`exports`)
- What you **allow reflection** on (`opens`)

Think of it like a security gate for your packages. Before JPMS, every door was unlocked. Now you choose which doors to open, and to whom.

---

## The Three Directives Explained

**`requires`** — "I need this module to compile and run."

```java
requires spring.boot;
requires static lombok;  // compile-time only
```

This replaces the vague classpath with explicit dependency declarations. If you forget one, the compiler catches it — not a runtime `ClassNotFoundException` at 2 AM.

**`exports`** — "Other modules can use classes in this package."

```java
exports com.ai.coach.service;
exports com.ai.coach.domain.entity;
```

Even if a class is `public`, it's invisible to other modules unless you export its package. This is encapsulation beyond the class level — at the package level.

**`opens`** — "This package allows reflective access."

```java
opens com.ai.coach.domain.entity to spring.data.neo4j;
opens com.ai.coach.config to spring.core, spring.beans;
```

Spring creates beans via reflection. Jackson deserialises JSON via reflection. Neo4j maps nodes via reflection. Without `opens`, none of that works in module mode. You can scope it to specific modules — "only Spring can reflect into my config package."

---

## Why It Broke My Build — Three Rounds of Failure

I wrote a complete `module-info.java` for my AI Coach app. 17 `requires` directives. 10 `exports`. 10 `opens`. Hit build.

What followed was three rounds of increasingly deeper failures.

### Round 1 — jjwt has no module descriptor

```
error: module not found: io.jsonwebtoken.jjwt.api
```

The jjwt library doesn't ship a JPMS module descriptor. When the compiler sees `module-info.java`, it switches to strict module mode and demands every dependency be a proper module.

I tried to fix this. Using `jar --describe-module`, I discovered jjwt's automatic module name is `jjwt.api` (derived from the JAR filename), not `io.jsonwebtoken.jjwt.api`.

Fixed the `requires` directive. Still failed — Gradle puts non-modular JARs on the classpath, and named modules can't read the classpath.

### Round 2 — The Gradle plugin approach

I added the `org.gradlex.extra-java-module-info` plugin to force jjwt onto the module-path. That fixed jjwt — but opened a flood of new errors. Dozens of transitive dependencies (Spring AI, antlr, jtokkit) also lacked module descriptors.

Set `failOnMissingModuleInfo` to false to let non-modular JARs through. Build got further — then hit the real walls.

### Round 3 — The real blockers revealed

With jjwt resolved, three deeper problems surfaced:

**Lombok breaks completely** — In module mode, Lombok's annotation processor can't generate code. Every `@Builder`, `@Getter`, and `@Slf4j` annotation stopped working. `builder()`, `getUsername()`, `getPassword()`, `log` — all "cannot find symbol." This alone is a dealbreaker for most Spring Boot projects.

**jakarta.servlet is split across modules** — The servlet API lives in `org.apache.tomcat.embed.core`, but Spring Security's module also claims some of the same packages. The compiler sees conflicting exports and refuses to compile our `JwtAuthenticationFilter`.

**Spring AI has zero module descriptors** — Every Spring AI package — chat client, model, commons, autoconfigure — lands in the unnamed module. A named module can't read the unnamed module. Our entire `AiClient` class becomes unreachable.

The lesson: it's not one library blocking JPMS adoption. It's the entire Spring Boot ecosystem. Lombok, Servlet APIs, and newer libraries like Spring AI all lack proper module support.

---

## The Three Levels of JPMS Adoption

Here's what most guides don't tell you — modularity isn't all-or-nothing. There are three levels:

**Level 1: Automatic-Module-Name** — Add one line to your JAR manifest. Your library works on both the classpath and the module-path. No compilation changes. This is what I shipped in production.

**Level 2: Full module-info.java** — Compile-time enforcement of all dependencies, exports, and opens. Requires every dependency to have a module descriptor. I attempted this through three rounds of fixes and documented exactly why it fails today.

**Level 3: Strong encapsulation** — Only export the minimum API surface. Internal packages stay hidden even from reflection. This is the end goal for the Java ecosystem.

Most Spring Boot applications today sit at Level 1. That's not a failure — it's pragmatic engineering. I know because I tried Level 2 and hit three walls that no amount of Gradle configuration can fix.

---

## What Is package-info.java?

While working on modularity, I also added `package-info.java` files to all 11 packages. This is a lesser-known Java feature that serves two purposes:

**Documentation** — It's the only way to add Javadoc to a package. When you generate Javadoc, each package gets a summary page pulled from this file.

**Annotation host** — You can place package-level annotations here. For example, Spring's `@NonNullApi` on a `package-info.java` makes every method parameter in that package non-null by default — no need to annotate each one individually.

It's a small file, but it forces you to think about what each package does and why it exists. That clarity pays off as the codebase grows.

---

## What Is Automatic-Module-Name?

The one line I added to `build.gradle`:

```groovy
jar {
    manifest {
        attributes('Automatic-Module-Name': 'com.ai.coach')
    }
}
```

This tells the JVM: "When someone uses my JAR on the module-path, call it `com.ai.coach`."

Without it, the JVM derives a module name from the JAR filename — which is fragile and unpredictable. With it, you have a stable module identity that won't break when filenames change.

Major libraries like Guava and Jackson use this same approach. It's the safe first step.

---

## The Practical Takeaway

If you're building a Spring Boot app today:

1. **Add Automatic-Module-Name** to your JAR manifest. It costs nothing and future-proofs your library.

2. **Write package-info.java** for every package. It improves Javadoc and forces you to articulate your package structure.

3. **Write module-info.java as a reference file.** Even if it can't compile yet, it documents your real dependencies and is ready to activate when the ecosystem catches up.

4. **Don't force full JPMS on a Spring Boot app** with non-modular dependencies. I tried three rounds of fixes — correcting automatic module names, adding the extra-java-module-info Gradle plugin, configuring failOnMissingModuleInfo. Each fix revealed a deeper problem.

The value of this exercise isn't the compiled `module-info.java`. It's understanding exactly why the ecosystem isn't ready — and being prepared for when it is.

---

The code is open source: [github.com/ando7555/ai-coach](https://github.com/ando7555/ai-coach)

#Java #SpringBoot #JPMS #Modularity #SoftwareEngineering #LearningInPublic