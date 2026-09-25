# Support Ticket Management System — Data Model

This document is a **logical persistence model specification** only. It does not implement the application, define APIs, or create database objects.

**Sources of truth:** `spec/requirements.md` and `spec/architecture.md`.

**Boundaries:**

- **State-transition rules** are defined only in `spec/state-machine.md`. This document describes how status is **stored**, not when it may change.
- **API contracts and DTOs** are defined only in `spec/api-contract.md`. This document describes persisted data, not request/response JSON.

Where a choice is not fixed by the requirements or architecture, it is labeled an **implementation decision**.

---

## 1. Data Model Overview

**Purpose of the persistence model:** Provide durable storage for ticket data so the system can create tickets, list them, show details, update title/description/priority/assignee, add comments, search by keyword, filter by status, and keep data across application restarts.

**Main persisted concepts:**

| Concept | Role |
| --- | --- |
| **Ticket** | Aggregate root. Carries title, description, priority, assignee, and current status. |
| **Comment** | Dependent record attached to one ticket. |

No other entities are required by the existing specifications. In particular, no `User`, `Role`, `Authentication`, `Notification`, `SLA`, or `Attachment` entity is introduced.

**How the model supports the requirements:**

| Requirement area | Supported by |
| --- | --- |
| Create / list / view / update fields | Ticket row with the fields below; repository reads and writes. |
| Change assignee | Assignee stored as data on the ticket (no user table). |
| Add comments | Comment rows linked to a ticket. |
| Keyword search | Searchable text on the ticket (see §2 and §9). |
| Filter by status | Status stored on the ticket. |
| Persistence across restarts | Relational database (PostgreSQL or H2) per architecture. |
| Backend validation | Constraints and requiredness below are enforced on the backend; this document does not replace API validation rules. |

---

## 2. Ticket

**Purpose:** One persisted support ticket. The ticket is the unit users create, list, view, update, and move through the status lifecycle.

### Fields

| Field | Meaning | Logical type | Required | Notes |
| --- | --- | --- | --- | --- |
| `id` | Unique identifier for the ticket. | Unique identifier (implementation decision: numeric vs UUID). | Yes | Primary key. |
| `title` | Short subject of the ticket. Updatable. | Text. | Yes | Must be present so create/update can be validated on the backend. |
| `description` | Longer body of the ticket. Updatable. | Text. | Yes | Same rationale as title. |
| `priority` | Priority of the ticket. Updatable. | Priority value (see §5). | Yes | Required because the requirements treat priority as a ticket field that is updated; the requirements do not define a default. |
| `assignee` | Who the ticket is assigned to. Updatable. | Text (name or identifier as provided by the client). | No | Requirements require changing the assignee but do not require an assignee at creation; no user entity exists, so this is a stored value, not a foreign key. |
| `status` | Current lifecycle status. | Status value (see §4). | Yes | Every ticket has a status so filtering and the state machine apply. |
| `createdAt` | When the ticket was created. | Timestamp. | Yes | Implementation decision: supports stable ordering and audit without extra features. |
| `updatedAt` | When the ticket was last modified. | Timestamp. | Yes | Implementation decision: same rationale. |

**Constraints supported by the requirements:**

- `id` is unique and non-null (primary key).
- `title`, `description`, `priority`, and `status` are non-null.
- `assignee` may be null (optional).
- No uniqueness on `title` or other business fields is required by the specifications; do not add unique constraints for convention.

---

## 3. Comment

**Purpose:** One comment added to a ticket. Comments persist with ticket data and appear with ticket details.

### Fields

| Field | Meaning | Logical type | Required | Notes |
| --- | --- | --- | --- | --- |
| `id` | Unique identifier for the comment. | Unique identifier (implementation decision). | Yes | Primary key. |
| `ticketId` | The ticket this comment belongs to. | Same type as Ticket `id`. | Yes | Logical foreign key to Ticket. |
| `body` | Comment text. | Text. | Yes | Required so “add comments” can be validated and stored. |
| `createdAt` | When the comment was created. | Timestamp. | Yes | Implementation decision: ordering and audit for the comment list. |

**Relationship with Ticket:** Many comments belong to one ticket. A comment cannot exist without its ticket (see §6).

---

## 4. Ticket Status

**Representation:** Status is a **value stored on the ticket** (`status` field), not a separate table.

**Allowed values** (supported by `spec/requirements.md` and `spec/architecture.md`):

```text
OPEN
IN_PROGRESS
RESOLVED
CLOSED
CANCELLED
```

These are the only status values named in the specifications. Do not add others.

**Initial value:** `spec/architecture.md` states the minimal assumption that new tickets start in `OPEN` so the state machine has a starting status. That assumption applies to the persisted model: a newly created ticket row has status `OPEN`.

