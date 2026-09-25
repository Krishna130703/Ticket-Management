const STATUS_CLASS = {
  OPEN: 'status-open',
  IN_PROGRESS: 'status-in-progress',
  RESOLVED: 'status-resolved',
  CLOSED: 'status-closed',
  CANCELLED: 'status-cancelled',
};

export default function StatusBadge({ status }) {
  const className = STATUS_CLASS[status] || 'status-default';
  const label = status?.replace(/_/g, ' ') ?? 'Unknown';

  return <span className={`status-badge ${className}`}>{label}</span>;
}
