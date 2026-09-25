# Testing — AI Coding Guidelines

This document directs an AI coding assistant writing or changing tests for the Support Ticket Management System (Java 21, Spring Boot, JUnit 5, Gradle).

Prefer **behavior-focused, independent tests** that protect real requirements. Do not chase coverage percentages, generate large test suites, or invent domain rules.

---

## Source of truth

1. **Project specification** (accepted requirements, domain rules, ticket lifecycle) is the primary source of expected behavior.
2. **Existing application code and tests** are the source of APIs, types, status names, HTTP contracts, and test style.
3. If spec and code disagree, **ask**—do not “fix” either side in tests by guessing.
4. **Never invent** endpoints, entities, status values, transition tables, error codes, or fixtures that are not in the spec or codebase.
5. When adding tests for a change, cover **that behavior**. Do not rewrite unrelated tests or add speculative cases.

Read existing tests first. Match package layout, annotations, assertion style, and naming already used in `Backend/src/test`.

---

## What to optimize for

- Meaningful coverage: **rules that can break** (status transitions, validation, authorization, persistence, API contracts).
- Not: line-coverage quotas, testing getters, testing the framework, or asserting mock call order with no business meaning.
- Smallest useful test set. One clear behavior per test. Ask when the allowed transition matrix is incomplete.

---

## JUnit 5 and test structure

- Use **JUnit 5** (`org.junit.jupiter`). Do not add JUnit 4, TestNG, or extra assertion libraries unless already in the project.
- Use the test starters already on the classpath (`spring-boot-starter-*-test`). Do not add Mockito, AssertJ, or MockMvc separately if they come transitively—use what the project already uses.
- Place tests next to production packages under `src/test/java` (e.g. `com.ttn.ticket_api...`). Name classes `{ClassUnderTest}Test` or `{Feature}IT` for heavier Spring tests—**follow existing names**.
- Prefer `@Nested` + `@DisplayName` only if the class is large (e.g. many transitions); otherwise keep a flat class.
- Use `@BeforeEach` for per-test setup. Do not share mutable state via `static` fields or `@BeforeAll` unless the resource is immutable.
- `@ParameterizedTest` is preferred for **transition matrices** and validation permutations instead of copy-pasted tests.
- Do not use `@SpringBootTest` for logic that is a pure unit/service test. Reserve the full context for wiring and true integration.

---

## Test types (what belongs where)

| Type | Purpose | Typical tools | Avoid |
| --- | --- | --- | --- |
| **Unit** | Pure domain rules, mappers, validators, status guards with no I/O | JUnit, optionally Mockito | Loading Spring |
| **Service** | Use cases, orchestration, transactions as logic, domain exceptions | JUnit + mocks **or** `@DataJpaTest`/`@SpringBootTest` if the project tests services with persistence | Re-testing controller JSON in every service test |
| **Controller / API** | HTTP mapping, status codes, validation errors, auth on the web layer | `@WebMvcTest` + MockMvc (or the project’s equivalent) | Business rules that belong in service tests |
| **Repository** | Queries, mapping to schema, constraints | `@DataJpaTest` (or existing slice) + test DB | Mocking `JpaRepository` to “test” derived method names |
| **Integration** | Cross-layer flows: persist + service + (optionally) HTTP | `@SpringBootTest`, MockMvc/`TestRestTemplate` as already used, real test DB (H2 if that is the project test DB) | Duplicating every unit case as a full-stack test |

Put **ticket status transition rules** in service/domain tests first. Use **integration tests** to prove valid and invalid transitions persist and (if applicable) return the correct HTTP outcome. Do not test the same assertion at every layer without a reason.

---

## Mockito: when to use and when to avoid

**Use mocks** for:

- Collaborators that are slow, external, or orthogonal (mail, clocks if not already injectable, HTTP clients).
- Controller tests: mock the **service**, not the repository, unless the controller talks to the repository (it should not).
- Service tests that must not hit the database **and** the project already tests that service with mocks.

**Do not mock**:

- The class under test.
- JPA entities, DTOs, or value objects.
- `EntityManager` / Spring Data repositories when the goal is to verify queries or constraints—use a slice test.
- Types you do not own if a fakes/stub in-memory implementation is simpler.
- Everything in an integration test; that defeats the test.

Do not verify `times(1)` on every interaction. Verify **outcomes** (returned DTO, persisted status, thrown exception). Use `verify` only when the side effect *is* the requirement (e.g. notification sent).

Do not add mockito-inline / extra mock config unless required and already used.

---

## Naming conventions

Follow existing test names. If none exist, use:

```text
methodOrUseCase_condition_expectedResult
```

Examples:

- `assignTicket_whenOpen_setsStatusInProgress`
- `changeStatus_whenClosedToOpen_throwsInvalidTransition`
- `createTicket_whenTitleBlank_returns400`

`@DisplayName` may restate the spec in prose; the method name must still be unique and searchable. Do not name tests `test1`, `shouldWork`, or `happyPath` without the scenario.

---

