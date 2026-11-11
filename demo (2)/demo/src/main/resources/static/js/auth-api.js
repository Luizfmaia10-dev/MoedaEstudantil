(function initializeAuthApi(root) {
  function createAuthApi(request) {
    return {
      login(email, senha, perfil) {
        return request('http://localhost:8080/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: email.trim(), senha, perfil })
        });
      }
    };
  }

  const api = { createAuthApi };
  if (root.AppHttp && typeof root.AppHttp.request === 'function') {
    root.AuthApi = createAuthApi(root.AppHttp.request);
  }
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
