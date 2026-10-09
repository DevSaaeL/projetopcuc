window.Auth = (() => {
    let user;
    const page = location.pathname.split('/').pop();
    function loginUrl() { return 'login.html?returnTo=' + encodeURIComponent(page + location.search); }
    function destination() {
        const value = new URLSearchParams(location.search).get('returnTo');
        const allowed = ['dashboard','chamados','novo-chamado','chamado-detalhes','meus-chamados','notificacoes','usuarios','filiais','qr-codes','relatorios','perfil','ler-qrcode'];
        if (value && allowed.some(name => value === name+'.html' || value.startsWith(name+'.html?'))) return value;
        return 'dashboard.html';
    }
    const qrEntry = page === 'novo-chamado.html' && new URLSearchParams(location.search).has('qr');
    if(qrEntry) location.replace('solicitar.html?qr='+encodeURIComponent(new URLSearchParams(location.search).get('qr')));
    const ready = (page === 'login.html' || qrEntry) ? Promise.resolve(null) : API.get('/auth/me').then(value => {
        user = value;
        if (['usuarios.html','filiais.html','qr-codes.html'].includes(page) && !['ADMIN','MASTER_ADMIN'].includes(user.perfil)) {
            location.replace('meus-chamados.html'); return null;
        }
        return user;
    }).catch(error => { if(error.status===401) location.replace(loginUrl()); else setTimeout(()=>window.App?.error(error),0); return null; });
    async function logout() { await API.post('/auth/logout'); API.reset(); location.replace('login.html'); }
    return {ready, destination, logout, get user(){return user;}, admin:()=>['ADMIN','MASTER_ADMIN'].includes(user?.perfil), support:()=>['ADMIN','MASTER_ADMIN','SUPORTE'].includes(user?.perfil)};
})();
