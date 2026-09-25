const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

async function parseErrorResponse(response) {
  let body = null;
  try {
    body = await response.json();
  } catch {
    return new ApiError(
      response.status,
      'UNKNOWN_ERROR',
      response.statusText || 'Request failed.',
      []
    );
  }

  const fieldErrors = body.fieldErrors || body.details || [];
  return new ApiError(
    body.status ?? response.status,
    body.code ?? 'UNKNOWN_ERROR',
    body.message ?? response.statusText ?? 'Request failed.',
    fieldErrors
  );
}

export async function apiRequest(path, options = {}) {
  const url = `${API_BASE_URL}${path}`;
  const headers = {
    Accept: 'application/json',
    ...options.headers,
  };

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  let response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Unable to reach the API. Please check your connection.');
  }

  if (!response.ok) {
    throw await parseErrorResponse(response);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return response.json();
  }

  return null;
}
