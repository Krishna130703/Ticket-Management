# Support Ticket Management System — Requirements

## 1. Purpose

This document states the requirements for a Support Ticket Management System built as a Spec-Driven Development assignment.

The system must be implemented using Java 21, Spring Boot, PostgreSQL or H2, a REST API, and a React, Next.js, or equivalent frontend.

## 2. Scope

In scope:

- Creating tickets.
- Listing tickets.
- Viewing ticket details.
- Updating ticket title, description, priority, and assignee.
- Adding comments.
- Searching tickets by keyword.
- Filtering tickets by status.
- Persisting ticket data in a database.
- Validating input on the backend.
- Displaying meaningful backend errors in the UI.
- Enforcing the ticket status state machine defined in this document.
- Rejecting invalid status transitions on the backend.

Out of scope is defined in section 10.

## 3. Functional Requirements

The system must support the following:

1. Create tickets.
2. List tickets.
3. View ticket details.
4. Update ticket title, description, priority, and assignee.
5. Add comments.
6. Search tickets by keyword.
7. Filter tickets by status.
8. Persist ticket data in a database.
9. Validate input on the backend.
10. Display meaningful backend errors in the UI.

## 4. Ticket Requirements

- A user must be able to create a ticket from the UI.
- A user must be able to list tickets.
- A user must be able to view ticket details.
- A user must be able to update a ticket’s title.
- A user must be able to update a ticket’s description.
- A user must be able to update a ticket’s priority.
- A user must be able to change a ticket’s assignee.
- A user must be able to add comments to a ticket.
- A user must be able to search tickets by keyword.
- A user must be able to filter tickets by status.

## 5. Status Requirements

Ticket status must follow this state machine.

Valid transitions:

```text
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

The backend must accept valid status transitions.

The backend must reject all invalid status transitions.

Invalid transition examples:

```text
CLOSED → OPEN
RESOLVED → OPEN
CANCELLED → OPEN
```

State-machine integration tests must pass.

## 6. Validation Requirements

- The backend must validate input.
- Backend validation must be enforced.

## 7. Error Handling Requirements

- The backend must reject invalid status transitions.
- The UI must display meaningful backend errors.

## 8. Persistence Requirements

- Ticket data must be persisted in a database.
- Data must persist across application restarts.
- No secrets must be committed to the repository.

## 9. Acceptance Criteria

The following criteria are directly traceable to the requirements in this document.

| Acceptance criterion | Traces to |
| --- | --- |
| Tickets can be created from the UI. | Functional requirement 1; Ticket Requirements |
| Tickets can be listed and viewed. | Functional requirements 2 and 3; Ticket Requirements |
| Ticket fields can be updated. | Functional requirement 4; Ticket Requirements (title, description, priority) |
| Assignees can be changed. | Functional requirement 4; Ticket Requirements |
| Comments can be added. | Functional requirement 5; Ticket Requirements |
| Keyword search works. | Functional requirement 6; Ticket Requirements |
| Status filtering works. | Functional requirement 7; Ticket Requirements |
| Valid status transitions are accepted. | Status Requirements |
| Invalid status transitions are rejected by the backend. | Status Requirements; Error Handling Requirements |
| Data persists across application restarts. | Functional requirement 8; Persistence Requirements |
| Backend validation is enforced. | Functional requirement 9; Validation Requirements |
| Meaningful errors are displayed in the UI. | Functional requirement 10; Error Handling Requirements |
| State-machine integration tests pass. | Status Requirements |
| No secrets are committed to the repository. | Persistence Requirements |

## 10. Out of Scope

Anything not stated in sections 1–9 of this document is out of scope.

This document does not add features, business rules, or implementation details beyond those stated above.
