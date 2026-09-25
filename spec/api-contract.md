# Support Ticket Management System — API Contract

This document is an **API contract specification only**. It does not implement the application.

**Sources of truth:** `spec/requirements.md`, `spec/architecture.md`, `spec/data-model.md`, `spec/state-machine.md`.

This contract does not add authentication, authorization, users, notifications, SLA, attachments, pagination, or other capabilities not required by those specifications.

Choices not mandated by the specifications are labeled **design decision**.

---

## Requirement mapping

Every endpoint exists only to support an existing requirement.

| Endpoint | Requirement |
| --- | --- |
| `POST /api/tickets` | Create tickets (from the UI via this API). |
| `GET /api/tickets` | List tickets; keyword search; filter by status. |
| `GET /api/tickets/{id}` | View ticket details. |
| `PATCH /api/tickets/{id}` | Update title, description, priority, assignee. |
| `POST /api/tickets/{id}/status` | Valid status transitions; reject invalid ones. |
| `POST /api/tickets/{id}/comments` | Add comments. |

No other resources or operations are defined.

---

## 1. API Conventions

| Topic | Convention |
| --- | --- |
| **Base path** | `/api` (**design decision**; architecture allows `/api/...`). |
| **Content type** | `application/json` for request and response bodies. Charset UTF-8. |
| **JSON** | Object and array JSON. Property names are **camelCase**. |
| **HTTP methods** | `GET` read; `POST` create ticket, add comment, change status; `PATCH` partial field update. |
| **Identifiers** | Ticket and comment `id` values are JSON numbers (64-bit integers). **Design decision** (`spec/data-model.md` left numeric vs UUID open). |
| **Timestamps** | ISO-8601 UTC strings, e.g. `2026-09-25T10:00:00Z`. |
| **Null** | Optional `assignee` may be `null`. |
| **Success body** | Resource DTO(s) as defined in §5. Do not return persistence entities. |
| **Error body** | Single error object (§7) for all client and server errors. |
| **Auth** | None. Not specified. |

**Status values** (only these; `spec/state-machine.md`):

`OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`

**Priority values** (**design decision**; requirements do not name levels):

`LOW` | `MEDIUM` | `HIGH`

The backend must reject unknown `status` and `priority` values as validation failures.

---

## 2. Ticket Endpoints

### 2.1 Create ticket

| | |
| --- | --- |
| **Method / path** | `POST /api/tickets` |
| **Purpose** | Create a ticket. |
| **Parameters** | None. |
| **Request body** | `CreateTicketRequest` (§4.1). Must **not** include `status`. |
| **Success** | `201 Created`. Body: `TicketResponse`. Optional `Location: /api/tickets/{id}` (**design decision**). |
| **Errors** | `400` validation; `500` unexpected. |

New tickets are persisted with status `OPEN` (`spec/state-machine.md` §3).

### 2.2 List tickets

| | |
| --- | --- |
| **Method / path** | `GET /api/tickets` |
| **Purpose** | List tickets. Same endpoint supports search and status filter (§8). |
| **Query parameters** | `keyword` (optional, string); `status` (optional, status enum). |
| **Request body** | None. |
| **Success** | `200 OK`. Body: `TicketListResponse`. Empty list is success. |
| **Errors** | `400` if `status` is not a valid status value; `500` unexpected. |

List items do **not** include comments (**design decision**). Use get-details for comments.

### 2.3 Get ticket details

| | |
| --- | --- |
| **Method / path** | `GET /api/tickets/{id}` |
| **Purpose** | View one ticket and its comments. |
| **Path** | `id` (required, integer) — ticket identifier. |
| **Request body** | None. |
| **Success** | `200 OK`. Body: `TicketResponse` including `comments`. |
| **Errors** | `400` if `id` is not a valid integer; `404` ticket not found; `500` unexpected. |

### 2.4 Update ticket fields

| | |
| --- | --- |
| **Method / path** | `PATCH /api/tickets/{id}` |
| **Purpose** | Update title, description, priority, and/or assignee. **Does not change status.** |
| **Path** | `id` (required, integer). |
| **Request body** | `UpdateTicketRequest` (§4.2). |
| **Success** | `200 OK`. Body: `TicketResponse`. |
| **Errors** | `400` validation (including empty body); `404` not found; `500` unexpected. |

### 2.5 Change ticket status

| | |
| --- | --- |
| **Method / path** | `POST /api/tickets/{id}/status` |
| **Purpose** | Request a status transition. |
| **Path** | `id` (required, integer). |
| **Request body** | `ChangeStatusRequest` (§4.3): `{ "status": "<target>" }`. |
| **Success** | `200 OK`. Body: `TicketResponse` with the new status. |
| **Errors** | `400` validation (missing/unknown status); `404` not found; `409` invalid transition; `500` unexpected. |

**409** for invalid transitions is a **design decision** (architecture requires a client error; conflict fits “state unchanged”).

