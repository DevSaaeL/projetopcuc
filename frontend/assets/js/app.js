window.LiveUpdates = (() => {
    const refreshers = new Set();
    let user, timer, running = false, stopped = false, initialized = false, highWater = 0;
    let audio, pendingBell = false;
    function preferences() { try { return JSON.parse(user.preferencias || '{}'); } catch { return {}; } }
    function storedId() { try { return Number(localStorage.getItem('helpdesk_notifications_' + user.id)) || 0; } catch { return 0; } }
    function remember(id) { highWater = id; try { localStorage.setItem('helpdesk_notifications_' + user.id, String(id)); } catch {} }
    function bell() {
        if (preferences().notificationSound === false) return;
        if (!audio || audio.state !== 'running') { pendingBell = true; return; }
        pendingBell = false;
        // Two decaying tones make a bell without downloading an audio file.
        for (const [offset, frequency] of [[0, 880], [0.22, 1174]]) {
            const gain = audio.createGain(), tone = audio.createOscillator();
            const start = audio.currentTime + offset;
            tone.type = 'sine'; tone.frequency.value = frequency;
            gain.gain.setValueAtTime(0.001, start);
            gain.gain.exponentialRampToValueAtTime(0.2, start + 0.01);
            gain.gain.exponentialRampToValueAtTime(0.001, start + 0.9);
            tone.connect(gain); gain.connect(audio.destination);
            tone.start(start); tone.stop(start + 0.95);
            tone.onended = () => { tone.disconnect(); gain.disconnect(); };
        }
    }
    async function enableSound(event) {
        if (!event.isTrusted || preferences().notificationSound === false) return;
        try {
            const Audio = window.AudioContext || window.webkitAudioContext;
            if (!Audio) return;
            audio ||= new Audio();
            await audio.resume();
            if (audio.state === 'running') {
                if (pendingBell) bell();
            }
        } catch { /* The popup remains available if sound is blocked. */ }
    }
    function popup(notifications) {
        if (preferences().systemNotifications === false) return;
        let container = document.getElementById('liveTicketAlerts');
        if (!container) {
            container = document.createElement('div'); container.id = 'liveTicketAlerts';
            container.className = 'hd-live-alerts'; container.setAttribute('aria-live', 'assertive');
            document.body.append(container);
        }
        const latest = notifications[0];
        const alert = document.createElement('div'); alert.className = 'hd-live-alert'; alert.setAttribute('role', 'alert');
        const title = document.createElement('strong');
        title.textContent = notifications.length === 1 ? 'Novo chamado recebido' : notifications.length + ' novos chamados recebidos';
        const link = document.createElement('a'); link.href = 'chamado-detalhes.html?id=' + encodeURIComponent(latest.chamado_id);
        link.textContent = 'Ver chamado #' + String(latest.chamado_id).padStart(6, '0');
        const close = document.createElement('button'); close.type = 'button'; close.textContent = '×';
        close.setAttribute('aria-label', 'Fechar aviso de novo chamado'); close.addEventListener('click', () => alert.remove());
        alert.append(title, link, close); container.append(alert);
        while (container.children.length > 3) container.firstElementChild.remove();
    }
    function consume(notifications) {
        const count = notifications.filter(n => !n.lida).length;
        document.querySelectorAll('.notification-count,.notification-badge,.menu-item[href="notificacoes.html"] .menu-badge').forEach(e => e.textContent = count);
        document.querySelectorAll('.notification-dot').forEach(e => e.hidden = !count);
        const threshold = Math.max(highWater, storedId());
        const max = Math.max(threshold, ...notifications.map(n => Number(n.id)));
        const fresh = initialized ? notifications.filter(n => Number(n.id) > threshold && !n.lida && n.tipo === 'chamado' && n.titulo === 'Novo chamado') : [];
        remember(max); initialized = true;
        document.dispatchEvent(new CustomEvent('helpdesk:notifications', {detail: notifications}));
        if (Auth.support() && fresh.length) { popup(fresh); bell(); }
    }
    function expired(error) {
        if (error?.status !== 401) return false;
        stopped = true; clearTimeout(timer);
        location.replace('login.html?returnTo=' + encodeURIComponent(location.pathname.split('/').pop() + location.search));
        return true;
    }
    async function tick() {
        if (running || stopped) return;
        clearTimeout(timer); running = true;
        try {
            consume(await API.get('/notificacoes'));
            if (!document.hidden) {
                const results = await Promise.allSettled([...refreshers].map(fn => Promise.resolve().then(fn)));
                results.forEach(result => { if (result.status === 'rejected') expired(result.reason); });
            }
        } catch (error) { expired(error); }
        finally { running = false; if (!stopped) timer = setTimeout(tick, 5000); }
    }
    function start(account) {
        if (user) return;
        user = account;
        if (Auth.support()) {
            document.addEventListener('pointerdown', enableSound);
            document.addEventListener('keydown', enableSound);
        }
        document.addEventListener('visibilitychange', () => { if (!document.hidden) tick(); });
        window.addEventListener('online', tick);
        tick();
    }
    return {start, subscribe(fn) { refreshers.add(fn); return () => refreshers.delete(fn); }};
})();

