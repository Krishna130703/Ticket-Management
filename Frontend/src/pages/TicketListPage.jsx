import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listTickets } from '../api/ticketApi.js';
import ErrorAlert from '../components/ErrorAlert.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';
import TicketList from '../components/TicketList.jsx';
import { ALL_STATUSES } from '../utils/statusTransitions.js';

export default function TicketListPage() {
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await listTickets({
        keyword: keyword || undefined,
        status: statusFilter || undefined,
      });
      setTickets(response.tickets ?? []);
    } catch (err) {
      setError(err);
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, statusFilter]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  function handleSearchSubmit(event) {
    event.preventDefault();
    const trimmed = keywordInput.trim();
    if (keywordInput && !trimmed) {
      setError({ message: 'Search keyword must not be blank.' });
      return;
    }
    setKeyword(trimmed);
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Support Tickets</h1>
          <p className="muted">Search, filter, and manage support tickets.</p>
        </div>
        <Link to="/tickets/new" className="btn btn-primary">Create ticket</Link>
      </header>

      <div className="filters">
        <form className="search-form" onSubmit={handleSearchSubmit}>
          <label htmlFor="keyword">Search</label>
          <div className="search-row">
            <input
              id="keyword"
              type="search"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              placeholder="Search title or description…"
            />
            <button type="submit" className="btn btn-secondary">Search</button>
            {keyword && (
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setKeywordInput('');
                  setKeyword('');
                }}
              >
                Clear
              </button>
            )}
          </div>
        </form>

        <div className="form-field">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="">All</option>
            {ALL_STATUSES.map((status) => (
              <option key={status} value={status}>{status.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </div>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {loading ? (
        <LoadingSpinner label="Loading tickets…" />
      ) : (
        <TicketList tickets={tickets} />
      )}
    </section>
  );
}
