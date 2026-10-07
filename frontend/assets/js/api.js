window.API = (() => {
    const base = (window.HELPDESK_CONFIG?.apiBase || '').replace(/\/$/, '') + '/api';
    let csrf;
    async function request(path, options = {}) {
        const method = options.method || 'GET';
        const headers = { ...options.headers };
        if (!['GET', 'HEAD'].includes(method)) {
            if (!csrf) csrf = await request('/auth/csrf');
            headers[csrf.headerName] = csrf.token;
        }
        let body = options.body;
        if (body && !(body instanceof FormData)) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(body); }
        let response;
        try { response = await fetch(base + path, { ...options, method, headers, body, credentials: 'include' }); }
        catch { throw new Error('Não foi possível conectar à API. Verifique se o backend está iniciado e a configuração de acesso.'); }
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            const error = new Error(data.message || `Falha na operação (${response.status}).`); error.status = response.status;
            if (response.status === 403) csrf = null;
            throw error;
        }
        if (options.blob) return response.blob();
        const text = await response.text(); return text ? JSON.parse(text) : null;
    }
    return { request, get: path => request(path), post: (path, body) => request(path, {method:'POST',body}), put: (path,body) => request(path,{method:'PUT',body}), delete: path => request(path,{method:'DELETE'}), reset: () => {csrf=null;} };
})();
