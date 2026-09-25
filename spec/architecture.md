# Support Ticket Management System — Architecture

This document is an **architecture specification** only. It does not implement the application.

**Functional requirements** are defined in `spec/requirements.md` (source of truth). This document describes **how** those requirements are supported technically. It does not add features.

Where `spec/requirements.md` is silent, a **minimal architectural assumption** is stated explicitly. Assumptions do not expand product scope.

---

## Functional requirements vs architectural decisions

| Kind | Meaning |
| --- | --- |
| **Requirement** | Behavior the system must provide (create, list, view, update, comments, search, filter, persist, validate, show errors, status machine). |
| **Architectural decision** | Layering, REST, JPA, DTO boundaries, where rules live, error shape, test placement. |
| **Minimal assumption** | A choice required to design the system when the requirements do not specify it. |

Out of scope (not in `spec/requirements.md` and not introduced here): authentication, authorization, roles, notifications, messaging, microservices, caching, queues, and other infrastructure not required to persist tickets and serve the REST/UI flows above.

---

## 1. System Overview

Three parts:

| Part | Responsibility |
| --- | --- |
| **Frontend** (React, Next.js, or equivalent) | Screens for creating, listing, viewing, updating, commenting on, searching, and filtering tickets. Sends HTTP requests to the REST API. Displays loading, success, and meaningful backend errors. Does **not** own business rules or status-transition enforcement. |
| **Backend** (Java 21, Spring Boot, REST) | HTTP API, input validation, ticket use cases, **authoritative** status-transition rules, persistence orchestration, consistent error responses. |
| **Database** (PostgreSQL or H2) | Durable storage of ticket data (and comments as part of ticket data) so data survives application restarts. |

**Communication:** The frontend calls the backend over HTTP (JSON REST). The backend accesses the database through Spring Data JPA repositories. The frontend never talks to the database.

**High-level request/response flow:**

1. The user acts in the UI (submit form, open list, change status, add comment, search, filter).
2. The frontend sends a REST request with a request DTO (or query parameters).
3. The controller binds and triggers **backend validation**, then calls a service.
4. The service applies use-case logic, including **status-transition rules** when status changes.
5. The repository reads or writes entities; the database commits durable state.
6. The service maps entities to response DTOs.
7. The controller returns HTTP success or a structured error.
8. The frontend renders the result or a meaningful error from the backend payload.

---

## 2. Backend Architecture

Straightforward **layered** Spring Boot application. No extra services or message buses.

| Layer | Owns | Must not own |
| --- | --- | --- |
| **Controllers** | HTTP mapping, status codes, invoking validation on request DTOs, calling one service use case per action, returning DTOs or error mapping already handled globally. | Persistence, status-transition tables, query construction, UI concerns. |
| **Services** | Use cases: create, list, get, update fields (title, description, priority, assignee), add comment, search by keyword, filter by status, **apply status transitions**. Mapping between DTOs and entities. Transaction boundary for writes. | HTTP types, SQL/JPQL details beyond calling repositories. |
| **Repositories** | Data access: save, find by id, list, keyword search, filter by status. | Validation of input, transition legality, HTTP. |
| **Entities / models** | Persistent shape of a ticket and of comments belonging to a ticket. Status stored as the ticket’s current status. | REST JSON contracts, transition policy (policy lives in a dedicated rule owned by the service layer). |
| **Request/response DTOs** | API contract: inbound create/update/comment/search/filter payloads and outbound ticket/detail/list/error bodies. | JPA relations, Hibernate proxies. |
| **Validation** | Bean Validation on request DTOs (requiredness/format as implemented for backend-enforced input). Service-layer checks for domain rules (especially illegal status transitions). | Trusting the client to have already validated. |
| **Exception handling** | Global handler maps validation failures, missing tickets, invalid transitions, malformed requests, and unexpected errors to a **consistent REST error body**. | Per-controller ad-hoc error JSON shapes. |
| **Business rules / state transitions** | A single backend component (used only by the service) that encodes the required graph: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, `OPEN → CANCELLED`, `IN_PROGRESS → CANCELLED`. Accept allowed changes; reject all others. | Duplicating that graph in controllers or the frontend as enforcement. |

