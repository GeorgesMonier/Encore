const API_BASE = import.meta.env?.VITE_API_BASE_URL ?? '/api';
const CSRF_PATH = '/auth/csrf';
let csrfToken = null;
let csrfRequest = null;

async function getCsrfToken() {
  if (csrfToken) return csrfToken;
  if (!csrfRequest) {
    const request = (async () => {
      const response = await fetch(`${API_BASE}${CSRF_PATH}`, { credentials: 'include' });
      if (!response.ok) {
        throw new Error(`No se pudo iniciar la protección de sesión (HTTP ${response.status}).`);
      }
      const body = await response.json();
      if (typeof body?.token !== 'string' || !body.token) {
        throw new Error('La API no devolvió un token CSRF válido.');
      }
      csrfToken = body.token;
      return csrfToken;
    })();
    csrfRequest = request;
    try {
      return await request;
    } catch (error) {
      csrfToken = null;
      throw error;
    } finally {
      if (csrfRequest === request) csrfRequest = null;
    }
  }
  return csrfRequest;
}

async function sendRequest(path, options, headers) {
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
  return { response, body };
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  const method = (options.method ?? 'GET').toUpperCase();
  const needsCsrf = !['GET', 'HEAD', 'OPTIONS'].includes(method);

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (needsCsrf) {
    headers.set('X-XSRF-TOKEN', await getCsrfToken());
  }

  let { response, body } = await sendRequest(path, options, headers);
  const canReplayBody = options.body == null || typeof options.body === 'string' || options.body instanceof Blob;
  if (needsCsrf && response.status === 403
      && ['csrf_missing', 'csrf_invalid'].includes(body?.code)
      && canReplayBody) {
    csrfToken = null;
    csrfRequest = null;
    headers.set('X-XSRF-TOKEN', await getCsrfToken());
    ({ response, body } = await sendRequest(path, options, headers));
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
