import TicketCard from './TicketCard.jsx';
import EmptyState from './EmptyState.jsx';

export default function TicketList({ tickets }) {
  if (!tickets.length) {
    return <EmptyState message="Try adjusting your search or filter, or create a new ticket." />;
  }

  return (
    <div className="ticket-list">
      {tickets.map((ticket) => (
        <TicketCard key={ticket.id} ticket={ticket} />
      ))}
    </div>
  );
}
