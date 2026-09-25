import { apiRequest } from './client.js';

export function listTickets({ keyword, status } = {}) {
  const params = new URLSearchParams();
  if (keyword) {
    params.set('keyword', keyword);
  }
  if (status) {
    params.set('status', status);
  }
  const query = params.toString();
  const path = query ? `/tickets?${query}` : '/tickets';
  return apiRequest(path);
}

export function getTicket(ticketId) {
  return apiRequest(`/tickets/${ticketId}`);
}

export function createTicket(payload) {
  return apiRequest('/tickets', {
    method: 'POST',
    body: payload,
  });
}

export function updateTicket(ticketId, payload) {
  return apiRequest(`/tickets/${ticketId}`, {
    method: 'PATCH',
    body: payload,
  });
}

export function changeTicketStatus(ticketId, status) {
  return apiRequest(`/tickets/${ticketId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function addComment(ticketId, body) {
  return apiRequest(`/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: { body },
  });
}
