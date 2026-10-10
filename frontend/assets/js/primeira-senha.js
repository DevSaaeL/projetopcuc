App.start(async user=>{
 if(!user.senha_temporaria){location.replace(sessionStorage.getItem('helpdesk_after_password_change')||'dashboard.html');return;}
 const form=App.$('primeiraSenhaForm');
 form.addEventListener('submit',event=>{event.preventDefault();App.run(async()=>{
  const password=App.$('novaSenha').value,confirmation=App.$('confirmarSenha').value;
  if(password!==confirmation)throw Error('As senhas não coincidem.');
  if(!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,72}$/.test(password))throw Error('Use pelo menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial.');
  if(new TextEncoder().encode(password).length>72)throw Error('A senha não pode ultrapassar 72 bytes UTF-8.');
  const button=App.$('salvarNovaSenha');button.disabled=true;
  try{await API.put('/auth/primeira-senha',{nova:password,confirmacao:confirmation});API.reset();const destination=sessionStorage.getItem('helpdesk_after_password_change')||'dashboard.html';sessionStorage.removeItem('helpdesk_after_password_change');location.replace(destination);}
  finally{button.disabled=false;}
 });});
});