# API Standards — AI Coding Guidelines

This document directs an AI coding assistant designing or changing HTTP APIs for the Support Ticket Management System (Spring Boot).

**Before adding or changing any endpoint, review the project’s API specification** (OpenAPI/Swagger, API docs, or agreed contract). Follow the spec for paths, methods, fields, status codes, and error shapes. If the spec is missing, ambiguous, or conflicts with existing code, **ask**—do not invent endpoints, fields, or semantics.

---

## Source of truth

1. **API specification** (OpenAPI, docs, or written contract) defines the public contract.
2. **Existing controllers, DTOs, and tests** define how this project implements that contract.
3. **Never invent** resources, paths, JSON properties, status codes, or error formats without spec or existing precedent.
4. Preserve **backward compatibility** unless the task is an explicit breaking change with a stated migration.

---

## RESTful design and resource naming

- Model resources as **nouns**, not actions: `tickets`, `users`, `comments`—not `createTicket` or `doAssign`.
- Use **plural** collection names: `/tickets`, `/tickets/{id}/comments`.
- Use **lowercase** path segments; use hyphens for multi-word segments (`/ticket-categories`) unless the project already uses another convention—then match it.
- Keep paths **stable** and shallow. Prefer `/tickets/{id}` over deep nesting; nest only for true ownership (`/tickets/{id}/comments`).
- Use the **same base path** as existing controllers (e.g. `/api/...`). Do not add `/v1` or versioning unless the spec already defines it.
- Actions that do not map to CRUD (e.g. `assign`, `close`) should follow the spec; if unspecified, prefer a **sub-resource or documented action** over new verbs in the path.
- Do not expose internal persistence names (table names, column names) in URLs or JSON.

---

## HTTP methods and status codes

Use methods and codes that match the **spec** and existing endpoints. When the spec is silent, use these defaults:

| Method | Use | Typical success |
| --- | --- | --- |
| GET | Read one or many | 200 |
| POST | Create | 201 (with `Location` if the project does so) or 200 per existing pattern |
| PUT | Full replace | 200 or 204 |
| PATCH | Partial update | 200 or 204 |
| DELETE | Remove | 204 or 200 per existing pattern |

- **Idempotency**: GET/PUT/DELETE must be idempotent; POST is not unless the spec says otherwise.
- **Client errors**: 400 validation/bad input, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict (e.g. duplicate, invalid state), 422 only if the project already uses it.
- **Server errors**: 500 only for unexpected failures; do not map business rules to 500.
- Do not return 200 with an error body for failures. Do not invent new status codes.
- Match existing **success payload** shape (empty body vs resource) for the same operation type.

---

## Request and response DTOs

- Use **dedicated request and response DTOs**; do not bind or return JPA entities (see below).
- Request DTOs: only fields the client is allowed to send for that operation. Do not reuse a “full” DTO for create/update if the spec distinguishes them.
- Response DTOs: only fields the API contract exposes. Do not leak internal IDs, audit internals, or lazy-loaded graphs unless specified.
- JSON naming: follow existing Jackson configuration (typically camelCase). Do not change global naming without a spec change.
- Do not add fields “for future use.” Add fields when the spec or requirement defines them.

---

## Input validation and validation errors

- Validate at the **boundary** with Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) on request DTOs.
- Enforce **domain rules** (status transitions, authorization) in the service layer; validation annotations are for format and requiredness.
- On validation failure, return the **same error format** as the rest of the API (see below) with 400 (or the project’s validation status).
- Do not return raw Bean Validation exception messages or stack traces. Map to field-level errors in the project’s standard shape.
- Reject unknown JSON properties if the project configures `FAIL_ON_UNKNOWN_PROPERTIES`; otherwise match existing behavior—do not change it silently.

---

## Consistent error-response format

- Use **one** error body shape across the API. Extend the existing `@ControllerAdvice` / error DTO if present; do not introduce a second format.
- Include at minimum (as the project already does): HTTP status, a machine-readable code or type if used, a human-readable message, and optionally field errors for validation.
- Do not include stack traces, SQL, internal class names, or secrets in production responses.
- Keep error **codes and messages stable** for clients; change them only with a spec update.

---

## Pagination, searching, and filtering

- For list endpoints, follow the spec. If unspecified and the dataset can grow, prefer **pageable** lists over unbounded arrays.
- Use the project’s existing pagination style (e.g. Spring `Pageable` with `page`, `size`, `sort`) and response wrapper (`Page` or custom). Do not invent a new envelope.
- **Filtering/search**: use query parameters defined in the spec (e.g. `?status=OPEN&assignee=...`). Do not add ad-hoc filters without spec or requirement.
- Document defaults (page size, max size) in config or the spec; do not hardcode magic numbers in controllers.

---

## Path variables, query parameters, and request bodies

- **Path variables**: identify the resource (`/tickets/{id}`). Use for required identity, not optional filters.
- **Query parameters**: optional filters, sorting, pagination. Do not use for required create/update payloads.
- **Request body**: POST/PUT/PATCH payloads as JSON DTOs. Do not mix body and query for the same logical field.
- Validate path and query parameters (`@Min`, `@Pattern`, etc.) when the spec constrains them.
- Do not accept entity IDs in the body for the resource identified in the path unless the spec requires it; prefer path as the source of truth.

---

## Persistence entities out of API responses

- Never serialize JPA entities directly: no lazy proxies, no bidirectional loops, no Hibernate internals in JSON.
- Map entities to **response DTOs** in the service or mapper layer, consistent with existing code.
- Do not expose database-generated fields (internal numeric IDs, audit columns) unless the API contract includes them.

---

## Backward compatibility

- Prefer **additive** changes: new optional fields, new endpoints. Avoid removing fields, renaming JSON properties, or changing types.
- Do not change existing status codes or error shapes for the same scenario without a spec change.
- If a breaking change is required, confirm versioning or migration strategy from the spec before implementing.

---

## Clear, useful error messages

- Messages should say **what is wrong** and, when safe, **how to fix it** (e.g. “status must be one of OPEN, IN_PROGRESS”).
- Validation errors should identify the **field** and constraint.
- Do not expose internal details (table names, constraint names, stack traces) that help attackers or leak implementation.
- Keep tone neutral and consistent with existing messages.

---

## Workflow for the assistant

1. Read the API spec and existing controllers/DTOs for the resource.
2. Match paths, methods, status codes, DTO fields, and error format to the spec and existing patterns.
3. Implement the smallest change that satisfies the requirement.
4. Add or update validation and error mapping consistent with the existing error handler.
5. If the spec is missing or unclear, **ask** before adding endpoints or fields.

Do not create controllers, endpoints, or application code when the task is only to define or review standards.
