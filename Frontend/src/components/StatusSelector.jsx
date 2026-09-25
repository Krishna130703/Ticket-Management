import { getAllowedNextStatuses } from '../utils/statusTransitions.js';

export default function StatusSelector({ currentStatus, onChange, disabled, error }) {
  const allowedStatuses = getAllowedNextStatuses(currentStatus);

  if (allowedStatuses.length === 0) {
    return <p className="muted">No further status changes are available for this ticket.</p>;
  }

  return (
    <div className="status-selector">
      <label htmlFor="status-change">Change status</label>
      <div className="status-selector-row">
        <select
          id="status-change"
          defaultValue=""
          disabled={disabled}
          onChange={(event) => {
            const value = event.target.value;
            if (value) {
              onChange(value);
              event.target.value = '';
            }
          }}
        >
          <option value="" disabled>Select next status…</option>
          {allowedStatuses.map((status) => (
            <option key={status} value={status}>
              {status.replace(/_/g, ' ')}
            </option>
          ))}
        </select>
      </div>
      {error && <p className="field-error">{error}</p>}
    </div>
  );
}