**Coupling:** Controllers depend on services (interfaces if the project uses them). Services depend on repositories and the transition rule. Repositories depend on entities. DTOs are independent of persistence. Entities are not returned from controllers.

**Minimal assumption (create payload):** Requirements list title, description, priority, and assignee as updatable fields, plus status and comments. Create must produce a ticket that can be listed, viewed, and moved from `OPEN`. Architecture assumes a create request carries the fields needed to persist a ticket and that **new tickets start in `OPEN`** so the stated state machine has a starting status. Keyword search is assumed to apply to persisted ticket text used in listing/detail (title and description); the requirements do not name columns.

**Minimal assumption (status vs field update):** Updating title, description, priority, and assignee is a field-update use case. Changing status is a **separate** service operation that always runs the state machine. Mixing an arbitrary status into a generic field update without the transition check is not allowed.

---

## 3. Frontend Architecture

The UI exists to satisfy: create from UI; list and view; update fields; change assignee; add comments; keyword search; status filter; show meaningful backend errors.

**Pages (or equivalent routes):**

| Page | Supports |
| --- | --- |
| Create ticket | Create from the UI; submit to REST; show validation/backend errors. |
| Ticket list | List; keyword search; filter by status; navigate to details. |
| Ticket detail | View details; update title, description, priority, assignee; add comments; request status change; show errors. |

**Reusable components (illustrative, not extra features):** ticket form fields, list/table or list items, comment list and comment form, status control, search input, status filter control, loading indicator, error display bound to the API error body.

**API communication:** A small HTTP client (fetch or the stack’s equivalent) calls the backend REST API. No direct database access. Responses are JSON DTOs; errors are the shared error structure.

**State management:** Component-local or page-level state is sufficient (form fields, list results, selected ticket, loading/error flags). No global store, cache layer, or real-time channel is required.

**Form handling and validation:** The UI may check empty fields for usability. **Backend validation is authoritative** and always enforced. On submit, the UI sends the request and displays backend field/message errors when validation or transitions fail.

**Loading, success, and error states:** Each mutating or fetching action has a loading indication, a success path (updated list/detail), and an error path that renders the backend’s meaningful message (and field errors when present). The UI does not invent a success state when the API returned an error.

The frontend **must not** be the source of truth for status transitions. It may hide or disable illegal options for usability; the backend still rejects invalid transitions.

---

## 4. Database Architecture

**Persistence strategy:** A single relational database, **PostgreSQL or H2**, accessed via Spring Data JPA. Schema is created/maintained in the usual Spring Boot way for this assignment (no extra database products, replicas, or search engines). Data must survive process restart; H2 must be file-based if used as the durable store, or PostgreSQL used as the durable store. In-memory-only storage does not meet persistence-across-restarts.

**Backend-to-database communication:** Only the repository layer talks to the database (JPA). Services never open JDBC connections ad hoc. Controllers never inject `EntityManager` or repositories if a service exists for that use case.

**Repository / data-access approach:** Spring Data repositories for tickets (and comments as needed). Derived or query methods support list, find by id, filter by status, and keyword search. Writes go through `save` inside service transactions.

**Domain models vs database entities:** JPA entities are the persistence model. API DTOs are the external model. The service maps between them. Do not expose entities as JSON. Do not introduce a second domain-model stack unless the codebase already has one.

**Relationships (minimal):** A ticket is the aggregate root. Comments belong to a ticket (required to “add comments” and persist them with ticket data). Status is a field on the ticket, not a separate product feature.

No caching, connection-routing, or extra infrastructure.

---

## 5. Package Structure

Existing application base package: `com.ttn.ticket_api`.