## Arrange–Act–Assert

Every test has three visible parts:

1. **Arrange** — fixture, persisted ticket, mocks, request body.
2. **Act** — a single behavior (one service call or one HTTP request).
3. **Assert** — observable result: return value, persistence, HTTP status/body, exception type.

Do not mix multiple acts (create + update + delete) unless the spec requires a **workflow** (then name it as a workflow and keep asserts tied to that story). Prefer separate tests over one long script.

Keep arrange data **minimal**. Do not populate unused fields.

---

## Positive, negative, edge, validation, and exceptions

Cover, when the spec or code defines them:

- **Positive** — allowed operations and successful responses.
- **Negative** — disallowed operations, missing resources, wrong actor.
- **Edge** — empty collections, boundaries on length/size, last allowed transition, concurrent-uniqueness if specified.
- **Validation** — Bean Validation on DTOs (blank title, invalid enum, missing required fields). Assert **status and error contract** already used by the API, not only “not 200”.
- **Exceptions** — domain exceptions (not found, conflict, invalid transition). Assert type and, if stable, message/code the API already exposes.

Do not test framework behavior (`@NotNull` on a field with no corresponding API). Do not catch exceptions in tests to make them pass; use `assertThrows` (or MockMvc expected status).

---

## State machine and ticket status transitions

Ticket lifecycle is **core domain**. Tests must follow the **specified** transition table, not an assumed Jira/ServiceNow model.

- Encode **allowed** transitions: from-status + action/role → to-status (and any required fields, assignee, comments).
- Encode **forbidden** transitions: same from-status + illegal target → specified error (no silent ignore).
- Cover **terminal** states (no further changes) if the spec defines them.
- Cover **identity**: same-status no-ops only if the spec allows them.
- Cover **role/permission** if transitions depend on actor.
- Prefer a **parameterized matrix** (from, to, allowed) derived from the spec so missing rows are obvious.

Do not invent statuses (`OPEN`, `IN_PROGRESS`, …) in guidelines-as-code. Use names from the spec/enum in the repo.

When the transition table is unspecified, **ask** before writing a large matrix.

---

## Integration tests: valid and invalid status transitions

Integration tests prove the rule still holds **through persistence and, where relevant, HTTP**.

**Valid transitions**

- Start from a ticket in a known persisted status (arrange via API or repository, matching project style).
- Perform the documented action.
- Assert persisted status (and other specified fields) after commit.
- If testing API: assert HTTP success and response body status field.

**Invalid transitions**

- Arrange a ticket in a status that must not accept the action.
- Perform the illegal transition.
- Assert **failure**: domain exception and/or HTTP error (400/409/403 as specified—do not guess).
- Assert **status unchanged** in the database (no partial update).

Keep these tests independent: each test creates its own ticket (or uses transactional rollback / `@Transactional` on tests if that is the project pattern). Do not rely on test execution order.

Do not spin a full `@SpringBootTest` for every matrix row if service-level tests already lock the matrix; integration-test **representative** valid and invalid paths plus any mapping/persistence risk (enum columns, optimistic lock). If the spec calls out integration coverage for all transitions, follow the spec.

---

## Independence and isolation

- Tests must pass **alone** and in **any order**.
- No shared mutable tickets, static counters, or “run after `createTicket`”.
- Isolate time, locale, and randomness if behavior depends on them (inject clock if the app has one; do not `Thread.sleep` to wait for status).
- Use the **test database** (H2 in this project’s stack unless changed). Do not point tests at shared dev/prod data.
- Clean up via transaction rollback, `@DirtiesContext` only when unavoidable, or explicit delete—match existing tests. Do not leave leaked rows that affect later tests.
- Do not depend on insertion order or generated ID values beyond “id was assigned”.

---

## Avoid redundant, brittle, or meaningless tests

**Skip**

- Tests that only assert a mock was called with `any()`.
- Tests of Lombok getters, constructors with no logic, or Spring starting (`contextLoads`) unless you are changing configuration and need a smoke test.
- Duplicating the same transition assert in unit, service, controller, and integration layers.
- Snapshot-style JSON of entire payloads when only `status` matters—assert the fields the spec cares about.
- Brittle tests tied to exact log lines, timestamp strings, or collection iteration order unless specified.
- `Thread.sleep`, real network, or full UI.

**Prefer**

- One test per specified rule.
- Parameterized matrices over clones.
- Asserting business-visible state.

If a test can pass while the feature is wrong, delete or rewrite it—do not add more of the same.

---

## Workflow for the assistant

1. Read the spec section and the production code for the behavior.
2. Read existing tests for style and to avoid duplication.
3. Add the **smallest set** of tests that would fail if the rule broke.
4. Include positive, negative, and (for tickets) **allowed vs rejected transitions**, with integration coverage for both as required above.
5. Do not implement product features in order to make a test compile; if production types are missing, ask or wait for the implementation task.
6. Do not add test dependencies or custom runners without need.

When requirements or existing patterns are unclear, **ask** instead of generating a speculative suite.
