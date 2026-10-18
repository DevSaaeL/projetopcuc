window.HELPDESK_CONFIG = {
    apiBase: ''
};

// A API e os cookies de sessão precisam da página servida pelo backend.
if (window.location.protocol === 'file:') {
    const page = window.location.pathname.match(/\/pages\/([^/]+\.html)$/)?.[1] || 'login.html';
    window.location.replace('http://localhost:8080/pages/' + page + window.location.search + window.location.hash);
}
