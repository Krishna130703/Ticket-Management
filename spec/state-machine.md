# Support Ticket Management System — Ticket Status State Machine

This document defines the **ticket status state machine only**.

**Sources of truth:** `spec/requirements.md`, `spec/architecture.md`, and `spec/data-model.md`.

This document does not define API endpoints, request/response contracts, database DDL, UI behavior, permissions, roles, or implementation code. It does not add statuses, transitions, automatic transitions, or business rules beyond the existing specifications.

---

## 1. Purpose

The state machine defines the **allowed lifecycle** of a ticket’s status.

Its role is to ensure that:

- A ticket’s status changes only along paths the specifications allow.
- The backend **accepts valid** status transitions.
- The backend **rejects all invalid** status transitions.
- Rejected transitions do not corrupt persisted ticket data.

Status is stored on the ticket as described in `spec/data-model.md`. This document governs **when** that stored value may change.

---

## 2. States

The only ticket status values are:

| State | Meaning in this specification |
| --- | --- |
| `OPEN` | Ticket exists and has not entered progress. |
| `IN_PROGRESS` | Ticket is being worked. |
| `RESOLVED` | Work is complete pending closure. |
| `CLOSED` | Ticket is closed. |
| `CANCELLED` | Ticket was cancelled. |

No other status values exist. Do not add states.

---

## 3. Initial State

When a ticket is created, its status is **`OPEN`**.

This follows `spec/architecture.md` (minimal assumption: new tickets start in `OPEN` so the required graph has a starting status) and `spec/data-model.md` (newly created ticket row has status `OPEN`).

---

## 4. Valid Transitions

The following transitions are **valid**. The backend must accept them.

| From | To |
| --- | --- |
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

Equivalent notation (from `spec/requirements.md`):

```text
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

There are no other valid transitions.

---

## 5. Invalid Transitions

**Any transition not explicitly listed in §4 is invalid and must be rejected.**

The full transition matrix is below. `✓` means valid; `✗` means invalid and rejected.

| From \\ To | `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED` |
| --- | --- | --- | --- | --- | --- |
| `OPEN` | ✗ | ✓ | ✗ | ✗ | ✓ |
| `IN_PROGRESS` | ✗ | ✗ | ✓ | ✗ | ✓ |
| `RESOLVED` | ✗ | ✗ | ✗ | ✓ | ✗ |
| `CLOSED` | ✗ | ✗ | ✗ | ✗ | ✗ |
| `CANCELLED` | ✗ | ✗ | ✗ | ✗ | ✗ |

Notes:

- **Same-state** changes (e.g. `OPEN → OPEN`) are not listed in §4 and are therefore **invalid**.
- There are no transitions **out of** `CLOSED` or `CANCELLED`.

---

## 6. Terminal States

States with **no valid outgoing transitions**:

- `CLOSED`
- `CANCELLED`

From these states, every requested status change is invalid and must be rejected.

---

## 7. Enforcement

- The **backend** is the authoritative enforcement point for this state machine.
- The backend must apply the rules in §4–§6 on every status change.
- **Frontend validation alone is not sufficient.** The UI may hide or disable options for usability, but the backend must still reject invalid transitions because clients can send arbitrary requests.

---

## 8. Invalid Transition Behavior

For any rejected transition, the backend must:

- **Preserve the current status.** The ticket’s stored status does not change.
- **Not persist an invalid status.** No write commits a status that violates §4–§6.
- **Return an appropriate client error** (consistent with the API error handling in `spec/architecture.md`).
- **Allow the frontend to display a meaningful error** using the backend error response.

The requirements do not specify a particular HTTP status code or message text; those are defined in the API/error specifications, not here.

---

## 9. Examples

Representative **valid** transitions:

```text
OPEN → IN_PROGRESS
OPEN → CANCELLED
IN_PROGRESS → RESOLVED
IN_PROGRESS → CANCELLED
RESOLVED → CLOSED
```

Representative **invalid** transitions (must be rejected):

```text
CLOSED → OPEN
RESOLVED → OPEN
CANCELLED → OPEN
OPEN → RESOLVED
OPEN → CLOSED
IN_PROGRESS → CLOSED
RESOLVED → CANCELLED
CLOSED → IN_PROGRESS
CANCELLED → RESOLVED
OPEN → OPEN
```

The invalid list is illustrative; **any** pair not in §4 is invalid.

---

## 10. Testing Requirements

Tests must cover the state machine without changing these rules.

| Scenario | Requirement |
| --- | --- |
| **Every valid transition** | For each row in §4, a ticket in the “from” state can move to the “to” state, and the new status is persisted. |
| **Representative invalid transitions** | For invalid pairs (including the examples in §9), the backend rejects the change. |
| **Transitions from terminal states** | From `CLOSED` and from `CANCELLED`, every requested transition is rejected. |
| **Rejected transitions do not change persisted status** | After a rejected transition, the ticket’s stored status remains the original “from” status. |

State-machine integration tests must pass, as required by `spec/requirements.md`. Test implementation details (framework, URLs, fixtures) are out of scope for this document.
