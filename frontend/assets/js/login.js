document.addEventListener('DOMContentLoaded', async () => {
 const form=document.getElementById('loginForm'),alert=document.getElementById('loginAlert');
 const show=message=>{alert.classList.remove('d-none');alert.querySelector('span').textContent=message;};
 const remembered=localStorage.getItem('helpdesk_login_email');if(remembered){form.elements.username.value=remembered;document.getElementById('remember').checked=true;}
 document.getElementById('microsoftLogin').addEventListener('click',()=>sessionStorage.setItem('helpdesk_sso_destination',Auth.destination()));
 document.getElementById('passwordToggle').addEventListener('click',()=>{const field=form.elements.password;field.type=field.type==='password'?'text':'password';});
 form.addEventListener('submit',async event=>{event.preventDefault();if(!form.reportValidity())return;const button=document.getElementById('loginButton');button.disabled=true;
  try{const username=form.elements.username.value.trim();const login=await API.post('/auth/login',{username,password:form.elements.password.value});API.reset();if(login.mustChangePassword){sessionStorage.setItem('helpdesk_after_password_change',Auth.destination());location.replace('primeira-senha.html');return;}if(document.getElementById('remember').checked)localStorage.setItem('helpdesk_login_email',username);else localStorage.removeItem('helpdesk_login_email');location.replace(Auth.destination());}
  catch(error){show(error.message);}finally{button.disabled=false;}
 });
 try{
  const providers=await API.get('/auth/providers');
  document.getElementById('microsoftLogin').classList.toggle('d-none',!providers.microsoftEnabled);
  document.getElementById('authStatus').textContent=providers.microsoftEnabled?'Novos técnicos: use a senha temporária recebida no cadastro. Contas técnicas existentes e administradores: entre com a Microsoft.':'Técnicos e administradores podem entrar com e-mail e senha.';
  const result=new URLSearchParams(location.search).get('sso');
  if(result==='success'){await API.get('/auth/me');const destination=sessionStorage.getItem('helpdesk_sso_destination');sessionStorage.removeItem('helpdesk_sso_destination');if(destination){const query=new URLSearchParams(location.search);query.set('returnTo',destination);history.replaceState(null,'',location.pathname+'?'+query.toString());}location.replace(Auth.destination());}
  else if(result==='requester')show('Solicitantes abrem chamados lendo o QR Code. O acesso fica disponível para técnicos e administradores.');
  else if(result==='local')show('Técnicos devem entrar com o e-mail e a senha temporária recebida no cadastro.');
  else if(result==='unassigned')show('Conta sem acesso. Solicite ao administrador o vínculo da conta Microsoft e dos locais de atuação.');
  else if(result==='failed')show('Não foi possível concluir o login Microsoft. Tente novamente ou contate o administrador.');
 }catch(error){show(error.message);}
});
