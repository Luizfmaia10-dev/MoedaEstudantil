(function initializeHttp(root) {
  class HttpError extends Error {
    constructor(message, status, body) {
      super(message);
      this.name = 'HttpError';
      this.status = status;
      this.body = body;
    }
  }

  async function parseBody(response) {
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try { return JSON.parse(text); } catch { return text; }
    }
    return text;
  }

  async function request(url, options = {}, fetchImpl = root.fetch) {
    const response = await fetchImpl(url, options);
    const body = await parseBody(response);
    if (!response.ok) {
      const message = body && typeof body === 'object' && body.message
        ? body.message
        : 'Não foi possível concluir a operação.';
      throw new HttpError(message, response.status, body);
    }
    return body;
  }

  const api = { HttpError, request };
  root.AppHttp = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
