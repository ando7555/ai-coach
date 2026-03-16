## What Is This About?

This issue adds **package-level documentation** (`package-info.java`) to every package, a **module descriptor** (`module-info.java`) to define the module structure, and sets an **Automatic-Module-Name** in the JAR manifest — bringing Java 9+ modularity concepts into our codebase.

---

## Knowledge Transfer: Java 9 Module System (JPMS)

### The Problem JPMS Solves

Before Java 9, Java had one big flat **classpath**. Every JAR you added was dumped into a single namespace. This caused:

- **Classpath hell** — two JARs with the same class name? Runtime crash. No way to know until it blows up.
- **No encapsulation** — any class marked `public` was accessible to the entire world. Libraries couldn't hide their internal implementation classes.
- **No explicit dependencies** — you couldn't declare "this module needs that module". The JVM loaded everything and hoped for the best.

### What JPMS Introduced (Java 9)

Java 9 introduced the **Java Platform Module System** — a way to organize code into self-contained modules:

```java
// module-info.java — placed at the root of your source tree
module com.ai.coach {
    requires spring.boot;              // declares dependency
    requires spring.data.neo4j;
    exports com.ai.coach.controller;   // only these packages are visible outside
    exports com.ai.coach.domain.entity;
    // internal packages stay hidden!
}
```

**Key concepts:**

| Concept | What it does |
|---|---|
| `module-info.java` | Declares a module — its name, dependencies (`requires`), and what it exposes (`exports`) |
| `requires` | "I need this other module to compile and run" |
| `exports` | "Other modules can see classes in this package" |
| `opens` | "This package is available for reflection" (needed by frameworks like Spring) |
| Strong encapsulation | Packages NOT exported are invisible — even if classes are `public` |

---

## Our module-info.java

Here's the module descriptor for AI Coach. It maps our entire dependency graph:

```java
module com.ai.coach {

    // -- Java Platform Modules --
    requires java.base;                          // always implicit, but listed for clarity

    // -- Spring Boot & Core --
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.web;
    requires spring.webflux;

    // -- Spring Security --
    requires spring.security.config;
    requires spring.security.core;
    requires spring.security.web;

    // -- Spring Data & Neo4j --
    requires spring.data.commons;
    requires spring.data.neo4j;

    // -- Spring GraphQL --
    requires spring.graphql;

    // -- Spring AI --
    requires spring.ai.core;
    requires spring.ai.openai;

    // -- Jakarta APIs --
    requires jakarta.servlet;
    requires jakarta.validation;

    // -- JWT (JJWT) --
    requires io.jsonwebtoken.jjwt.api;

    // -- Reactive --
    requires reactor.core;

    // -- Jackson JSON --
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    // -- Logging --
    requires org.slf4j;

    // -- Lombok (compile-time only, but module system still needs it) --
    requires static lombok;

    // -- Exports: what other modules can see --
    exports com.ai.coach;
    exports com.ai.coach.domain.entity;
    exports com.ai.coach.domain.dto;
    exports com.ai.coach.domain.repository;
    exports com.ai.coach.service.dto;

    // -- Opens: packages Spring needs to reflect into --
    // Spring Boot does classpath scanning, dependency injection,
    // and proxying — all via reflection. Without 'opens', Spring
    // can't instantiate beans or read annotations at runtime.
    opens com.ai.coach to spring.core, spring.context, spring.beans;
    opens com.ai.coach.config to spring.core, spring.context, spring.beans;
    opens com.ai.coach.controller to spring.core, spring.context, spring.graphql;
    opens com.ai.coach.service to spring.core, spring.context, spring.beans;
    opens com.ai.coach.security to spring.core, spring.context, spring.beans, spring.web;
    opens com.ai.coach.exception to spring.core, spring.context, spring.graphql;
    opens com.ai.coach.domain.entity to spring.core, spring.data.neo4j, com.fasterxml.jackson.databind;
    opens com.ai.coach.domain.dto to spring.core, spring.graphql, com.fasterxml.jackson.databind;
    opens com.ai.coach.domain.repository to spring.core, spring.data.neo4j, spring.data.commons;
    opens com.ai.coach.service.dto to spring.core, com.fasterxml.jackson.databind;
}
```

### Reading the module-info.java

**`requires`** = "I depend on this module"
- Each Spring starter, each library we use needs its own `requires` line
- `requires static lombok` — the `static` keyword means "compile-time only, not needed at runtime" (Lombok generates code at compile time then disappears)

