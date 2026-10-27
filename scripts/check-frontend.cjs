// Auditoria local, sem dependências ou acesso à rede.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../frontend'),files=[];
function walk(dir){for(const entry of fs.readdirSync(dir,{withFileTypes:true})){const file=path.join(dir,entry.name);if(entry.isDirectory())walk(file);else files.push(file);}}
walk(root);const sources=files.filter(file=>/\.(html|css|js)$/.test(file)),errors=[];
const content=new Map(sources.map(file=>[file,fs.readFileSync(file,'utf8')]));
for(const file of sources){const text=content.get(file);
 if(file.endsWith('.js'))try{new vm.Script(text,{filename:file});}catch(error){errors.push(error.message);}
 if(!file.endsWith('.html'))continue;
 const ids=new Set();for(const match of text.matchAll(/\bid="([^"]+)"/g)){if(ids.has(match[1]))errors.push(path.relative(root,file)+': ID duplicado '+match[1]);ids.add(match[1]);}
 for(const match of text.matchAll(/\b(?:src|href)="([^"#?]+)(?:[?#][^"]*)?"/g)){
  const ref=match[1];if(/^(?:[a-z][a-z0-9+.-]*:|\/\/)/i.test(ref))continue;
  const target=ref.startsWith('/')?path.join(root,ref):path.resolve(path.dirname(file),ref);
  if(!fs.existsSync(target))errors.push(path.relative(root,file)+': referência inexistente '+ref);
 }
}
for(const file of sources.filter(file=>/\.(css|js)$/.test(file)&&!file.includes(path.sep+'vendor'+path.sep))){
 const name=path.basename(file);if(!sources.some(other=>other!==file&&content.get(other).includes(name)))errors.push('Sem referência: '+path.relative(root,file));
}
if(errors.length){console.error(errors.join('\n'));process.exitCode=1;}
else console.log(`Frontend validado: ${sources.filter(file=>file.endsWith('.html')).length} páginas, ${sources.filter(file=>file.endsWith('.js')).length} scripts e ${sources.filter(file=>file.endsWith('.css')).length} estilos; sintaxe, IDs e referências válidos.`);
