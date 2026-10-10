App.start(async()=>{
 let qrId=null;const form=App.$('ticketForm');
 QrCamera.mount(async id=>{const qr=await API.get('/qrcodes/'+encodeURIComponent(id));qrId=id;const label=App.$('qrLocationInfo');label.hidden=false;label.textContent=[qr.filial_nome,qr.cidade,'Bloco '+qr.bloco,'Sala '+qr.sala].join(' · ');});
 for(const id of ['titulo','descricao'])App.$(id).addEventListener('input',()=>App.text(id+'Counter',App.$(id).value.length));
 App.$('btnCancelar').addEventListener('click',()=>location.href='chamados.html');
 form.addEventListener('submit',event=>{event.preventDefault();App.run(async()=>{if(!qrId)throw Error('Leia o QR Code do local antes de abrir o chamado.');const button=form.querySelector('[type="submit"]');button.disabled=true;try{const result=await API.post('/chamados',{titulo:App.$('titulo').value.trim(),descricao:App.$('descricao').value.trim(),qrId});QrCamera.stop();App.text('numeroChamado',result.protocolo);bootstrap.Modal.getOrCreateInstance(App.$('modalSucesso')).show();App.$('btnVerChamado').onclick=()=>location.href='chamado-detalhes.html?id='+result.id;App.$('btnNovoChamado').onclick=()=>location.reload();}finally{button.disabled=false;}});});
});
