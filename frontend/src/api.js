const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';
const CSRF_PATH = '/auth/csrf';
let csrfToken = null;
let csrfRequest = null;

async function getCsrfToken() {
  if (csrfToken) return csrfToken;
  if (!csrfRequest) {
    csrfRequest = fetch(`${API_BASE}${CSRF_PATH}`, { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`No se pudo iniciar la protección de sesión (HTTP ${response.status}).`);
        }
        const body = await response.json();
        csrfToken = body.token;
        return csrfToken;
      })
      .catch((error) => {
        csrfRequest = null;
        throw error;
      });
  }
  return csrfRequest;
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  const method = (options.method ?? 'GET').toUpperCase();

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers.set('X-XSRF-TOKEN', await getCsrfToken());
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: 'include',
    headers,
  });
  const text = await response.text();
  let body = text;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof body === 'string'
        ? body
        : body?.message ?? body?.error ?? `Error ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return body;
}
