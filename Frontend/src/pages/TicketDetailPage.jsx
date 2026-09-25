import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  addComment,
  changeTicketStatus,
  getTicket,
  updateTicket,
} from '../api/ticketApi.js';
import { ApiError } from '../api/client.js';
import CommentForm from '../components/CommentForm.jsx';
import CommentList from '../components/CommentList.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import StatusSelector from '../components/StatusSelector.jsx';
import TicketForm, {
  buildUpdatePayload,
  ticketToForm,
  validateTicketForm,
} from '../components/TicketForm.jsx';
import { formatDate } from '../utils/formatDate.js';

export default function TicketDetailPage() {
  const { ticketId } = useParams();
  const [ticket, setTicket] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [updateError, setUpdateError] = useState(null);
  const [statusError, setStatusError] = useState(null);
  const [commentError, setCommentError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [updating, setUpdating] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const [postingComment, setPostingComment] = useState(false);

  const loadTicket = useCallback(async () => {
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const data = await getTicket(ticketId);
      setTicket(data);
      setForm(ticketToForm(data));
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setNotFound(true);
      } else {
        setError(err);
      }
      setTicket(null);
      setForm(null);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function handleUpdate(event) {
    event.preventDefault();
    const validationErrors = validateTicketForm(form);
    setFormErrors(validationErrors);
    setUpdateError(null);
    setSuccessMessage('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    const payload = buildUpdatePayload(form, ticket);
    if (Object.keys(payload).length === 0) {
      setUpdateError({ message: 'No changes to save.' });
      return;
    }

    setUpdating(true);
    try {
      const updated = await updateTicket(ticketId, payload);
      setTicket(updated);
      setForm(ticketToForm(updated));
      setSuccessMessage('Ticket updated successfully.');
    } catch (err) {
      setUpdateError(err);
      if (err.fieldErrors?.length) {
        const fieldMap = {};
        err.fieldErrors.forEach((fieldError) => {
          fieldMap[fieldError.field] = fieldError.message;
        });
        setFormErrors(fieldMap);
      }
    } finally {
      setUpdating(false);
    }
  }

  async function handleStatusChange(nextStatus) {
    setStatusError(null);
    setSuccessMessage('');
    setChangingStatus(true);
    try {
      const updated = await changeTicketStatus(ticketId, nextStatus);
      setTicket(updated);
      setForm(ticketToForm(updated));
      setSuccessMessage(`Status changed to ${nextStatus.replace(/_/g, ' ')}.`);
    } catch (err) {
      setStatusError(err.message);
    } finally {
      setChangingStatus(false);
    }
  }

  async function handleAddComment(body) {
    setCommentError(null);
    setSuccessMessage('');
    setPostingComment(true);
    try {
      await addComment(ticketId, body);
      const refreshed = await getTicket(ticketId);
      setTicket(refreshed);
      setForm(ticketToForm(refreshed));
      setSuccessMessage('Comment added.');
    } catch (err) {
      setCommentError(err.message);
      if (err.fieldErrors?.length) {
        const bodyError = err.fieldErrors.find((fieldError) => fieldError.field === 'body');
        if (bodyError) {
          setCommentError(bodyError.message);
        }
      }
    } finally {
      setPostingComment(false);
    }
  }

  if (loading) {
    return <LoadingSpinner label="Loading ticket…" />;
  }

  if (notFound) {
    return (
      <section className="page">
        <ErrorAlert error={{ message: 'Ticket not found.' }} />
        <Link to="/" className="btn btn-secondary">Back to list</Link>
      </section>
    );
  }

  if (error) {
    return (
      <section className="page">
        <ErrorAlert error={error} />
        <Link to="/" className="btn btn-secondary">Back to list</Link>
      </section>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Ticket #{ticket.id}</h1>
          <p className="muted">Created {formatDate(ticket.createdAt)} · Updated {formatDate(ticket.updatedAt)}</p>
        </div>
        <div className="header-actions">
          <StatusBadge status={ticket.status} />
          <Link to="/" className="btn btn-ghost">Back to list</Link>
        </div>
      </header>

      {successMessage && <div className="alert alert-success" role="status">{successMessage}</div>}
      <ErrorAlert error={updateError} onDismiss={() => setUpdateError(null)} />

      <div className="detail-grid">
        <section className="panel">
          <h2>Edit ticket</h2>
          <TicketForm
            form={form}
            onChange={handleFormChange}
            onSubmit={handleUpdate}
            submitLabel="Save changes"
            submitting={updating}
            errors={formErrors}
          />
        </section>

        <section className="panel">
          <h2>Status</h2>
          <p className="muted">Current status: <strong>{ticket.status.replace(/_/g, ' ')}</strong></p>
          <StatusSelector
            currentStatus={ticket.status}
            onChange={handleStatusChange}
            disabled={changingStatus}
            error={statusError}
          />
        </section>
      </div>

      <section className="panel">
        <h2>Comments</h2>
        <CommentList comments={ticket.comments} />
        <CommentForm
          onSubmit={handleAddComment}
          submitting={postingComment}
          error={commentError}
        />
      </section>
    </section>
  );
}
