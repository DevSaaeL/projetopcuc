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

function abrirAlterarSenha() {

    const modal =
        new bootstrap.Modal(
            document.getElementById(
                "passwordModal"
            )
        );


    modal.show();

}


/* ================================
   TOGGLE SENHA
================================ */

function togglePassword(
    inputId,
    button
) {

    const input =
        document.getElementById(
            inputId
        );


    const icon =
        button.querySelector("i");


    if (
        input.type === "password"
    ) {

        input.type = "text";

        icon.classList.remove(
            "bi-eye"
        );

        icon.classList.add(
            "bi-eye-slash"
        );

    } else {

        input.type = "password";

        icon.classList.remove(
            "bi-eye-slash"
        );

        icon.classList.add(
            "bi-eye"
        );

    }

}


/* ================================
   FORÇA DA SENHA
================================ */

function verificarSenha() {

    const password =
        document.getElementById(
            "newPassword"
        ).value;


    const bar =
        document.getElementById(
            "strengthBar"
        );


    const text =
        document.getElementById(
            "strengthText"
        );


    let strength = 0;


    if (password.length >= 8) {

        strength += 25;

    }


    if (password.length >= 12) {

        strength += 25;

    }


    if (/[A-Z]/.test(password)) {

        strength += 15;

    }


    if (/[0-9]/.test(password)) {

        strength += 15;

    }


    if (/[^A-Za-z0-9]/.test(password)) {

        strength += 20;

    }


    if (strength > 100) {

        strength = 100;

    }


    bar.style.width =
        strength + "%";


    if (!password) {

        text.textContent =
            "Digite uma senha.";

    } else if (strength < 40) {

        text.textContent =
            "Senha fraca.";

    } else if (strength < 70) {

        text.textContent =
            "Senha média.";

    } else {

        text.textContent =
            "Senha forte.";

    }

}


/* ================================
   ALTERAR SENHA
================================ */

function alterarSenha(event){event.preventDefault();App.run(async()=>{if(App.$('newPassword').value!==App.$('confirmPassword').value)throw Error('As senhas não coincidem.');await API.put('/auth/senha',{atual:App.$('currentPassword').value,nova:App.$('newPassword').value});location.replace('login.html');});}


/* ================================
   MFA
================================ */

function configurarMFA() {

    const modal =
        new bootstrap.Modal(
            document.getElementById(
                "mfaModal"
            )
        );


    modal.show();

}


function ativarMFA() {

    alert(
        "A ativação do MFA será integrada ao backend posteriormente."
    );

}


/* ================================
   PREFERÊNCIAS
================================ */

function salvarPreferencia(){App.run(async()=>{const data={};for(const id of ['systemNotifications','notificationSound','emailNotifications','slaNotifications'])data[id]=App.$(id).checked;await API.put('/auth/preferencias',data);});}


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
 cancelarEdicao();App.text('profileName',user.nome);App.text('profileAvatar',App.initials(user.nome));document.querySelector('.profile-role').textContent=App.labels[user.perfil];
 const vals=[user.email,user.filial_nome||'Todas as unidades',user.setor||'—',App.date(user.criado_em)];document.querySelectorAll('.profile-info-item strong').forEach((e,i)=>e.textContent=vals[i]);
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
document.querySelector('[data-event-18]')?.addEventListener('submit', function(event) { alterarSenha(event); });
document.querySelector('[data-event-19]')?.addEventListener('click', function(event) { togglePassword('currentPassword', this); });
document.querySelector('[data-event-20]')?.addEventListener('input', function(event) { verificarSenha(); });
document.querySelector('[data-event-21]')?.addEventListener('click', function(event) { togglePassword('newPassword', this); });
document.querySelector('[data-event-22]')?.addEventListener('click', function(event) { togglePassword('confirmPassword', this); });
document.querySelector('[data-event-23]')?.addEventListener('click', function(event) { ativarMFA(); });