**This document does not define transitions.** Which changes are allowed is owned by `spec/state-machine.md`.

---

## 5. Ticket Priority

**Representation:** Priority is a **value stored on the ticket** (`priority` field), not a separate table.

**Allowed values:** The existing specifications do **not** name priority levels. Therefore this document does not define an allowed-value list.

**Implementation decision (not a requirement):** The implementation must choose a concrete representation (for example a small set of named levels or an agreed scale) and enforce it consistently on the backend. Until that choice is fixed in code or in `spec/api-contract.md`, no priority values are specified here.

---

## 6. Relationships and Cardinality

| Relationship | Cardinality | Ownership | Logical foreign key |
| --- | --- | --- | --- |
| Ticket → Comment | One ticket has zero or many comments. Each comment belongs to exactly one ticket. | Ticket is the parent; Comment is dependent on Ticket. | `Comment.ticketId` references `Ticket.id`. |

- **Ownership:** Comments exist only in the context of a ticket. The architecture treats the ticket as the aggregate root.
- **Dependency:** Deleting or referencing behavior beyond “comments belong to a ticket” is not specified in the requirements. Do not add cascade rules as requirements; any cascade is an **implementation decision**.

---

## 7. Database Constraints

Logical constraints only; physical DDL is out of scope.

| Constraint | Applies to | Justification |
| --- | --- | --- |
| Primary key | `Ticket.id`, `Comment.id` | Uniquely identify rows. |
| Foreign key | `Comment.ticketId` → `Ticket.id` | Referential integrity: every comment references an existing ticket. |
| Not null | Ticket: `title`, `description`, `priority`, `status`, `createdAt`, `updatedAt`; Comment: `ticketId`, `body`, `createdAt` | Backend validation and persistence of required data. |
| Not null (optional) | Ticket `assignee` may be null | Requirements do not mandate an assignee at creation. |
| Unique | Only primary keys | No other uniqueness is required by the specifications. |

Do not add check constraints, unique indexes, or composite keys unless the requirements or a later specification explicitly require them.

---

## 8. Validation Rules

Only rules supported by the requirements or existing specifications:

- The **backend validates input**; persistence must not accept a ticket without required fields listed as required in §2, or a comment without `body` and `ticketId`.
- `status` must be one of the values in §4.
- `priority` must be a valid value **once the implementation has fixed the allowed set** (§5). This document does not invent that set.
- Invalid **status transitions** are rejected by the backend; the rule set is defined in `spec/state-machine.md`, not here.

Not specified (do not invent): maximum lengths, title uniqueness, assignee format, comment length limits, or extra defaults.

---

## 9. Persistence Considerations

**Requirements vs implementation decisions:**

| Topic | Requirement (from specs) | Implementation decision (not a requirement) |
| --- | --- | --- |
| Durability | Data persists across restarts; use PostgreSQL or H2. | File-based H2 vs PostgreSQL per environment. |
| Identifier | Each ticket and comment is uniquely identifiable. | Numeric sequence vs UUID; generation strategy. |
| Timestamps | Not explicitly required. | `createdAt` / `updatedAt` on ticket; `createdAt` on comment (see §2–§3). |
| Nullability | Required vs optional as in §2–§3. | Column nullability matches those tables. |
| Status storage | Status stored on ticket; values in §4. | Enum as string vs ordinal; string is typical for readability. |
| Priority storage | Priority stored on ticket. | Representation and allowed values (§5). |
| Relationships | Comments belong to a ticket. | One-to-many mapping details; fetch strategy. |
| Keyword search | Search tickets by keyword. | Architecture’s minimal assumption: search applies to ticket `title` and `description`. |
| Indexing | None stated. | Optional index on `Ticket.status` for filter; optional support for keyword search on title/description. Add only if needed for the required queries. |

Do not add audit tables, history tables, or soft-delete unless a future specification requires them.

---

## 10. Example Logical Data Representation

Illustrative only. Not JSON API shape, not SQL, not implementation code.

```text
Ticket
  id:          T-1001
  title:       Cannot log in
  description: User sees an error after entering credentials.
  priority:    <implementation-defined value>
  assignee:    null
  status:      OPEN
  createdAt:   2026-09-25T10:00:00Z
  updatedAt:   2026-09-25T10:00:00Z

Comment
  id:          C-501
  ticketId:    T-1001
  body:        Investigating with the user.
  createdAt:   2026-09-25T10:05:00Z

Comment
  id:          C-502
  ticketId:    T-1001
  body:        Password reset resolved the issue.
  createdAt:   2026-09-25T10:20:00Z
```

This shows one ticket with two comments. It does not prescribe field sizes, JSON names, or database types.