window.App = (() => {
    const $ = id => document.getElementById(id);
    const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    const labels = {MASTER_ADMIN:'Administrador mestre',ADMIN:'Administrador',SUPORTE:'Técnico de TI',USUARIO:'Usuário',ABERTO:'Aberto',EM_ATENDIMENTO:'Em atendimento',AGUARDANDO_USUARIO:'Aguardando usuário',FINALIZADO:'Finalizado',CANCELADO:'Cancelado',BAIXA:'Baixa',MEDIA:'Média',ALTA:'Alta',CRITICA:'Crítica'};
    const statusClass = {ABERTO:'open',EM_ATENDIMENTO:'progress',AGUARDANDO_USUARIO:'waiting',FINALIZADO:'finished',CANCELADO:'cancelled'};
    const priorityClass = {BAIXA:'low',MEDIA:'medium',ALTA:'high',CRITICA:'critical'};
    function error(e) { let box=$('apiError'); if(!box){box=document.createElement('div');box.id='apiError';box.className='alert alert-danger m-3';box.setAttribute('role','alert');(document.querySelector('main')||document.body).prepend(box);}box.textContent=e.message||String(e);box.hidden=false; }
    function clearError(){if($('apiError'))$('apiError').hidden=true;}
    async function run(fn){clearError();try{return await fn();}catch(e){error(e);}}
    function start(fn){const go=()=>run(async()=>{const user=await Auth.ready;if(user)await fn(user);});if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',go);else go();}
    function text(id,value){if($(id))$(id).textContent=value??'';}
    function date(value){return value?new Date(value).toLocaleString('pt-BR'):'—';}
    function duration(s){s=Math.max(0,Math.floor(s||0));return `${Math.floor(s/3600).toString().padStart(2,'0')}:${Math.floor(s/60)%60<10?'0':''}${Math.floor(s/60)%60}:${(s%60).toString().padStart(2,'0')}`;}
    function options(id,items,valueKey,labelKey,placeholder='Selecione'){const select=$(id);if(!select)return;select.replaceChildren(new Option(placeholder,''),...items.map(v=>new Option(v[labelKey],v[valueKey])));}
    function saveFile(blob,name){const link=document.createElement('a');const url=URL.createObjectURL(blob);link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
    function exportRows(rows,type='csv'){
        const fields=['protocolo','titulo','solicitante','filial_nome','cidade','bloco','sala','tecnico','prioridade','status','abertura','atendimento','finalizacao'];
        const names=['Protocolo','Título','Solicitante','Filial','Cidade','Bloco','Sala','Técnico','Prioridade','Status','Abertura','Atendimento','Finalização'];
        if(type.toLowerCase()==='pdf'){const w=window.open('','_blank');if(!w)throw Error('Permita a janela de impressão.');w.document.write('<!doctype html><html lang="pt-BR"><title>Chamados</title><link rel="stylesheet" href="'+new URL('../assets/css/impressao.css',location.href).href+'"><body><h1>Chamados</h1><table><thead><tr>'+names.map(n=>'<th>'+esc(n)+'</th>').join('')+'</tr></thead><tbody>'+rows.map(r=>'<tr>'+fields.map(k=>'<td>'+esc(r[k])+'</td>').join('')+'</tr>').join('')+'</tbody></table></body></html>');w.document.close();w.onload=()=>w.print();return;}
        // CSV UTF-8 é compatível com Excel; neutraliza fórmulas em valores fornecidos pelo usuário.
        const cell=v=>'"'+String(v??'').replace(/^[=+@\-\t\r]/,m=>"'"+m).replace(/"/g,'""')+'"';
        const csv=[names,...rows.map(r=>fields.map(k=>r[k]))].map(row=>row.map(cell).join(';')).join('\r\n');saveFile(new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'}),'chamados.csv');
    }
    function initials(name){return name.split(/\s+/).filter(Boolean).map(n=>n[0]).slice(0,2).join('').toUpperCase();}
    function avatar(element,user){
        if(!element)return;
        element.textContent=initials(user.nome);
        if(!user.foto_versao)return;
        const image=document.createElement('img');
        image.alt='Foto de '+user.nome;
        image.src=(window.HELPDESK_CONFIG?.apiBase||'').replace(/\/$/,'')+'/api/auth/foto?v='+encodeURIComponent(user.foto_versao);
        image.addEventListener('error',()=>{element.textContent=initials(user.nome);});
        element.replaceChildren(image);
    }
    start(async user=>{
        if(user.senha_temporaria)return;
        document.querySelectorAll('.user-avatar').forEach(e=>avatar(e,user));
        document.querySelectorAll('.topbar .user-name,.topbar .fw-semibold,.user-menu-info strong,.sidebar-user .user-info strong').forEach(e=>e.textContent=user.nome);
        document.querySelectorAll('.topbar .user-role,.topbar .text-muted,.user-menu-info small,.sidebar-user .user-info small').forEach(e=>e.textContent=labels[user.perfil]);
        document.querySelectorAll('.sidebar a').forEach(a=>{if(['usuarios.html','filiais.html','qr-codes.html'].includes(a.getAttribute('href')))a.hidden=a.getAttribute('href')==='filiais.html'?user.perfil!=='MASTER_ADMIN':!Auth.admin();});
        if(user.perfil==='MASTER_ADMIN')API.get('/filiais').then(locais=>{if(!locais.length){const banner=document.createElement('div');banner.className='alert alert-info m-3';banner.innerHTML='Primeiro acesso: cadastre manualmente as faculdades e unidades antes de cadastrar os técnicos. <a href="filiais.html">Cadastrar locais</a>';document.querySelector('main, .main-content')?.prepend(banner);}}).catch(error=>App.error(error));
        LiveUpdates.start(user);
    });
    document.addEventListener('DOMContentLoaded',()=>{
        document.querySelectorAll('.password-toggle:not([data-password-toggle])').forEach(button=>button.addEventListener('click',()=>{const input=button.parentElement.querySelector('input');if(!input)return;input.type=input.type==='password'?'text':'password';button.innerHTML='<i class="bi bi-eye'+(input.type==='text'?'-slash':'')+'"></i>';button.setAttribute('aria-label',input.type==='text'?'Ocultar senha':'Mostrar senha');}));
        document.querySelectorAll('.mobile-menu-button').forEach(button=>button.addEventListener('click',toggleSidebar));
        document.querySelectorAll('.notification-button').forEach(button=>button.addEventListener('click',()=>location.href='notificacoes.html'));
        document.querySelectorAll('.user-profile').forEach(button=>{button.tabIndex=0;button.setAttribute('role','link');button.addEventListener('click',()=>location.href='perfil.html');button.addEventListener('keydown',e=>{if(e.key==='Enter')location.href='perfil.html';});});
    });
    return {$,esc,labels,statusClass,priorityClass,error,clearError,run,start,text,date,duration,options,saveFile,exportRows,initials,avatar};
})();
function toggleSidebar(){document.querySelector('.sidebar')?.classList.toggle('show');document.querySelector('.sidebar-overlay')?.classList.toggle('show');}
function logout(){if(confirm('Deseja sair do sistema?'))return App.run(()=>Auth.logout());}

document.addEventListener('DOMContentLoaded',()=>{
 if(!document.querySelector('.sidebar'))return;
 if(!document.querySelector('.mobile-menu-button,.menu-toggle')){const b=document.createElement('button');b.type='button';b.className='menu-toggle hd-menu-button';b.setAttribute('aria-label','Abrir menu');b.innerHTML='<i class="bi bi-list"></i>';b.addEventListener('click',toggleSidebar);document.querySelector('.topbar > div')?.prepend(b);}
 if(!document.querySelector('.sidebar-overlay')){const overlay=document.createElement('div');overlay.className='sidebar-overlay';overlay.addEventListener('click',toggleSidebar);document.body.append(overlay);}
 document.addEventListener('keydown',event=>{if(event.key==='Escape'){document.querySelector('.sidebar').classList.remove('show');document.querySelector('.sidebar-overlay').classList.remove('show');}});
});