**`exports`** = "These packages are my public API"
- Only 5 packages are exported — the domain model and DTOs that external code might reference
- Internal packages (`config`, `security`, `controller`, `service`, `exception`) are **hidden** — this is real encapsulation

**`opens ... to`** = "These frameworks can use reflection on this package"
- Spring needs reflection for: bean instantiation, `@Autowired` injection, `@QueryMapping` annotation processing, Neo4j entity mapping
- We specify exactly WHICH modules can reflect into each package — principle of least privilege
- Without `opens`, Spring Boot would crash at startup with `InaccessibleObjectException`

### Why This Is Hard With Spring Boot (Today)

Even with all the correct `requires`, `exports`, and `opens`:
1. **Automatic modules** — most Spring JARs don't have their own `module-info.java`. The JVM derives module names from filenames, which can be unstable
2. **Transitive dependencies** — Spring pulls in dozens of transitive JARs, each needing its own module name resolution
3. **Annotation processors** — Lombok and Spring's annotation processing interact poorly with module boundaries
4. **Split packages** — some libraries put classes in the same package across different JARs (forbidden by JPMS)

We include `module-info.java` in the project as a **documentation artifact** showing the complete module structure. If it causes build issues, we'll keep it as a reference with a note about Spring Boot compatibility.

---

## package-info.java Files

A `package-info.java` is a special Java file that provides **package-level documentation**. It's been around since Java 5 but is often overlooked.

**What it looks like:**
```java
/**
 * Security infrastructure for the AI Coach application.
 *
 * <p>Implements stateless JWT-based authentication using HMAC-SHA signed tokens.
 * The filter chain extracts Bearer tokens, validates signatures, and populates
 * the Spring SecurityContext.</p>
 */
package com.ai.coach.security;
```

**Why it matters:**
- **Self-documenting architecture** — each package explains its role in the system
- **Javadoc generation** — `package-info.java` content appears as package descriptions in generated docs
- **Package-level annotations** — you can apply annotations to an entire package (e.g., `@NonNullApi` to enforce null safety)
- **Professional practice** — well-maintained open source projects and enterprise codebases use these

---

## Automatic-Module-Name

This is the **safe middle ground** between no modularity and full JPMS:

```gradle
jar {
    manifest {
        attributes 'Automatic-Module-Name': 'com.ai.coach'
    }
}
```

**What it does:**
- Gives your JAR a **stable module name** that other modules can `requires`
- Forward-looking step — when Spring fully supports JPMS, this name becomes the real module name
- **Zero risk** — doesn't enable any module enforcement, just reserves the name

---

## Implementation Plan

### Files to Create:

| # | File | Purpose |
|---|---|---|
| 1 | `src/main/java/module-info.java` | Module descriptor — documents the full module structure |
| 2 | `src/main/java/com/ai/coach/package-info.java` | Root package docs |
| 3 | `src/main/java/com/ai/coach/config/package-info.java` | Config layer docs |
| 4 | `src/main/java/com/ai/coach/controller/package-info.java` | Controller layer docs |
| 5 | `src/main/java/com/ai/coach/domain/package-info.java` | Domain parent docs |
| 6 | `src/main/java/com/ai/coach/domain/dto/package-info.java` | DTO package docs |
| 7 | `src/main/java/com/ai/coach/domain/entity/package-info.java` | Entity package docs |
| 8 | `src/main/java/com/ai/coach/domain/repository/package-info.java` | Repository package docs |
| 9 | `src/main/java/com/ai/coach/exception/package-info.java` | Exception handling docs |
| 10 | `src/main/java/com/ai/coach/security/package-info.java` | Security layer docs |
| 11 | `src/main/java/com/ai/coach/service/package-info.java` | Service layer docs |
| 12 | `src/main/java/com/ai/coach/service/dto/package-info.java` | AI response DTO docs |

### Files to Modify:
- `build.gradle` — add `Automatic-Module-Name` to JAR manifest

### Key Takeaway

| Approach | Status | Why |
|---|---|---|
| `module-info.java` | Added (best-effort) | Documents module structure; may need adjustment as Spring evolves |
| `package-info.java` | Added (11 files) | Self-documenting packages with Javadoc |
| `Automatic-Module-Name` | Added | Reserves stable module name in JAR manifest |