```text
com.ttn.ticket_api
├── TicketApiApplication.java    # Spring Boot entrypoint
├── ticket
│   ├── TicketController         # REST: create, list, get, update fields, search, filter, status, comments
│   ├── TicketService            # Use cases and orchestration
│   ├── TicketRepository         # Persistence
│   ├── Ticket                   # JPA entity
│   ├── Comment                  # JPA entity (belongs to ticket)
│   ├── TicketStatus             # Status values used by the state machine
│   ├── dto                      # Request and response DTOs
│   └── TicketStatusTransition   # Transition rules used by the service
├── exception
│   ├── GlobalExceptionHandler   # Maps exceptions to REST error body
│   └── ...                      # Domain exceptions (not found, invalid transition, …)
└── config                       # Non-secret application configuration only
```

| Package | Purpose |
| --- | --- |
| `com.ttn.ticket_api` | Application bootstrap. |
| `ticket` | Ticket feature: HTTP, services, persistence, status enum, transition rules. |
| `ticket.dto` | API request/response types; not entities. |
| `exception` | API-wide errors and handler. |
| `config` | Spring configuration; no secrets in source. |

Tests mirror this layout under `src/test/java`. Frontend lives in a separate frontend app/directory; it is not a Java package.

This is a **feature-oriented** layout on a single Spring Boot module. Do not split into microservices.

---

## 6. Ticket State Machine

**Required graph** (from `spec/requirements.md`):

