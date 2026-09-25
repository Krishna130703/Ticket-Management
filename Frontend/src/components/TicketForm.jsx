import { PRIORITIES } from '../utils/statusTransitions.js';

const EMPTY_FORM = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  assignee: '',
};

export function createEmptyTicketForm() {
  return { ...EMPTY_FORM };
}

export function ticketToForm(ticket) {
  return {
    title: ticket.title ?? '',
    description: ticket.description ?? '',
    priority: ticket.priority ?? 'MEDIUM',
    assignee: ticket.assignee ?? '',
  };
}

export function validateTicketForm(form) {
  const errors = {};
  if (!form.title.trim()) {
    errors.title = 'Title is required.';
  }
  if (!form.description.trim()) {
    errors.description = 'Description is required.';
  }
  if (!form.priority) {
    errors.priority = 'Priority is required.';
  }
  return errors;
}

export function buildCreatePayload(form) {
  const payload = {
    title: form.title.trim(),
    description: form.description.trim(),
    priority: form.priority,
  };
  if (form.assignee.trim()) {
    payload.assignee = form.assignee.trim();
  } else {
    payload.assignee = null;
  }
  return payload;
}

export function buildUpdatePayload(form, originalTicket) {
  const payload = {};
  const trimmedTitle = form.title.trim();
  const trimmedDescription = form.description.trim();
  const trimmedAssignee = form.assignee.trim();

  if (trimmedTitle !== originalTicket.title) {
    payload.title = trimmedTitle;
  }
  if (trimmedDescription !== originalTicket.description) {
    payload.description = trimmedDescription;
  }
  if (form.priority !== originalTicket.priority) {
    payload.priority = form.priority;
  }

  const originalAssignee = originalTicket.assignee ?? '';
  if (trimmedAssignee !== originalAssignee) {
    payload.assignee = trimmedAssignee ? trimmedAssignee : null;
  }

  return payload;
}

export default function TicketForm({
  form,
  onChange,
  onSubmit,
  submitLabel,
  submitting,
  errors = {},
  apiError,
}) {
  return (
    <form className="ticket-form" onSubmit={onSubmit} noValidate>
      <div className="form-field">
        <label htmlFor="title">Title *</label>
        <input
          id="title"
          name="title"
          value={form.title}
          onChange={onChange}
          disabled={submitting}
          aria-invalid={Boolean(errors.title)}
        />
        {errors.title && <p className="field-error">{errors.title}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="description">Description *</label>
        <textarea
          id="description"
          name="description"
          rows={5}
          value={form.description}
          onChange={onChange}
          disabled={submitting}
          aria-invalid={Boolean(errors.description)}
        />
        {errors.description && <p className="field-error">{errors.description}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="priority">Priority *</label>
        <select
          id="priority"
          name="priority"
          value={form.priority}
          onChange={onChange}
          disabled={submitting}
        >
          {PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>{priority}</option>
          ))}
        </select>
        {errors.priority && <p className="field-error">{errors.priority}</p>}
      </div>

      <div className="form-field">
        <label htmlFor="assignee">Assignee</label>
        <input
          id="assignee"
          name="assignee"
          value={form.assignee}
          onChange={onChange}
          disabled={submitting}
          placeholder="Optional"
        />
        <p className="field-hint">Leave blank to leave unassigned. On edit, clear the field to remove assignee.</p>
        {errors.assignee && <p className="field-error">{errors.assignee}</p>}
      </div>

      {apiError && <p className="field-error">{apiError}</p>}

      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Saving…' : submitLabel}
      </button>
    </form>
  );
}
