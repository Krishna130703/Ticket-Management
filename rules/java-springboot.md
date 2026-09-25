# Java 21 / Spring Boot — AI Coding Guidelines

This document directs an AI coding assistant working on this Support Ticket Management System. Prefer **safe, minimal, maintainable edits to existing code** over generating large amounts of new code.

The backend is a **Java 21** Spring Boot application (Gradle). Treat repository code, existing packages, and established patterns as the source of truth. Do not invent APIs, entities, database fields, or conventions without sufficient context from this project.

---

## Operating principles (always)

1. **Understand existing code before modifying it.** Read the relevant controllers, services, repositories, entities, DTOs, tests, and configuration. Identify how similar features are already implemented.
2. **Follow existing project conventions** unless there is a strong, stated reason to change them (bugs, security, or an explicit request). Match naming, package layout, mapping style, exception types, and logging already used in the codebase.
3. **Make the smallest necessary change** that satisfies the requirement. Prefer extending an existing class or method over adding new layers.
4. **Avoid unnecessary refactoring, new dependencies, or large code generation.** Do not “improve” unrelated code, rename for taste, or introduce libraries when the JDK or existing Spring starters suffice.
5. **Preserve existing behavior** unless the requested change explicitly requires otherwise. Keep public API contracts, status codes, and persistence semantics stable.
6. **Ask for clarification** when requirements, domain rules, or existing patterns are unclear. Do not guess field names, endpoints, or business rules.
7. **Consider validation, error handling, security, transactions, and backward compatibility** on every change that mutates data or exposes an API.
8. **Never invent APIs, entities, database fields, or project conventions** without sufficient context. If the feature is not represented in the repo, ask before creating new resources.

When in doubt: **read more, change less, ask first.**

---

## Java 21 standards and best practices

- Target **Java 21** language level and APIs. Do not use preview features unless the project already enables them.
- Prefer **immutability** where it fits existing code: `final` fields, records for simple DTOs/value objects if that pattern is already used.
- Use **records** for DTOs and value types when the project does so; otherwise match existing DTO style (classes + Lombok, etc.). Do not mix styles in the same layer without a reason.
- Prefer **clear, explicit types** over raw types and unnecessary `Object`.
- Use **pattern matching**, `switch` expressions, and text blocks only when they improve readability of the change you are making—not as drive-by modernization.
- Handle **nullability** consistently with the rest of the project (`Optional` at service boundaries only if already used; do not wrap everything in `Optional`).
- Prefer **checked-in, production-quality code**: no commented-out blocks, no unused imports, no `System.out.println`.
- Use the **standard library and existing Spring APIs** before adding utilities.

```java
// Prefer
if (status instanceof TicketStatus s && s.isTerminal()) { ... }

// Avoid drive-by rewrites of working if/else that is already clear
```

---

## Spring Boot architecture

- Keep a **layered architecture**: HTTP adapters (controllers) → application/domain services → persistence (repositories). Add new layers only if the project already uses them (e.g. mappers, facades).
- Controllers are **thin**: parse input, call one service method, map the result to the HTTP contract.
- Services own **use cases and business rules**.
- Repositories own **data access only**—no business decisions.
- Do not put JPA entities on the public API unless the project already does so. Prefer DTOs for request/response.
- Configuration belongs in **`@Configuration` / properties**, not scattered constants in business classes.
- Respect Spring Boot **auto-configuration**. Do not duplicate beans or disable auto-config without a documented need.
- Match the project’s Spring Boot version and starters; do not assume Spring Boot 2.x APIs.

---

## Controller → Service → Repository separation

| Layer | Allowed | Not allowed |
| --- | --- | --- |
| Controller | HTTP mapping, status codes, `@Valid`, auth principal | Business rules, transactions, JPA, query logic |
| Service | Domain rules, orchestration, transactions, mapping to/from entities | `HttpServletRequest` details, building `ResponseEntity` unless already the pattern |
| Repository | Queries, persistence | Validation of business invariants, HTTP, logging of user PII |

