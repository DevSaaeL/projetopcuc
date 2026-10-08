document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const loginTab = document.getElementById('loginTab');
    const registerTab = document.getElementById('registerTab');
    const loginAlert = document.getElementById('loginAlert');
    const registerAlert = document.getElementById('registerAlert');
    const remembered = localStorage.getItem('helpdesk_login_email');

    if (remembered) {
        loginForm.elements.username.value = remembered;
        document.getElementById('remember').checked = true;
    }

    function showAlert(box, message, type = 'danger') {
        box.classList.remove('d-none', 'alert-danger', 'alert-success', 'alert-warning');
        box.classList.add(`alert-${type}`);
        box.querySelector('span').textContent = message;
    }

    function hideAlerts() {
        loginAlert.classList.add('d-none');
        registerAlert.classList.add('d-none');
    }

    function switchMode(mode) {
        const isLogin = mode === 'login';
        loginForm.classList.toggle('d-none', !isLogin);
        registerForm.classList.toggle('d-none', isLogin);
        loginTab.classList.toggle('active', isLogin);
        registerTab.classList.toggle('active', !isLogin);
        loginTab.setAttribute('aria-selected', String(isLogin));
        registerTab.setAttribute('aria-selected', String(!isLogin));
        hideAlerts();
        (isLogin ? loginForm.elements.username : registerForm.elements.email).focus();
    }

    function wirePasswordToggle(inputId, buttonId) {
        const input = document.getElementById(inputId);
        const button = document.getElementById(buttonId);
        button.addEventListener('click', () => {
            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            button.setAttribute('aria-label', visible ? 'Mostrar senha' : 'Ocultar senha');
            button.querySelector('i').classList.toggle('bi-eye', visible);
            button.querySelector('i').classList.toggle('bi-eye-slash', !visible);
        });
    }


    loginTab.addEventListener('click', () => switchMode('login'));
    registerTab.addEventListener('click', () => switchMode('register'));

    loginForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (!loginForm.reportValidity()) return;
        const button = document.getElementById('loginButton');
        button.disabled = true;
        hideAlerts();
        document.getElementById('loginButtonText').classList.add('d-none');
        document.getElementById('loginLoading').classList.remove('d-none');
        try {
            const username = loginForm.elements.username.value.trim();
            await API.post('/auth/login', { username, password: loginForm.elements.password.value });
            API.reset();
            if (document.getElementById('remember').checked) localStorage.setItem('helpdesk_login_email', username);
            else localStorage.removeItem('helpdesk_login_email');
            location.replace(Auth.destination());
        } catch (error) {
            showAlert(loginAlert, error.message);
        } finally {
            button.disabled = false;
            document.getElementById('loginButtonText').classList.remove('d-none');
            document.getElementById('loginLoading').classList.add('d-none');
        }
    });

    registerForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (!registerForm.reportValidity()) return;
        const email = registerForm.elements.email.value.trim().toLowerCase();
        const password = registerForm.elements.senha.value;
        const confirmation = registerForm.elements.confirmacao.value;
        if (password !== confirmation) {
            showAlert(registerAlert, 'As senhas não coincidem.');
            return;
        }
        const button = document.getElementById('registerButton');
        button.disabled = true;
        hideAlerts();
        document.getElementById('registerButtonText').classList.add('d-none');
        document.getElementById('registerLoading').classList.remove('d-none');
        try {
            await API.post('/auth/register', { email, senha: password });
            registerForm.reset();
            loginForm.elements.username.value = email;
            switchMode('login');
            showAlert(loginAlert, 'Conta criada com sucesso. Agora entre com seu e-mail e senha.', 'success');
            loginForm.elements.password.focus();
        } catch (error) {
            showAlert(registerAlert, error.message);
        } finally {
            button.disabled = false;
            document.getElementById('registerButtonText').classList.remove('d-none');
            document.getElementById('registerLoading').classList.add('d-none');
        }
    });

    document.getElementById('forgotPassword').addEventListener('click', event => {
        event.preventDefault();
        showAlert(loginAlert, 'Solicite ao administrador da sua unidade a redefinição da senha.', 'warning');
    });
});

