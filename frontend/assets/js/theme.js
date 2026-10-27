(()=>{
 const key='helpdesk_theme',root=document.documentElement,media=window.matchMedia?.('(prefers-color-scheme: dark)');
 const choices={light:['Claro','bi-sun'],dark:['Escuro','bi-moon-stars'],system:['Sistema','bi-display']};
 let choice='system',control,trigger,panel;
 try{const saved=localStorage.getItem(key);if(choices[saved])choice=saved;}catch{}
 function apply(){const theme=choice==='system'?(media?.matches?'dark':'light'):choice;root.dataset.theme=theme;root.dataset.bsTheme=theme;root.style.colorScheme=theme;if(trigger){trigger.querySelector('i').className='bi '+choices[choice][1];trigger.querySelector('span').textContent=choices[choice][0];trigger.setAttribute('aria-label','Aparência: '+choices[choice][0]+'. Alterar tema');panel.querySelectorAll('[data-theme-choice]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.themeChoice===choice)));}}
 function close(restoreFocus=false){if(!panel)return;panel.hidden=true;trigger.setAttribute('aria-expanded','false');if(restoreFocus)trigger.focus();}
 apply();
 function mount(){
  if(document.getElementById('themeToggle'))return;
  control=document.createElement('div');control.className='theme-control';
  trigger=document.createElement('button');trigger.id='themeToggle';trigger.type='button';trigger.className='theme-toggle';trigger.setAttribute('aria-expanded','false');trigger.setAttribute('aria-controls','themeOptions');trigger.innerHTML='<i aria-hidden="true"></i><span></span>';
  panel=document.createElement('div');panel.id='themeOptions';panel.className='theme-options';panel.hidden=true;panel.setAttribute('role','group');panel.setAttribute('aria-label','Escolha a aparência');
  const title=document.createElement('strong');title.className='theme-options-title';title.textContent='Aparência';panel.append(title);
  for(const [value,[label,icon]] of Object.entries(choices)){const button=document.createElement('button');button.type='button';button.dataset.themeChoice=value;button.innerHTML='<i class="bi '+icon+'" aria-hidden="true"></i><span>'+label+'</span><i class="bi bi-check2 theme-selected" aria-hidden="true"></i>';button.addEventListener('click',()=>{choice=value;try{localStorage.setItem(key,choice);}catch{}apply();close(true);});panel.append(button);}
  trigger.addEventListener('click',()=>{const opening=panel.hidden;panel.hidden=!opening;trigger.setAttribute('aria-expanded',String(opening));if(opening)panel.querySelector('[data-theme-choice="'+choice+'"]').focus();});
  control.append(trigger,panel);const target=document.querySelector('.topbar .user-area')||document.querySelector('.topbar')||document.querySelector('header');if(target)target.append(control);else{control.classList.add('theme-floating');document.body.append(control);}
  document.addEventListener('click',event=>{if(!control.contains(event.target))close();});control.addEventListener('keydown',event=>{if(event.key==='Escape'&&!panel.hidden){event.preventDefault();close(true);}});control.addEventListener('focusout',event=>{if(!control.contains(event.relatedTarget))close();});apply();
 }
 media?.addEventListener('change',()=>{if(choice==='system')apply();});window.addEventListener('storage',event=>{if(event.key===key||event.key===null){choice=choices[event.newValue]?event.newValue:'system';apply();}});
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();
})();