Rules: see §9.

### 2.6 Search tickets

| | |
| --- | --- |
| **Method / path** | `GET /api/tickets?keyword={keyword}` |
| **Purpose** | Search tickets by keyword. |
| **Query** | `keyword` — non-blank string. Matching is against ticket **title and description** (architecture assumption). |
| **Success / errors** | Same as list (§2.2). |

### 2.7 Filter tickets by status

| | |
| --- | --- |
| **Method / path** | `GET /api/tickets?status={status}` |
| **Purpose** | Return tickets whose stored status equals the given value. |
| **Query** | `status` — one of the five allowed statuses. |
| **Success / errors** | Same as list (§2.2). |

Search and filter may be combined: `GET /api/tickets?keyword={keyword}&status={status}` (§8).

---

## 3. Comment Endpoint

### 3.1 Add comment

| | |
| --- | --- |
| **Method / path** | `POST /api/tickets/{id}/comments` |
| **Purpose** | Add a comment to an existing ticket. |
| **Path** | `id` (required, integer) — ticket identifier. |
| **Request body** | `AddCommentRequest` (§4.4). |
| **Success** | `201 Created`. Body: `CommentResponse`. Optional `Location` (**design decision**). |
| **Errors** | `400` validation; `404` if the ticket does not exist; `500` unexpected. |

There is no separate list-comments endpoint; comments are returned on `GET /api/tickets/{id}`.

---

## 4. Request DTOs

Backend validation is authoritative. The UI may validate for usability; the API still enforces these rules.

### 4.1 CreateTicketRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | Yes | Non-blank. |
| `description` | string | Yes | Non-blank. |
| `priority` | string | Yes | Exactly `LOW`, `MEDIUM`, or `HIGH`. |
| `assignee` | string or `null` | No | If present and not `null`, non-blank. |

Must not include `status` or `id`. Unknown JSON properties: rejected if the application is configured to fail on unknown properties (**design decision**: fail on unknown properties for these DTOs).

### 4.2 UpdateTicketRequest

Partial update. At least one of the fields below must be present.

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | No | If present: non-blank. |
| `description` | string | No | If present: non-blank. |
| `priority` | string | No | If present: `LOW`, `MEDIUM`, or `HIGH`. |
| `assignee` | string or `null` | No | If present: `null` (clear) or non-blank string. |

Must not include `status`. Status changes use §2.5 only.

### 4.3 ChangeStatusRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `status` | string | Yes | Exactly one of: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`. |

Presence of a **named** status is validation (`400`). Whether **current → requested** is allowed is the state machine (`409` if not allowed). An unknown string is `400`, not a transition.

### 4.4 AddCommentRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `body` | string | Yes | Non-blank. |

---

## 5. Response DTOs

### 5.1 TicketResponse

| Field | Type | Notes |
| --- | --- | --- |
| `id` | number | Ticket identifier. |
| `title` | string | |
| `description` | string | |
| `priority` | string | `LOW` \| `MEDIUM` \| `HIGH` |
| `assignee` | string or `null` | |
| `status` | string | One of the five statuses. |
| `createdAt` | string | ISO-8601 UTC. |
| `updatedAt` | string | ISO-8601 UTC. |
| `comments` | array of `CommentResponse` | Present on create/get/update/status responses. Empty array if none. Omitted on list items (**design decision**). |

### 5.2 TicketListResponse

| Field | Type | Notes |
| --- | --- | --- |
| `tickets` | array of ticket summaries | Each summary is `TicketResponse` **without** `comments`. |

### 5.3 CommentResponse

| Field | Type | Notes |
| --- | --- | --- |
| `id` | number | Comment identifier. |
| `ticketId` | number | Parent ticket. |
| `body` | string | |
| `createdAt` | string | ISO-8601 UTC. |

### 5.4 ErrorResponse

See §7.

---

## 6. HTTP Status Codes

| Outcome | HTTP status |
| --- | --- |
| Successful creation (ticket or comment) | `201 Created` |
| Successful retrieval (list or detail) | `200 OK` |
| Successful update (fields or valid status change) | `200 OK` |
| Validation failure (DTO, blank fields, unknown enum, malformed JSON, invalid `id` format, empty PATCH) | `400 Bad Request` |
| Ticket not found | `404 Not Found` |
| Invalid status transition (named status, disallowed by `spec/state-machine.md`) | `409 Conflict` |
| Unexpected server error | `500 Internal Server Error` |

Do not return `200` with an error payload.

---

## 7. Error Contract

One JSON object for every error:

```text
ErrorResponse
  status     number   HTTP status code
  code       string   Machine-readable code (see below)
  message    string   Human-readable message suitable for the UI
  fieldErrors array   Optional; present for validation failures
    field    string   JSON field or query/path parameter name
    message  string   Why that field failed
```

