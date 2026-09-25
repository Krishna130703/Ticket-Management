import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createTicket } from '../api/ticketApi.js';
import ErrorAlert from '../components/ErrorAlert.jsx';
import TicketForm, {
  buildCreatePayload,
  createEmptyTicketForm,
  validateTicketForm,
} from '../components/TicketForm.jsx';

export default function CreateTicketPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(createEmptyTicketForm());
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validateTicketForm(form);
    setErrors(validationErrors);
    setApiError(null);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setSubmitting(true);
    try {
      const ticket = await createTicket(buildCreatePayload(form));
      navigate(`/tickets/${ticket.id}`);
    } catch (err) {
      setApiError(err);
      if (err.fieldErrors?.length) {
        const fieldMap = {};
        err.fieldErrors.forEach((fieldError) => {
          fieldMap[fieldError.field] = fieldError.message;
        });
        setErrors(fieldMap);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Create Ticket</h1>
          <p className="muted">New tickets are created with status OPEN.</p>
        </div>
        <Link to="/" className="btn btn-ghost">Back to list</Link>
      </header>

      <ErrorAlert error={apiError} onDismiss={() => setApiError(null)} />

      <TicketForm
        form={form}
        onChange={handleChange}
        onSubmit={handleSubmit}
        submitLabel="Create ticket"
        submitting={submitting}
        errors={errors}
      />
    </section>
  );
}
