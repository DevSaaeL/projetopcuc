package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class PublicTicketService {
 private final DeskRepository repo;
 private final AttendanceArchive archive;
 @Value("${app.sla-response-minutes}") private int responseMinutes;
 @Value("${app.sla-resolution-minutes}") private int resolutionMinutes;
 public PublicTicketService(DeskRepository repo,AttendanceArchive archive){this.repo=repo;this.archive=archive;}
 private Map<String,Object> qr(String id,boolean lock){
  var qr=repo.one("SELECT q.id,q.nome,q.filial_id,q.bloco,q.sala,f.nome filial_nome,f.cidade FROM qrcodes q JOIN filiais f ON f.id=q.filial_id WHERE q.id=? AND q.ativo=true AND f.ativo=true"+(lock?" FOR UPDATE":""),id);
  if(qr==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"QR Code inválido ou desativado. Procure a equipe de TI.");return qr;
 }
 @Transactional
 public Map<String,Object> location(String id){var q=qr(id,false);q.put("assuntos",TicketTopicService.active(repo,((Number)q.get("filial_id")).longValue()));
  var active=repo.rows("SELECT id FROM chamados WHERE filial_id=? AND LOWER(TRIM(bloco))=LOWER(TRIM(?)) AND LOWER(TRIM(sala))=LOWER(TRIM(?)) AND status NOT IN ('FINALIZADO','CANCELADO') ORDER BY COALESCE(inicio_ciclo,abertura),id",q.get("filial_id"),q.get("bloco"),q.get("sala"));
  q.put("chamados_ativos",active.stream().map(row->receipt(((Number)row.get("id")).longValue())).toList());q.remove("filial_id");return q;}
 @Transactional
 public Map<String,Object> create(String qrId,String name,long topicId,String requestId,String key){
  var q=qr(qrId,true);
  var previous=repo.one("SELECT id,qr_id,chave_cancelamento_hash FROM chamados WHERE requisicao_id=?",requestId);
  if(previous!=null){if(!qrId.equals(previous.get("qr_id")))throw DeskService.bad("Solicitação inválida.");if(previous.get("chave_cancelamento_hash")!=null&&!Objects.equals(previous.get("chave_cancelamento_hash"),hash(key)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitação não encontrada neste navegador.");return ownedReceipt(((Number)previous.get("id")).longValue());}
  var topic=TicketTopicService.selected(repo,topicId,((Number)q.get("filial_id")).longValue());String title=topic.get("nome").toString();
  var duplicate=repo.one("SELECT id FROM chamados WHERE filial_id=? AND LOWER(TRIM(bloco))=LOWER(TRIM(?)) AND LOWER(TRIM(sala))=LOWER(TRIM(?)) AND assunto_id=? AND status NOT IN ('FINALIZADO','CANCELADO') ORDER BY COALESCE(inicio_ciclo,abertura),id LIMIT 1",q.get("filial_id"),q.get("bloco"),q.get("sala"),topicId);
  if(duplicate!=null){var result=receipt(((Number)duplicate.get("id")).longValue());result.put("ja_existente",true);return result;}
  long id=repo.insert("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,bloco,sala,prioridade,sla_resposta,sla_resolucao,origem,qr_id,requisicao_id,assunto_id,categoria) VALUES (?,?,?,?,?,?,?,'MEDIA',?,?,'QR',?,?,?,?)",title,"Assunto selecionado: "+title,name.trim(),q.get("filial_id"),q.get("cidade"),q.get("bloco"),q.get("sala"),responseMinutes,resolutionMinutes,qrId,requestId,topicId,title);
  if(key!=null)repo.update("UPDATE chamados SET chave_cancelamento_hash=? WHERE id=?",hash(key),id);
  repo.update("INSERT INTO auditoria(chamado_id,autor_nome,acao,descricao) VALUES (?,?,'ABERTO','Chamado aberto pelo QR Code, sem conta de acesso.')",id,name.trim());
  var targets=repo.rows("SELECT id FROM usuarios WHERE ativo=true AND (perfil='MASTER_ADMIN' OR (perfil IN ('ADMIN','SUPORTE') AND id IN (SELECT usuario_id FROM usuario_filiais WHERE filial_id=?)))",q.get("filial_id"));
  for(var target:targets)repo.update("INSERT INTO notificacoes(usuario_id,chamado_id,tipo,titulo) VALUES (?,?,'chamado','Novo chamado')",target.get("id"),id);
  return ownedReceipt(id);
 }
 private String hash(String key){if(key==null)return "";try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private Map<String,Object> owned(long id,String key){var row=repo.one("SELECT * FROM chamados WHERE id=? AND chave_cancelamento_hash=? FOR UPDATE",id,hash(key));if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chamado não encontrado neste navegador.");return row;}
 @Transactional public Map<String,Object> status(long id,String key){owned(id,key);return ownedReceipt(id);}
 @Transactional public Map<String,Object> cancel(long id,String key,String reason){
  var row=owned(id,key);String state=row.get("status").toString();
  if(state.equals("CANCELADO")&&Boolean.TRUE.equals(row.get("cancelado_por_solicitante")))return ownedReceipt(id);
  if(Set.of("FINALIZADO","CANCELADO").contains(state))throw new ResponseStatusException(HttpStatus.CONFLICT,"Este chamado já foi encerrado e não pode ser cancelado.");
  String motive=reason.trim();if(motive.isEmpty()||motive.length()>1000)throw DeskService.bad("Informe o motivo do cancelamento, com até 1000 caracteres.");
  String name=Objects.toString(row.get("solicitante_nome"),"Solicitante");
  repo.update("UPDATE chamados SET status='CANCELADO',finalizacao=CURRENT_TIMESTAMP,cancelado_por_solicitante=true,motivo_cancelamento=? WHERE id=?",motive,id);
  repo.update("INSERT INTO auditoria(chamado_id,autor_nome,acao,descricao) VALUES (?,?,'CANCELADO_SOLICITANTE',?)",id,name,"Solicitante cancelou o chamado. Motivo: "+motive);
  archive.captureRequester(id,name);
  var targets=repo.rows("SELECT id FROM usuarios WHERE ativo=true AND (perfil='MASTER_ADMIN' OR (perfil IN ('ADMIN','SUPORTE') AND id IN (SELECT usuario_id FROM usuario_filiais WHERE filial_id=?)))",row.get("filial_id"));
  for(var target:targets)repo.update("INSERT INTO notificacoes(usuario_id,chamado_id,tipo,titulo) VALUES (?,?,'chamado','Chamado cancelado pelo solicitante')",target.get("id"),id);
  return ownedReceipt(id);
 }
 private Map<String,Object> ownedReceipt(long id){var row=repo.one("SELECT id,status,cancelado_por_solicitante,motivo_cancelamento FROM chamados WHERE id=?",id);var result=receipt(id);result.putAll(row);return result;}
 public Map<String,Object> tracking(String token){
  var ticket=repo.one("SELECT id FROM chamados WHERE acompanhamento_token=?",token);
  if(ticket==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Acompanhamento não encontrado. Confira o link recebido.");
  return receipt(((Number)ticket.get("id")).longValue());
 }
 private Map<String,Object> receipt(long id){
  var ticket=repo.one("SELECT id,filial_id,assunto_id,titulo,status,abertura,inicio_ciclo,atendimento,finalizacao,acompanhamento_token FROM chamados WHERE id=?",id);
  if(ticket.get("acompanhamento_token")==null){repo.update("UPDATE chamados SET acompanhamento_token=? WHERE id=? AND acompanhamento_token IS NULL",UUID.randomUUID().toString(),id);ticket.put("acompanhamento_token",repo.one("SELECT acompanhamento_token FROM chamados WHERE id=?",id).get("acompanhamento_token"));}
  var result=new LinkedHashMap<String,Object>();
  for(String key:List.of("assunto_id","titulo","status","abertura","atendimento","finalizacao","acompanhamento_token"))result.put(key,ticket.get(key));
  result.put("protocolo",String.format("#%06d",id));
  boolean active=!Set.of("FINALIZADO","CANCELADO").contains(ticket.get("status"));
  Object cycle=java.time.OffsetDateTime.parse(Objects.toString(ticket.get("inicio_ciclo"),ticket.get("abertura").toString()));
  result.put("posicao_fila",active?repo.one("SELECT COUNT(*) total FROM chamados WHERE filial_id=? AND status NOT IN ('FINALIZADO','CANCELADO') AND (COALESCE(inicio_ciclo,abertura)<? OR (COALESCE(inicio_ciclo,abertura)=? AND id<=?))",ticket.get("filial_id"),cycle,cycle,id).get("total"):null);
  return result;
 }
}
