import { Link } from 'react-router-dom';
import StatusBadge from './StatusBadge.jsx';
import { formatDate } from '../utils/formatDate.js';

export default function TicketCard({ ticket }) {
  return (
    <article className="ticket-card">
      <div className="ticket-card-header">
        <h3>
          <Link to={`/tickets/${ticket.id}`}>#{ticket.id} — {ticket.title}</Link>
        </h3>
        <StatusBadge status={ticket.status} />
      </div>
      <p className="ticket-description">{ticket.description}</p>
      <dl className="ticket-meta">
        <div>
          <dt>Priority</dt>
          <dd>{ticket.priority}</dd>
        </div>
        <div>
          <dt>Assignee</dt>
          <dd>{ticket.assignee ?? 'Unassigned'}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatDate(ticket.updatedAt)}</dd>
        </div>
      </dl>
    </article>
  );
}
