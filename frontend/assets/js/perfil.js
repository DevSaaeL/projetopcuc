/* ================================
   SIDEBAR
================================ */




/* ================================
   SALVAR PERFIL
================================ */

function salvarPerfil(event){event.preventDefault();App.run(async()=>{await API.put('/auth/perfil',{nome:App.$('name').value.trim(),email:App.$('email').value.trim(),telefone:App.$('phone').value.trim()});location.reload();});}


/* ================================
   CANCELAR
================================ */

function cancelarEdicao(){App.$('name').value=Auth.user.nome;App.$('email').value=Auth.user.email;App.$('phone').value=Auth.user.telefone;}


/* ================================
   FOTO
================================ */

function alterarFoto() {

    alert(
        "O envio da foto será conectado ao backend posteriormente."
    );

}


/* ================================
   ALTERAR SENHA
================================ */

function abrirAlterarSenha(){window.open('https://myaccount.microsoft.com','_blank','noopener,noreferrer');}
function configurarMFA(){window.open('https://mysignins.microsoft.com/security-info','_blank','noopener,noreferrer');}

function salvarPreferencia(){App.run(async()=>{const data={};for(const id of ['systemNotifications','notificationSound','emailNotifications','slaNotifications'])data[id]=App.$(id).checked;await API.put('/auth/preferencias',data);Auth.user.preferencias=JSON.stringify(data);});}


/* ================================
   ATIVIDADES
================================ */

function verAtividades(){App.run(async()=>{const rows=await API.get('/auth/atividades');alert(rows.map(r=>App.date(r.criado_em)+' — '+r.acao+': '+r.descricao).join('\n')||'Nenhuma atividade registrada.');});}


/* ================================
   SESSÕES
================================ */

function verSessoes(){alert('Esta sessão está ativa. Use Encerrar sessões para invalidar o acesso em todos os dispositivos.');}


/* ================================
   ENCERRAR SESSÕES
================================ */

function encerrarSessoes(){if(confirm('Encerrar todas as sessões da sua conta?'))App.run(async()=>{await API.post('/auth/encerrar-sessoes');location.replace('login.html');});}


/* ================================
   LOGOUT
================================ */



App.start(async user=>{
 if(user.microsoft_object_id)App.$('email').readOnly=true;
 cancelarEdicao();App.text('profileName',user.nome);App.text('profileAvatar',App.initials(user.nome));document.querySelector('.profile-role').textContent=App.labels[user.perfil];
 const vals=[user.email,user.perfil==='MASTER_ADMIN'?'Todas as unidades':user.locais.map(f=>f.nome).join(', ')||'Sem local atribuído',user.setor||'—',App.date(user.criado_em)];document.querySelectorAll('.profile-info-item strong').forEach((e,i)=>e.textContent=vals[i]);
 const locked=document.querySelectorAll('#profileForm input:disabled');[user.email,String(user.id),App.labels[user.perfil],user.setor||'—'].forEach((v,i)=>{if(locked[i])locked[i].value=v;});
 const prefs=JSON.parse(user.preferencias||'{}');for(const [key,value] of Object.entries(prefs))if(App.$(key))App.$(key).checked=value;
});

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-1]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-3]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-4]')?.addEventListener('click', function(event) { window.location.href='notificacoes.html'; });
document.querySelector('[data-event-5]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-6]')?.addEventListener('click', function(event) { alterarFoto(); });
document.querySelector('[data-event-7]')?.addEventListener('click', function(event) { abrirAlterarSenha(); });
document.querySelector('[data-event-8]')?.addEventListener('click', function(event) { configurarMFA(); });
document.querySelector('[data-event-9]')?.addEventListener('submit', function(event) { salvarPerfil(event); });
document.querySelector('[data-event-10]')?.addEventListener('click', function(event) { cancelarEdicao(); });
document.querySelector('[data-event-11]')?.addEventListener('change', function(event) { salvarPreferencia('Notificações do sistema', this.checked); });
document.querySelector('[data-event-12]')?.addEventListener('change', function(event) { salvarPreferencia('Som das notificações', this.checked); });
document.querySelector('[data-event-13]')?.addEventListener('change', function(event) { salvarPreferencia('Notificações por e-mail', this.checked); });
document.querySelector('[data-event-14]')?.addEventListener('change', function(event) { salvarPreferencia('Alertas de SLA', this.checked); });
document.querySelector('[data-event-15]')?.addEventListener('click', function(event) { verAtividades(); });
document.querySelector('[data-event-16]')?.addEventListener('click', function(event) { verSessoes(); });
document.querySelector('[data-event-17]')?.addEventListener('click', function(event) { encerrarSessoes(); });