- One controller method should typically call **one service use case**.
- Do not inject `EntityManager` or repositories into controllers if services exist for that aggregate.
- Do not inject other controllers.
- Reuse existing services instead of copying logic into a new class.

---

## DTO usage and mapping

- Use **request DTOs** for inbound payloads and **response DTOs** for outbound payloads. Do not expose persistence internals (lazy collections, Hibernate proxies) in JSON.
- Map in the **service** (or an existing mapper type). Do not map in controllers unless that is already the project pattern.
- Prefer **explicit mapping** (manual or the project’s mapper). Do not add MapStruct, ModelMapper, or similar unless already a dependency.
- Never bind `@RequestBody` directly to a JPA **entity** if DTOs exist or if the entity has relations, generated IDs, or audit fields.
- Keep DTOs **focused**: do not reuse one “god” DTO for create, update, and list unless the project already does.
- Do not invent DTO fields that have no source in the API contract, entity, or requirement.

---

## Entity design and JPA practices

- Entities represent **persistence**, not the HTTP API.
- Use existing ID, auditing, and naming strategies. Do not add `@CreatedDate` / `@Version` / soft-delete unless the project already uses them or the requirement needs them.
- Prefer **`@ManyToOne` / `@OneToMany`** mappings that match the real schema. Avoid bidirectional graphs unless already present and required.
- Set **fetch types** deliberately. Default to lazy collections; do not use `EAGER` to “make JSON work.”
- Avoid **N+1** queries: use existing fetch-join / entity-graph patterns in this repo.
- Do not call `CascadeType.ALL` or `orphanRemoval = true` unless the domain clearly owns the children.
- Equals/hashCode: follow existing entity identity (typically ID-based after persist). Do not use Lombok `@Data` on entities if it would include collections.
- Schema changes: **do not invent columns**. Align with existing entities, migrations, or ask. Do not enable destructive DDL (`ddl-auto=create-drop`) in non-dev configuration.
- Keep entities free of business workflows (no “send email” methods on entities unless that is an established domain-model style here).

---

## Dependency injection

- Prefer **constructor injection**. Do not use field `@Autowired` unless the file already does and you are making a tiny edit.
- Inject **interfaces** (repository, service) as the rest of the project does.
- Keep beans **focused**; do not inject the entire application context.
- Avoid circular dependencies. If a change would create one, stop and redesign the smallest split—or ask.
- Do not use `new` for Spring-managed types (services, repositories, `ObjectMapper` if it is a bean).
- `@RequiredArgsConstructor` (Lombok) is acceptable **only if** the project already uses it for injection.

---

## Exception handling and validation

- Validate at the **boundary** with Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) on DTOs. Do not skip validation on new or changed endpoints.
- Put **domain invariants** in the service (state transitions, authorization-to-act, uniqueness). Throw the same exception types the project already uses.
- Use a **global** `@ControllerAdvice` / `@RestControllerAdvice` if one exists; extend it rather than adding per-controller `try/catch` for mapping errors.
- Map exceptions to **stable HTTP statuses** consistent with existing APIs (e.g. 400 validation, 404 missing, 409 conflict, 403 forbidden). Do not invent a new error JSON shape if one exists.
- Never swallow exceptions. Log at the right level, then rethrow or map.
- Do not return stack traces, SQL, or internal identifiers that are not part of the API contract.
- Validation messages must not leak secrets or raw persistence details.

---

## Transaction management

- Place `@Transactional` on **service** methods that change data, matching existing usage (class vs method level).
- Prefer **read-only** transactions for queries: `@Transactional(readOnly = true)` when the project uses that pattern.
- Keep transactions **short**. Do not call HTTP clients, file I/O, or long loops inside a transaction unless unavoidable and already the pattern.
- Never start transactions in controllers.
- Be explicit about rollback for checked exceptions only if the project already configures `rollbackFor`.
- Do not add nested `@Transactional(propagation = REQUIRES_NEW)` without a clear isolation need.

