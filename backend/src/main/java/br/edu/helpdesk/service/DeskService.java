package br.edu.helpdesk.service;
import br.edu.helpdesk.entity.Account;
import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.dto.Requests.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
@Service
@Transactional
public class DeskService {
 private final DeskRepository r; private final PasswordEncoder encoder;
 @Value("${app.microsoft.enabled}") private boolean microsoftEnabled;
 @Value("${app.public-url}") private String publicUrl;
 @Value("${app.sla-response-minutes}") private int responseMinutes;
 @Value("${app.sla-resolution-minutes}") private int resolutionMinutes;
 private static final String USERS="SELECT u.id,u.nome,u.email,u.perfil,u.filial_id,u.ativo,u.telefone,u.setor,u.criado_em,u.preferencias,u.microsoft_object_id,f.nome filial_nome,f.cidade FROM usuarios u LEFT JOIN filiais f ON f.id=u.filial_id";
 private static final String TICKETS="SELECT c.*,u.nome solicitante,u.email solicitante_email,u.telefone solicitante_telefone,f.nome filial_nome,t.nome tecnico FROM chamados c JOIN usuarios u ON u.id=c.solicitante_id JOIN filiais f ON f.id=c.filial_id LEFT JOIN usuarios t ON t.id=c.tecnico_id";
 public DeskService(DeskRepository r,PasswordEncoder encoder){this.r=r;this.encoder=encoder;}
 public static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private static void require(boolean ok){if(!ok)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você não tem permissão para esta operação.");}
 private static Map<String,Object> exists(Map<String,Object> m){if(m==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado.");return m;}
 public Account current(){var a=SecurityContextHolder.getContext().getAuthentication();var u=a==null?null:r.account(a.getName());if(u==null||!u.ativo())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão expirada.");return u;}
 private String text(String s){return s==null?"":s.trim();}
 private void branchScope(Long id){var u=current();require(u.master()||("USUARIO".equals(u.perfil())&&u.filialId()==null)||r.branchIds(u.id()).contains(id));}
 private Map<String,Object> branch(long id){branchScope(id);return exists(r.one("SELECT * FROM filiais WHERE id=?",id));}
 private void activeBranch(Map<String,Object> b){if(!Boolean.TRUE.equals(b.get("ativo")))throw bad("Filial inativa.");}
 private Map<String,Object> userLocations(Map<String,Object> user){long id=((Number)user.get("id")).longValue();user.put("filial_ids",r.branchIds(id));user.put("locais",r.rows("SELECT f.id,f.nome,f.cidade FROM filiais f JOIN usuario_filiais uf ON uf.filial_id=f.id WHERE uf.usuario_id=? ORDER BY f.nome",id));return user;}
 public Map<String,Object> me(){return userLocations(exists(r.one(USERS+" WHERE u.id=?",current().id())));}
 public List<Map<String,Object>> branches(){var u=current();if(u.master()||("USUARIO".equals(u.perfil())&&u.filialId()==null))return r.rows("SELECT * FROM filiais WHERE ativo=true ORDER BY nome");return r.rows("SELECT * FROM filiais WHERE id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?) AND ativo=true ORDER BY nome",u.id());}
 public Map<String,Object> saveBranch(Long id,Branch b){var u=current();require(u.master());if(id==null){id=r.insert("INSERT INTO filiais(nome,cidade,estado,endereco,responsavel,telefone,ativo) VALUES (?,?,?,?,?,?,?)",b.nome().trim(),b.cidade().trim(),b.estado(),text(b.endereco()),text(b.responsavel()),text(b.telefone()),b.ativo());}else{branch(id);r.update("UPDATE filiais SET nome=?,cidade=?,estado=?,endereco=?,responsavel=?,telefone=?,ativo=? WHERE id=?",b.nome().trim(),b.cidade().trim(),b.estado(),text(b.endereco()),text(b.responsavel()),text(b.telefone()),b.ativo(),id);}audit(null,"FILIAL","Filial atualizada: "+id);return branch(id);}
 public void deleteBranch(long id){require(current().master());branch(id);if(!r.rows("SELECT id FROM qrcodes WHERE filial_id=? AND ativo=true",id).isEmpty())throw bad("Esta filial possui QR Codes ativos.");r.update("UPDATE filiais SET ativo=false WHERE id=?",id);audit(null,"FILIAL_INATIVA","Filial: "+id);}
 public List<Map<String,Object>> users(){var u=current();require(u.admin());var list=u.master()?r.rows(USERS+" ORDER BY u.nome"):r.rows(USERS+" WHERE u.perfil<>'MASTER_ADMIN' AND EXISTS (SELECT 1 FROM usuario_filiais target JOIN usuario_filiais actor ON actor.filial_id=target.filial_id WHERE target.usuario_id=u.id AND actor.usuario_id=?) ORDER BY u.nome",u.id());list.forEach(this::userLocations);return list;}
 private void manageableUser(Account actor,Account target){require(actor.master()||(!target.master()&&!r.branchIds(target.id()).isEmpty()&&r.branchIds(actor.id()).containsAll(r.branchIds(target.id()))));}
 public Map<String,Object> saveUser(Long id,User b){var actor=current();require(actor.admin());if(!Set.of("MASTER_ADMIN","ADMIN","SUPORTE","USUARIO").contains(b.perfil()))throw bad("Perfil inválido.");require(actor.master()||!b.perfil().equals("MASTER_ADMIN"));
  var ids=new LinkedHashSet<Long>();if(b.filialIds()!=null)ids.addAll(b.filialIds());else if(b.filialId()!=null)ids.add(b.filialId());
  if(ids.isEmpty()&&!b.perfil().equals("MASTER_ADMIN"))throw bad("Selecione ao menos um local de atuação.");
  for(long branchId:ids)activeBranch(branch(branchId));
  if(b.senha()!=null&&!b.senha().isBlank())throw bad("As senhas são gerenciadas exclusivamente pela Microsoft. Não envie uma senha.");
  String objectId=b.microsoftObjectId()==null||b.microsoftObjectId().isBlank()?null:b.microsoftObjectId().toLowerCase(Locale.ROOT);
  if(microsoftEnabled&&objectId==null)throw bad("Informe o ID do objeto Microsoft do usuário.");
  Long primary=ids.isEmpty()?null:ids.iterator().next();
  if(id==null){id=r.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id,ativo,microsoft_object_id) VALUES (?,?,?,?,?,?,?)",b.nome().trim(),b.email().trim().toLowerCase(Locale.ROOT),encoder.encode(UUID.randomUUID().toString()),b.perfil(),primary,b.ativo(),objectId);}
  else{var old=r.account(id);if(old==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);manageableUser(actor,old);
   if(id==actor.id()&&(!b.ativo()||!b.perfil().equals(actor.perfil())||!new HashSet<>(r.branchIds(actor.id())).equals(ids)))throw bad("Não altere o próprio perfil, locais, vínculo Microsoft ou status.");
   if(old.master()&&(!b.ativo()||!b.perfil().equals("MASTER_ADMIN"))&&r.rows("SELECT id FROM usuarios WHERE perfil='MASTER_ADMIN' AND ativo=true").size()<=1)throw bad("Mantenha ao menos um administrador mestre ativo.");
   r.update("UPDATE usuarios SET nome=?,email=?,perfil=?,filial_id=?,ativo=?,microsoft_object_id=?,versao_sessao=versao_sessao+1 WHERE id=?",b.nome().trim(),b.email().trim().toLowerCase(Locale.ROOT),b.perfil(),primary,b.ativo(),objectId,id);
  }
  r.update("DELETE FROM usuario_filiais WHERE usuario_id=?",id);for(long branchId:ids)r.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branchId);
  audit(null,"USUARIO","Usuário atualizado: "+id);return userLocations(exists(r.one(USERS+" WHERE u.id=?",id)));
 }
 public void deactivateUser(long id){var u=r.account(id);if(u==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);String oid=(String)r.one("SELECT microsoft_object_id FROM usuarios WHERE id=?",id).get("microsoft_object_id");saveUser(id,new User(u.nome(),u.email(),null,u.perfil(),u.filialId(),false,r.branchIds(id),oid));}
 public Map<String,Object> profile(Profile p){var u=current();if(r.one("SELECT microsoft_object_id FROM usuarios WHERE id=?",u.id()).get("microsoft_object_id")!=null&&!p.email().trim().equalsIgnoreCase(u.email()))throw bad("O e-mail da conta Microsoft é gerenciado pelo administrador.");r.update("UPDATE usuarios SET nome=?,email=?,telefone=? WHERE id=?",p.nome().trim(),p.email().trim().toLowerCase(Locale.ROOT),text(p.telefone()),u.id());return exists(r.one(USERS+" WHERE u.id=?",u.id()));}
 public void password(Password p){throw new ResponseStatusException(HttpStatus.GONE,"Alteração de senha disponível somente na Microsoft: https://passwordreset.microsoftonline.com");}
 public void endSessions(){var u=current();r.update("UPDATE usuarios SET versao_sessao=versao_sessao+1 WHERE id=?",u.id());}
 public String publicUrl(){
  try{URI uri=URI.create(publicUrl);String h=uri.getHost();if(!"https".equals(uri.getScheme())||h==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||h.equals("localhost")||h.endsWith(".localhost")||h.endsWith(".local")||!h.contains(".")||h.contains(":")||h.matches("[0-9.]+")||!uri.getPath().endsWith("novo-chamado.html"))throw new IllegalArgumentException();return uri.toString();}
  catch(Exception e){throw bad("Configure PUBLIC_FRONTEND_URL com a URL HTTPS pública completa de novo-chamado.html, incluindo a subpasta.");}
 }
 public List<Map<String,Object>> qrs(){var u=current();require(u.admin());String sql="SELECT q.*,f.nome filial_nome,f.cidade FROM qrcodes q JOIN filiais f ON f.id=q.filial_id WHERE q.ativo=true";return u.master()?r.rows(sql+" ORDER BY q.criado_em DESC"):r.rows(sql+" AND q.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?) ORDER BY q.criado_em DESC",u.id());}
 public Map<String,Object> qr(String id){var q=exists(r.one("SELECT q.*,f.nome filial_nome,f.cidade,f.ativo filial_ativa FROM qrcodes q JOIN filiais f ON f.id=q.filial_id WHERE q.id=? AND q.ativo=true",id));branchScope(((Number)q.get("filial_id")).longValue());if(!Boolean.TRUE.equals(q.get("filial_ativa")))throw bad("Filial inativa.");return q;}
 public Map<String,Object> createQr(Qr q){require(current().admin());publicUrl();activeBranch(branch(q.filialId()));String id=UUID.randomUUID().toString();r.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala,descricao) VALUES (?,?,?,?,?,?)",id,q.nome().trim(),q.filialId(),q.bloco().trim(),q.sala().trim(),text(q.descricao()));audit(null,"QR_CRIADO","QR: "+id);return qr(id);}
 public void deleteQr(String id){require(current().admin());qr(id);r.update("UPDATE qrcodes SET ativo=false WHERE id=?",id);audit(null,"QR_EXCLUIDO","QR: "+id);}
 private String scope(Account u){return u.master()?"1=1":u.support()?"c.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id="+u.id()+")":"c.solicitante_id="+u.id();}
 public List<Map<String,Object>> tickets(boolean mine){var u=current();var rows=r.rows(TICKETS+" WHERE "+scope(u)+(mine?" AND c.solicitante_id="+u.id():"")+" ORDER BY c.abertura DESC,c.id DESC");rows.forEach(this::decorate);return rows;}
 public Map<String,Object> ticket(long id){var u=current();var t=exists(r.one(TICKETS+" WHERE c.id=? AND "+scope(u),id));decorate(t);return t;}
 private void decorate(Map<String,Object> t){long id=((Number)t.get("id")).longValue();t.put("protocolo",String.format("#%06d",id));var open=Instant.parse(t.get("abertura").toString());var end=t.get("finalizacao")==null?Instant.now():Instant.parse(t.get("finalizacao").toString());var taken=t.get("atendimento")==null?end:Instant.parse(t.get("atendimento").toString());long response=Duration.between(open,taken).toSeconds();long total=Duration.between(open,end).toSeconds();t.put("tempo_resposta_segundos",response);t.put("tempo_total_segundos",total);t.put("fora_sla",!"CANCELADO".equals(t.get("status"))&&(response>((Number)t.get("sla_resposta")).longValue()*60||total>((Number)t.get("sla_resolucao")).longValue()*60));}
 public Map<String,Object> createTicket(Ticket b){var u=current();String prioridade=b.prioridade()==null?"MEDIA":b.prioridade();if(!Set.of("BAIXA","MEDIA","ALTA","CRITICA").contains(prioridade))throw bad("Prioridade inválida.");Long branchId=b.filialId();String bloco=text(b.bloco()),sala=text(b.sala()),qrId=text(b.qrId());
  if(!qrId.isBlank()){var qr=qr(qrId);branchId=((Number)qr.get("filial_id")).longValue();bloco=qr.get("bloco").toString();sala=qr.get("sala").toString();}
  if(branchId==null)throw bad("Selecione a filial.");var branch=branch(branchId);activeBranch(branch);
  long id=r.insert("INSERT INTO chamados(titulo,descricao,solicitante_id,filial_id,cidade,bloco,sala,setor,categoria,tipo_atendimento,prioridade,sla_resposta,sla_resolucao,origem,qr_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",b.titulo().trim(),b.descricao().trim(),u.id(),branchId,branch.get("cidade"),bloco,sala,text(b.setor()),text(b.categoria()),text(b.tipoAtendimento()),prioridade,responseMinutes,resolutionMinutes,qrId.isBlank()?"MANUAL":"QR",qrId.isBlank()?null:qrId);
  audit(id,"ABERTO","Chamado aberto.");notify(id,"chamado","Novo chamado");return ticket(id);
 }
 public Map<String,Object> action(long id,Action b){ticket(id);var u=current();require(u.support());var locked=exists(r.one("SELECT * FROM chamados WHERE id=? FOR UPDATE",id));String old=locked.get("status").toString();String next;
  switch(b.acao()){
   case "assumir" -> {if(!old.equals("ABERTO")&&!old.equals("AGUARDANDO_USUARIO"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chamado já assumido ou encerrado.");next="EM_ATENDIMENTO";r.update("UPDATE chamados SET tecnico_id=?,atendimento=COALESCE(atendimento,CURRENT_TIMESTAMP) WHERE id=?",u.id(),id);}
   case "finalizar" -> {if(!Set.of("EM_ATENDIMENTO","AGUARDANDO_USUARIO").contains(old))throw bad("Assuma o chamado antes de finalizar.");if(text(b.solucao()).isBlank())throw bad("Informe a solução aplicada.");next="FINALIZADO";r.update("UPDATE chamados SET finalizacao=CURRENT_TIMESTAMP,solucao=? WHERE id=?",text(b.solucao()),id);}
   case "aguardar" -> {if(!old.equals("EM_ATENDIMENTO"))throw bad("O chamado não está em atendimento.");next="AGUARDANDO_USUARIO";}
   case "cancelar" -> {if(Set.of("FINALIZADO","CANCELADO").contains(old))throw bad("Chamado já encerrado.");next="CANCELADO";r.update("UPDATE chamados SET finalizacao=CURRENT_TIMESTAMP WHERE id=?",id);}
   case "reabrir" -> {if(!Set.of("FINALIZADO","CANCELADO").contains(old))throw bad("Somente chamados encerrados podem ser reabertos.");next="ABERTO";r.update("UPDATE chamados SET finalizacao=null WHERE id=?",id);}
   default -> throw bad("Ação inválida.");
  }r.update("UPDATE chamados SET status=? WHERE id=?",next,id);audit(id,next,text(b.solucao()).isBlank()?"Status: "+old+" → "+next:text(b.solucao()));notify(id,"chamado","Chamado "+next.toLowerCase(Locale.ROOT).replace('_',' '));return ticket(id);
 }
 public List<Map<String,Object>> messages(long id){ticket(id);return r.rows("SELECT m.*,u.nome autor,u.perfil FROM mensagens m JOIN usuarios u ON u.id=m.autor_id WHERE m.chamado_id=? ORDER BY m.criado_em,m.id",id);}
 public void message(long id,Message b){var t=ticket(id);if(Set.of("FINALIZADO","CANCELADO").contains(t.get("status")))throw bad("Reabra o chamado antes de enviar novas mensagens.");r.insert("INSERT INTO mensagens(chamado_id,autor_id,mensagem) VALUES (?,?,?)",id,current().id(),b.mensagem().trim());audit(id,"MENSAGEM","Mensagem adicionada.");notify(id,"mensagem","Nova mensagem no chamado");}
 private void audit(Long id,String action,String description){r.update("INSERT INTO auditoria(autor_id,chamado_id,acao,descricao) VALUES (?,?,?,?)",current().id(),id,action,description);}
 public List<Map<String,Object>> history(long id){ticket(id);return r.rows("SELECT a.*,u.nome autor FROM auditoria a JOIN usuarios u ON u.id=a.autor_id WHERE a.chamado_id=? ORDER BY a.id",id);}
 public List<Map<String,Object>> activities(){return r.rows("SELECT * FROM auditoria WHERE autor_id=? ORDER BY id DESC LIMIT 100",current().id());}
 private void notify(long id,String type,String title){var t=exists(r.one("SELECT * FROM chamados WHERE id=?",id));var targets=r.rows("SELECT id FROM usuarios WHERE ativo=true AND (id=? OR perfil='MASTER_ADMIN' OR (perfil IN ('ADMIN','SUPORTE') AND id IN (SELECT usuario_id FROM usuario_filiais WHERE filial_id=?)))",t.get("solicitante_id"),t.get("filial_id"));for(var target:targets){if(((Number)target.get("id")).longValue()==current().id())continue;r.update("INSERT INTO notificacoes(usuario_id,chamado_id,tipo,titulo) VALUES (?,?,?,?)",target.get("id"),id,type,title);}}
 public List<Map<String,Object>> notifications(){return r.rows("SELECT n.* FROM notificacoes n JOIN chamados c ON c.id=n.chamado_id WHERE n.usuario_id=? AND "+scope(current())+" ORDER BY n.id DESC",current().id());}
 public void readNotification(Long id){if(id!=null){if(r.update("UPDATE notificacoes SET lida=true WHERE id=? AND usuario_id=?",id,current().id())==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}else r.update("UPDATE notificacoes SET lida=true WHERE usuario_id=?",current().id());}
 public void clearNotifications(){r.update("DELETE FROM notificacoes WHERE usuario_id=?",current().id());}
 public List<Map<String,Object>> attachments(long id){ticket(id);return r.rows("SELECT id,chamado_id,autor_id,nome,tamanho,criado_em FROM anexos WHERE chamado_id=? ORDER BY criado_em",id);}
 public void upload(long id,MultipartFile file)throws java.io.IOException {var t=ticket(id);if(Set.of("FINALIZADO","CANCELADO").contains(t.get("status")))throw bad("Chamado encerrado.");String name=text(file.getOriginalFilename()).replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(file.isEmpty()||file.getSize()>10*1024*1024||name.length()>255||!name.toLowerCase(Locale.ROOT).matches(".+\\.(jpg|jpeg|png|pdf|doc|docx|xls|xlsx|txt)"))throw bad("Arquivo inválido ou maior que 10 MB.");if(attachments(id).size()>=20)throw bad("Limite de 20 anexos por chamado.");r.update("INSERT INTO anexos(id,chamado_id,autor_id,nome,tamanho,conteudo) VALUES (?,?,?,?,?,?)",UUID.randomUUID().toString(),id,current().id(),name,file.getSize(),file.getBytes());audit(id,"ANEXO","Arquivo anexado: "+name);}
 public Map<String,Object> download(long id,String attachmentId){ticket(id);return exists(r.one("SELECT * FROM anexos WHERE id=? AND chamado_id=?",attachmentId,id));}
 public Map<String,Object> metrics(Map<String,String> filters){var list=tickets(false).stream().filter(t->matches(t,filters)).toList();var result=new LinkedHashMap<String,Object>();result.put("total",list.size());
  for(String key:List.of("status","prioridade","filial_nome","cidade","tecnico","categoria"))result.put("por_"+key,list.stream().collect(Collectors.groupingBy(t->Objects.toString(t.get(key),"Não atribuído"),TreeMap::new,Collectors.counting())));
  result.put("fora_sla",list.stream().filter(t->Boolean.TRUE.equals(t.get("fora_sla"))).count());result.put("dentro_sla_percentual",list.isEmpty()?0:100.0*list.stream().filter(t->!Boolean.TRUE.equals(t.get("fora_sla"))).count()/list.size());
  result.put("tempo_medio_resposta",list.stream().filter(t->t.get("atendimento")!=null).mapToLong(t->((Number)t.get("tempo_resposta_segundos")).longValue()).average().orElse(0));result.put("tempo_medio_resolucao",list.stream().filter(t->"FINALIZADO".equals(t.get("status"))).mapToLong(t->((Number)t.get("tempo_total_segundos")).longValue()).average().orElse(0));
  LocalDate today=LocalDate.now(ZoneId.of("America/Sao_Paulo"));result.put("chamados_hoje",list.stream().filter(t->Instant.parse(t.get("abertura").toString()).atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().equals(today)).count());result.put("finalizados_hoje",list.stream().filter(t->"FINALIZADO".equals(t.get("status"))&&Instant.parse(t.get("finalizacao").toString()).atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().equals(today)).count());result.put("chamados",list);return result;
 }
 private boolean matches(Map<String,Object> t,Map<String,String> f){for(String key:List.of("filial_id","cidade","tecnico_id","status","prioridade")){if(f.containsKey(key)&&!f.get(key).isBlank()&&!f.get(key).equals(Objects.toString(t.get(key),"")))return false;}try{LocalDate d=Instant.parse(t.get("abertura").toString()).atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();if(f.containsKey("inicio")&&!f.get("inicio").isBlank()&&d.isBefore(LocalDate.parse(f.get("inicio"))))return false;if(f.containsKey("fim")&&!f.get("fim").isBlank()&&d.isAfter(LocalDate.parse(f.get("fim"))))return false;}catch(java.time.format.DateTimeParseException e){throw bad("Período inválido.");}return true;}
 public void preferences(Map<String,Boolean> data)throws com.fasterxml.jackson.core.JsonProcessingException{if(!Set.of("systemNotifications","notificationSound","emailNotifications","slaNotifications").containsAll(data.keySet())||data.containsValue(null))throw bad("Preferência inválida.");r.update("UPDATE usuarios SET preferencias=? WHERE id=?",new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data),current().id());}
}


