document.addEventListener('DOMContentLoaded', async () => {
 const form=document.getElementById('loginForm'),alert=document.getElementById('loginAlert');
 const show=message=>{alert.classList.remove('d-none');alert.querySelector('span').textContent=message;};
 const remembered=localStorage.getItem('helpdesk_login_email');if(remembered){form.elements.username.value=remembered;document.getElementById('remember').checked=true;}
 form.addEventListener('submit',async event=>{event.preventDefault();if(!form.reportValidity())return;const button=document.getElementById('loginButton');button.disabled=true;
  try{const username=form.elements.username.value.trim();const login=await API.post('/auth/login',{username,password:form.elements.password.value});API.reset();if(login.mustChangePassword){sessionStorage.setItem('helpdesk_after_password_change',Auth.destination());location.replace('primeira-senha.html');return;}if(document.getElementById('remember').checked)localStorage.setItem('helpdesk_login_email',username);else localStorage.removeItem('helpdesk_login_email');location.replace(Auth.destination());}
  catch(error){show(error.message);}finally{button.disabled=false;}
 });

});
