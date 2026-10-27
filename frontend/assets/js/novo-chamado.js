// Compatibilidade com os QR Codes já impressos, que usam este endereço.
if(location.protocol!=='file:'){
 const query=new URLSearchParams(location.search);
 location.replace(query.has('qr')?'solicitar.html'+location.search+location.hash:'chamados.html');
}