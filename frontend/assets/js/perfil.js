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

function alterarFoto(){App.$('profilePhotoInput').click();}
App.$('profilePhotoInput').addEventListener('change',event=>{
 const file=event.target.files[0];if(!file)return;
 App.run(async()=>{
  if(!['image/jpeg','image/png'].includes(file.type)||file.size>2*1024*1024)throw Error('Escolha uma foto JPG ou PNG de até 2 MB.');
  const button=document.querySelector('.avatar-edit-button');button.disabled=true;
  App.text('photoStatus','Salvando foto…');
  try{const data=new FormData();data.append('arquivo',file);Object.assign(Auth.user,await API.post('/auth/foto',data));document.querySelectorAll('.user-avatar,#profileAvatar').forEach(e=>App.avatar(e,Auth.user));App.text('photoStatus','Foto salva com sucesso.');}
  finally{button.disabled=false;event.target.value='';if(!Auth.user.foto_versao)App.text('photoStatus','JPG ou PNG • até 2 MB');}
 });
});

/* ================================
   ALTERAR SENHA
================================ */

function abrirAlterarSenha(){App.$('alterarSenhaForm').reset();App.$('senhaErro').textContent='';bootstrap.Modal.getOrCreateInstance(App.$('alterarSenhaModal')).show();}
App.$('alterarSenhaForm').addEventListener('submit',async event=>{event.preventDefault();const b={atual:App.$('senha-atual').value,nova:App.$('senha-nova').value,confirmacao:App.$('senha-confirmacao').value};const button=event.target.querySelector('[type="submit"]');button.disabled=true;try{if(b.nova!==b.confirmacao)throw Error('As senhas não coincidem.');await API.put('/auth/senha',b);API.reset();location.replace('login.html');}catch(e){App.$('senhaErro').textContent=e.message;}finally{button.disabled=false;}});

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
 cancelarEdicao();App.text('profileName',user.nome);App.avatar(App.$('profileAvatar'),user);document.querySelector('.profile-role').textContent=App.labels[user.perfil];
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
document.querySelector('[data-event-9]')?.addEventListener('submit', function(event) { salvarPerfil(event); });
document.querySelector('[data-event-10]')?.addEventListener('click', function(event) { cancelarEdicao(); });
document.querySelector('[data-event-11]')?.addEventListener('change', function(event) { salvarPreferencia('Notificações do sistema', this.checked); });
document.querySelector('[data-event-12]')?.addEventListener('change', function(event) { salvarPreferencia('Som das notificações', this.checked); });
document.querySelector('[data-event-13]')?.addEventListener('change', function(event) { salvarPreferencia('Notificações por e-mail', this.checked); });
document.querySelector('[data-event-14]')?.addEventListener('change', function(event) { salvarPreferencia('Alertas de SLA', this.checked); });
document.querySelector('[data-event-15]')?.addEventListener('click', function(event) { verAtividades(); });
document.querySelector('[data-event-16]')?.addEventListener('click', function(event) { verSessoes(); });
document.querySelector('[data-event-17]')?.addEventListener('click', function(event) { encerrarSessoes(); });
