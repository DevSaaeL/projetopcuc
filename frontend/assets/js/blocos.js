let branchBlocks=[],editingBlock=null,savingBlock=false;
function resetBlock(){editingBlock=null;App.$('blockName').value='';App.$('blockActive').checked=true;App.$('blockBranch').disabled=false;App.$('cancelBlock').hidden=true;App.text('blockFormTitle','Cadastrar bloco');App.text('saveBlock','Cadastrar bloco');}
async function loadBlocks(){
 const branch=App.$('blockBranch').value;branchBlocks=branch?await API.get('/blocos?filial_id='+encodeURIComponent(branch)):[];
 App.text('blockCount',branchBlocks.length+' bloco(s)');App.$('blockList').innerHTML=branchBlocks.map(block=>`<div class="topic-admin-row"><div><strong>${App.esc(block.nome)}</strong><small>${block.ativo?'Ativo':'Desativado'}</small></div><button class="btn btn-sm btn-outline-secondary" type="button" data-edit-block="${block.id}">Editar</button></div>`).join('')||'<p class="topic-admin-empty">Nenhum bloco cadastrado nesta filial.</p>';
}
App.start(async()=>{
 App.options('blockBranch',await API.get('/filiais'),'id','nome','Selecione a filial');
 App.$('blockBranch').addEventListener('change',()=>{resetBlock();App.run(loadBlocks);});App.$('cancelBlock').addEventListener('click',resetBlock);
 App.$('blockList').addEventListener('click',event=>{const button=event.target.closest('[data-edit-block]');if(!button||savingBlock)return;const block=branchBlocks.find(block=>block.id===Number(button.dataset.editBlock));editingBlock=block.id;App.$('blockName').value=block.nome;App.$('blockActive').checked=block.ativo;App.$('blockBranch').disabled=true;App.$('cancelBlock').hidden=false;App.text('blockFormTitle','Editar bloco');App.text('saveBlock','Salvar alterações');App.$('blockName').focus();});
 App.$('blockForm').addEventListener('submit',event=>{event.preventDefault();if(savingBlock)return;App.run(async()=>{savingBlock=true;App.$('saveBlock').disabled=true;try{const body={filialId:Number(App.$('blockBranch').value),nome:App.$('blockName').value.trim(),ativo:App.$('blockActive').checked};if(editingBlock)await API.put('/blocos/'+editingBlock,body);else await API.post('/blocos',body);resetBlock();await loadBlocks();App.text('blockFeedback','Bloco salvo. Configure os responsáveis na aba Usuários.');}finally{savingBlock=false;App.$('saveBlock').disabled=false;}});});
 await loadBlocks();
});