| `code` | When |
| --- | --- |
| `VALIDATION_ERROR` | `400` — request does not satisfy §4 or query/path types. |
| `TICKET_NOT_FOUND` | `404` — no ticket for `{id}`. |
| `INVALID_STATUS_TRANSITION` | `409` — `current → requested` not in `spec/state-machine.md` §4. |
| `INTERNAL_ERROR` | `500` |

`message` must be meaningful and safe (no stack traces, SQL, or secrets). For `INVALID_STATUS_TRANSITION`, the message must make clear that the transition is not allowed (e.g. current and requested status).

`fieldErrors` is omitted or empty when there are no field-level validation failures.

---

## 8. Search and Filtering

Both use **query parameters on** `GET /api/tickets`.

| Parameter | Meaning |
| --- | --- |
| `keyword` | If present and non-blank, restrict to tickets whose **title or description** contains the keyword. Matching details (case sensitivity) are an **implementation decision**; behavior must be consistent. |
| `status` | If present, restrict to tickets whose status **equals** this value. Must be one of the five statuses. |

**Combined:** both parameters may be sent together. The result is the intersection: tickets matching the keyword **and** the given status.

Omitted parameters are not applied. A blank `keyword` is a validation error (**design decision**). Empty result set is `200` with `"tickets": []`.

---

## 9. Status Changes

The client supplies the **target** status in `ChangeStatusRequest.status` on `POST /api/tickets/{id}/status`.

- The backend loads the ticket’s **current** status and validates `current → requested` against **`spec/state-machine.md`**.
- Transitions listed in that specification’s valid matrix are accepted and persisted.
- **Any other pair is rejected** (`409`, `INVALID_STATUS_TRANSITION`). Current status is **unchanged** and not overwritten.
- This API **must not** introduce statuses or transitions other than those in `spec/state-machine.md`.
- Same-state requests (e.g. `OPEN` → `OPEN`) are invalid per that specification.
- Field `PATCH` must not accept `status`. Create must not accept `status`; the server sets `OPEN`.

Frontend restriction of options is optional and **not** authoritative.

---

## 10. Examples

Illustrative JSON only. Not executable code.

### Create ticket

Request: `POST /api/tickets`

```json
{
  "title": "Cannot log in",
  "description": "Error after entering credentials.",
  "priority": "HIGH",
  "assignee": null
}
```

Response: `201`

```json
{
  "id": 1001,
  "title": "Cannot log in",
  "description": "Error after entering credentials.",
  "priority": "HIGH",
  "assignee": null,
  "status": "OPEN",
  "createdAt": "2026-09-25T10:00:00Z",
  "updatedAt": "2026-09-25T10:00:00Z",
  "comments": []
}
```

### List tickets

Request: `GET /api/tickets`

Response: `200`

```json
{
  "tickets": [
    {
      "id": 1001,
      "title": "Cannot log in",
      "description": "Error after entering credentials.",
      "priority": "HIGH",
      "assignee": null,
      "status": "OPEN",
      "createdAt": "2026-09-25T10:00:00Z",
      "updatedAt": "2026-09-25T10:00:00Z"
    }
  ]
}
```

### Get ticket

Request: `GET /api/tickets/1001`

Response: `200` (same shape as create, with `comments` populated when present).

### Update ticket

Request: `PATCH /api/tickets/1001`

```json
{
  "assignee": "Alex",
  "priority": "MEDIUM"
}
```

Response: `200` — `TicketResponse` with updated `assignee`, `priority`, and `updatedAt`; `status` unchanged.

### Change status

Request: `POST /api/tickets/1001/status`

```json
{
  "status": "IN_PROGRESS"
}
```

Response: `200` — `TicketResponse` with `"status": "IN_PROGRESS"` when the ticket was `OPEN`.

### Add comment

Request: `POST /api/tickets/1001/comments`

```json
{
  "body": "Investigating with the user."
}
```

Response: `201`

```json
{
  "id": 501,
  "ticketId": 1001,
  "body": "Investigating with the user.",
  "createdAt": "2026-09-25T10:05:00Z"
}
```

### Search / filter

Request: `GET /api/tickets?keyword=login&status=OPEN`

Response: `200` — `TicketListResponse` containing only tickets that match both.

### Validation error

Request: `POST /api/tickets` with `"title": ""`

Response: `400`

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "fieldErrors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

### Invalid status transition

Ticket is `CLOSED`. Request: `POST /api/tickets/1001/status` with `"status": "OPEN"`

Response: `409`

```json
{
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Transition from CLOSED to OPEN is not allowed.",
  "fieldErrors": []
}
```

Persisted status remains `CLOSED`.

### Ticket not found

Request: `GET /api/tickets/99999`

Response: `404`

```json
{
  "status": 404,
  "code": "TICKET_NOT_FOUND",
  "message": "Ticket not found.",
  "fieldErrors": []
}
```
