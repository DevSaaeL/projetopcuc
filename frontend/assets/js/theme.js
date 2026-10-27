(()=>{
 const key='helpdesk_theme',root=document.documentElement,media=window.matchMedia?.('(prefers-color-scheme: dark)');
 let choice='system';try{const saved=localStorage.getItem(key);if(['light','dark','system'].includes(saved))choice=saved;}catch{}
 function apply(){const theme=choice==='system'?(media?.matches?'dark':'light'):choice;root.dataset.theme=theme;root.dataset.bsTheme=theme;root.style.colorScheme=theme;const select=document.getElementById('themeSelect');if(select)select.value=choice;}
 apply();
 function mount(){
  if(document.getElementById('themeSelect'))return;
  const box=document.createElement('div');box.className='theme-control';const label=document.createElement('label');label.htmlFor='themeSelect';label.textContent='Aparência';
  const select=document.createElement('select');select.id='themeSelect';select.className='form-select form-select-sm';select.setAttribute('aria-label','Aparência');
  for(const [value,text] of [['light','Claro'],['dark','Escuro'],['system','Sistema']]){const option=document.createElement('option');option.value=value;option.textContent=text;select.append(option);}
  select.value=choice;select.addEventListener('change',()=>{choice=select.value;try{localStorage.setItem(key,choice);}catch{}apply();});box.append(label,select);
  const target=document.querySelector('.topbar .user-area')||document.querySelector('.topbar')||document.querySelector('header');
  if(target)target.append(box);else{box.classList.add('theme-floating');document.body.append(box);}
 }
 media?.addEventListener('change',()=>{if(choice==='system')apply();});
 window.addEventListener('storage',event=>{if(event.key===key){choice=['light','dark','system'].includes(event.newValue)?event.newValue:'system';apply();}});
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();
})();
