import test from 'node:test';
import assert from 'node:assert/strict';

const response = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { 'Content-Type': 'application/json' },
});

async function loadApi() {
  return import(`./api.js?test=${crypto.randomUUID()}`);
}

test('sends cookie credentials and the CSRF header on protected mutations', async () => {
  const originalFetch = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options });
    return url.endsWith('/auth/csrf')
      ? response({ token: 'csrf-token' })
      : response({ saved: true });
  };

  try {
    const { api } = await loadApi();
    assert.deepEqual(await api('/auth/totp/setup', { method: 'POST' }), { saved: true });
    assert.equal(requests.length, 2);
    assert.equal(requests[1].options.credentials, 'include');
    assert.equal(requests[1].options.headers.get('X-XSRF-TOKEN'), 'csrf-token');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('refreshes a rejected CSRF token once, then replays the protected request', async () => {
  const originalFetch = globalThis.fetch;
  let csrfRequests = 0;
  let mutationRequests = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith('/auth/csrf')) {
      csrfRequests += 1;
      return response({ token: csrfRequests === 1 ? 'stale-token' : 'fresh-token' });
    }
    mutationRequests += 1;
    if (mutationRequests === 1) return response({ code: 'csrf_invalid' }, 403);
    return response({ setupIntent: true });
  };

  try {
    const { api } = await loadApi();
    assert.deepEqual(await api('/payments/setup-intent', { method: 'POST' }), { setupIntent: true });
    assert.equal(csrfRequests, 2);
    assert.equal(mutationRequests, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('does not retry authorization denials as CSRF failures', async () => {
  const originalFetch = globalThis.fetch;
  let requests = 0;
  globalThis.fetch = async (url) => {
    requests += 1;
    return url.endsWith('/auth/csrf')
      ? response({ token: 'csrf-token' })
      : response({ code: 'access_denied', message: 'Forbidden' }, 403);
  };

  try {
    const { api } = await loadApi();
    await assert.rejects(api('/payments/setup-intent', { method: 'POST' }), /Forbidden/);
    assert.equal(requests, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