---

## Naming conventions

Follow names already in `com.ttn.ticket_api` (and neighboring packages). If none exist yet for a new type, use these defaults—**do not rename existing types to match**.

| Kind | Pattern | Example |
| --- | --- | --- |
| REST controller | `{Resource}Controller` | `TicketController` |
| Application service | `{Resource}Service` / `{Resource}ServiceImpl` | Match existing |
| Spring Data repo | `{Entity}Repository` | `TicketRepository` |
| Entity | Singular noun | `Ticket` |
| Request DTO | `{Action}{Resource}Request` or `{Resource}Request` | `CreateTicketRequest` |
| Response DTO | `{Resource}Response` / `{Resource}Dto` | Match existing |
| Exception | `{Problem}Exception` | `TicketNotFoundException` |
| Methods | Verb phrases; repository methods follow Spring Data | `findById`, `assignAgent` |
| Packages | lowercase; no camelCase segments | `com.ttn.ticket_api.ticket` |

- REST paths: follow existing `/api/...` (or whatever is already mapped). Do not invent versioning (`/v1`) unless present.
- JSON property names: match existing Jackson settings (`camelCase` unless configured otherwise).

---

## Package structure

Do not create a new top-level layout if packages already exist. When the codebase is still a skeleton, prefer a **feature-oriented** or **layer-oriented** structure **consistent with the first real feature**—then stick to it.

Typical options (pick the one the repo uses; do not mix):

```text
com.ttn.ticket_api
  {feature}/          # controller, service, repository, dto, entity together
  config/
  exception/
  security/
```

or layered:

```text
com.ttn.ticket_api
  controller/
  service/
  repository/
  domain/             # or entity/
  dto/
  config/
```

- Keep the application class in `com.ttn.ticket_api`.
- Do not move files between layouts as a “cleanup” unless asked.

---

## Logging

- Use **SLF4J** (`Logger` / `@Slf4j` if Lombok is already used that way). Never `System.out` / `System.err`.
- Log **useful context** (ticket id, operation) at INFO for significant state changes; DEBUG for verbose flow; WARN for recoverable issues; ERROR for failures that need attention.
- **Never log secrets**: passwords, tokens, API keys, session IDs, authorization headers, or raw connection strings.
- Minimize **PII** in logs (emails, names) unless the project already logs them and the change is consistent—prefer identifiers.
- Log exceptions with the throwable: `log.error("Failed to update ticket {}", ticketId, ex);`
- Do not log entire entities or request bodies by default.

---

## Configuration and environment-specific settings

- Use `application.properties` / `application.yml` and **profile-specific** files (`application-dev`, `application-prod`) as the project already does.
- Externalize **all** environment-specific values: datasource URL, credentials, ports, feature flags.
- **Never hardcode secrets, credentials, or sensitive configuration** in source, tests, comments, or docs. Use environment variables or a secret manager; reference them from config (`${DB_PASSWORD}`).
- Do not commit `.env` files with real secrets. Do not add placeholder passwords that look real.
- Prefer `spring.config.import` / existing config patterns over new config libraries.
- Keep `@Value` / `@ConfigurationProperties` aligned with existing style. Prefer **`@ConfigurationProperties`** for grouped settings if that pattern exists.
- Do not change `ddl-auto`, logging levels, or Actuator exposure in shared config without being asked.

---

## Spring Data JPA and database access

- Extend the same base type the project uses (`JpaRepository`, `ListCrudRepository`, etc.).
- Prefer **derived query methods** or existing `@Query` style. Do not introduce Querydsl/Criteria unless already used.
- Use **`@Query`** for non-trivial JPQL; keep queries on repositories, not in services as string SQL.
- Prefer **pagination** (`Pageable`) for lists if list endpoints exist or the dataset can grow.
- `save` / `delete` belong in services behind transactions, not in controllers.
- Do not use native SQL unless required (DB-specific feature) and consistent with the repo.
- Do not silently catch `DataIntegrityViolationException`; map it like other persistence failures.
- Avoid `findAll()` for unbounded production tables.