```text
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

All other transitions are invalid (examples: `CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`).

**Where rules are maintained:** One backend module used by the ticket service (e.g. `TicketStatusTransition`). Controllers and the UI do not duplicate enforcement.

**Valid transitions:** Service loads the ticket, asks the rule whether `current → requested` is allowed, updates status, persists, returns the updated ticket DTO.

**Invalid transitions:** Service does not persist the new status. It signals a domain error; the exception handler returns a client-error REST body. Database status remains unchanged.

**Why the backend is authoritative:** Requirements require the **backend** to accept valid transitions and **reject all invalid** ones, and to enforce validation. The client can be bypassed (direct HTTP). Only server-side checks guarantee the machine. Integration tests of the state machine target backend behavior (service and/or API plus persistence).

**Controllers:** Accept a status-change request, validate the DTO, call the service, return  success or the mapped error. No transition table in the controller.

**Frontend:** May present only likely next statuses for usability. It must still handle rejection messages for invalid transitions. It never persists status locally as a substitute for the API.

---

## 7. Error Handling

One strategy for all REST errors.

| Situation | Handling |
| --- | --- |
| **Validation failures** | Bean Validation on DTOs; 400-class response; field-level messages in the error body. |
| **Invalid state transitions** | Domain exception from the service; client error; message that the transition is not allowed; ticket status unchanged. |
| **Missing resources** | Ticket (or comment target ticket) not found; 404-class response. |
| **Invalid requests** | Malformed JSON, wrong types; 400-class response via framework + handler. |
| **Unexpected server errors** | Uncaught failures; 500-class response; no stack traces, SQL, or secrets in the body. |

**REST error response structure (architectural contract):** A single JSON shape for the frontend, including:

- HTTP status
- A meaningful message suitable to show in the UI
- Optional field errors (name + message) for validation
- Optional machine-readable code (e.g. validation vs invalid transition vs not found) if useful for the UI

Do not return entities, stack traces, or connection strings.

**Frontend:** On non-success, parse this body and display the message (and field errors on forms). Loading is cleared; previous data is not treated as a successful mutation. List/detail refetch only after success.

---

## 8. Testing Architecture

Tests follow the same layers. Behavior over coverage metrics. State-machine **integration tests must pass** (requirements).

| Type | Where | What |
| --- | --- | --- |
| **Unit** | Same package as the rule/helper under `src/test/java` | Transition matrix: each required valid edge allowed; invalid edges rejected. Pure mapping/helpers if present. No Spring unless needed. |
| **Service-layer** | `*Service` tests | Create, update fields, assignee, comments, search, filter; valid transition persists new status; invalid transition throws and does not change status. Mocks **or** persistence per existing project style. |
| **Controller / API** | `@WebMvcTest` (or project equivalent) | HTTP mapping, validation errors, status codes, DTO in/out. Service mocked. Does not re-own the full transition table if service tests already do—still expose invalid-transition HTTP mapping. |
| **Repository / integration** | `@DataJpaTest` and/or `@SpringBootTest` | Persist and reload tickets/comments; search and status filter queries; **state-machine integration**: valid transition committed; invalid transition leaves stored status unchanged. Uses the test database (H2 or test PostgreSQL), not production secrets. |

Frontend may have its own tests later; they are not a substitute for backend state-machine integration tests.

---

## 9. Configuration and Secrets

**Environment-specific configuration:** Spring profiles (e.g. local/dev vs another environment) and `application.properties` / `application.yml` **without** secrets. Profile files may describe datasource **keys** that resolve from the environment.

**Database configuration:** URL, username, and password for PostgreSQL or H2 come from configuration. H2 for local development is acceptable if data is stored so restarts keep tickets. Switching to PostgreSQL is configuration-only, not a new architecture.

**Environment variables:** Sensitive and machine-specific values (`SPRING_DATASOURCE_*` or project-equivalent) are supplied at runtime. Application code does not hardcode passwords.

**Local development:** Documented local profile, local DB, and how to set env vars. Sample config uses placeholders only.

**Secrets:** Never commit credentials, tokens, or private keys. Never log them. If a secret appears in the repo, remove it and rotate; do not copy it into architecture or samples.

---

## 10. Request Flow Examples

Architectural flows only. Path names are illustrative of REST layering, not an extra product spec.

### 10.1 Creating a ticket

1. **Frontend:** Create page; user submits; loading on; POST JSON request DTO.
2. **Controller:** Bind body; **validation**; call create on the service.
3. **Service:** Map DTO to entity; set initial status `OPEN` (assumption in §2); persist via repository.
4. **Repository / database:** Insert ticket; commit.
5. **Error handling:** Validation or persistence failures → standard error body; frontend shows the message. Success → detail or list updated.

### 10.2 Updating ticket status

1. **Frontend:** Detail page; user requests a new status; loading on; REST call with requested status (UI may limit options; not authoritative).
2. **Controller:** Bind/validate request; call status-change use case (not generic field patch without rules).
3. **Service:** Load ticket; **transition rule** `current → requested`; if invalid, fail without save; if valid, set status and save.
4. **Repository / database:** Update only on valid transition.
5. **Error handling:** Invalid transition → client error, status unchanged, UI shows backend message. Missing ticket → not found. Success → detail shows new status.

### 10.3 Adding a comment

1. **Frontend:** Detail page comment form; POST comment body for that ticket.
2. **Controller:** Validate comment DTO; call add-comment on the service.
3. **Service:** Load ticket; reject if missing; attach comment; save.
4. **Repository / database:** Persist comment linked to the ticket (survives restart).
5. **Error handling:** Validation and missing ticket as in §7; success refreshes comments on the detail view.

### 10.4 Searching tickets

1. **Frontend:** List page keyword control (and optionally status filter as a separate required capability); GET with query parameters.
2. **Controller:** Bind query params; validate as applicable; call search and/or filter on the service.
3. **Service:** Delegate to repository; map entities to list DTOs. No transition logic.
4. **Repository / database:** Query by keyword (and by status when filtering). Read-only.
5. **Error handling:** Invalid query → client error; otherwise 200 with matching tickets. Empty list is success, not an error. Frontend shows results or empty list; backend errors shown via the error body.

Keyword search and status filter are distinct required behaviors; the list page may issue one request with both parameters if the API supports combined query without changing the requirements.

---

## Consistency notes

- **Backend** owns validation, persistence, and the status machine.
- **Frontend** owns presentation, HTTP calls, and display of backend errors.
- **Database** owns durable ticket and comment data only.
- **Tests** sit beside the layer they protect; state-machine integration tests prove valid accept / invalid reject **with persistence**.
- **Config** keeps secrets out of source control.

This architecture supports `spec/requirements.md` and nothing beyond it.
