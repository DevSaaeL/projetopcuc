document.querySelectorAll('[data-password-toggle]').forEach(button=>{
 button.addEventListener('click',()=>{const field=document.getElementById(button.dataset.passwordToggle);const visible=field.type==='password';field.type=visible?'text':'password';button.setAttribute('aria-pressed',String(visible));button.setAttribute('aria-label',visible?'Ocultar senha':'Mostrar senha');button.querySelector('i').className=visible?'bi bi-eye-slash':'bi bi-eye';});
});