---

## Keep business logic out of controllers

Controllers may:

- Read path/query/body and the authenticated principal
- Apply `@Valid` / `@Validated`
- Call the service
- Return `ResponseEntity` or a DTO (match existing)

Controllers must not:

- Compute ticket state machines, SLA, assignment rules, or permissions beyond “pass the user into the service”
- Run queries or updates
- Build ad-hoc maps as a substitute for DTOs (unless that is already the API)

If logic appears in a controller you are editing, **move only what the task requires** into the service—do not refactor the entire controller unsolicited.

---

## Clean, readable, maintainable code

- Change **one concern per PR/task**. No unrelated formatting sweeps.
- Names should describe behavior. Avoid `data`, `temp`, `process2`.
- Keep methods short; extract a private method only when it clarifies the change.
- Prefer composition over deep inheritance.
- Duplicating three lines once is better than a premature shared abstraction. Abstract when the project already has a pattern.
- Update or add **tests** when behavior or logic changes. Match existing test style (JUnit 5, MockMvc, Data JPA tests). If tests cannot be run, say so.
- Do not generate large “complete module” scaffolds (dozens of files) when a few focused types suffice.

---

## Security best practices

- Never disable CSRF, CORS, or auth **globally** to “make it work.” Follow existing Spring Security configuration. If security is not set up yet, do not invent a full security module unless asked; still avoid insecure defaults in new code.
- Authorize in the **service** (or method security if the project uses `@PreAuthorize`), not only by hiding a button. Check the current user can access the ticket/resource.
- Do not trust client-supplied **user id, role, or tenant** fields for authorization.
- Mass-assignment: do not bind unknown JSON fields onto entities; use dedicated DTOs and ignore or reject extra fields per existing Jackson config.
- Parameterize all queries. Never concatenate user input into JPQL/SQL.
- Encode output as Spring does by default; do not return unvalidated HTML.
- Do not log or return stack traces in production responses.
- Dependencies: do not add libraries with known insecure defaults. Do not introduce `eval`, native command execution, or unrestricted file upload without an explicit requirement and validation.
- CORS: least privilege; no `*` with credentials.

---

## Secrets and sensitive configuration (non-negotiable)

- **Never hardcode** passwords, API keys, tokens, certificates, private keys, or OAuth secrets.
- **Never** put secrets in `application.properties` checked into git as plaintext values. Use env vars or a vault.
- **Never** print secrets in logs, error messages, OpenAPI examples, or README snippets.
- If a secret is found in the repo during a task: **do not copy it into new files**. Flag it and recommend rotation and secret storage. Do not generate replacement secret values.

---

## Dependencies

- Do not add Gradle/Maven dependencies unless **necessary** to meet the request and **not** already covered by Java 21 or existing starters (Web, Validation, Data JPA, etc.).
- If a dependency is required: justify it, prefer maintained libraries, pin versions via the existing BOM / Spring dependency management, and update the lockfile/wrapper only as the project already does.
- Lombok is already in use—follow existing Lombok annotations; do not fight them or expand `@Data` onto entities.

---

## Tests and backward compatibility

- Preserve existing endpoint paths, DTO JSON field names, and status codes unless the task is an intentional breaking change.
- When adding fields, prefer **additive, optional** JSON properties.
- Database: additive migrations over destructive ones; ask if a breaking schema change is required.
- Add or update tests for changed behavior. Do not delete tests to make a change pass.

---

## Workflow checklist before editing

1. Locate similar features and copy their structure.
2. Confirm entities, tables, and APIs involved actually exist (or were requested).
3. Plan the smallest set of files to touch.
4. Implement with validation, errors, transactions, and security in mind.
5. Avoid new dependencies and refactors.
6. If anything is ambiguous—**ask** instead of inventing.
