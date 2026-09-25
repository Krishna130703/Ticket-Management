/** Allowed transitions per spec/state-machine.md */
const ALLOWED_TRANSITIONS = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

export const ALL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

export function getAllowedNextStatuses(currentStatus) {
  return ALLOWED_TRANSITIONS[currentStatus] ?? [];
}

export function isTerminalStatus(status) {
  return getAllowedNextStatuses(status).length === 0;
}
