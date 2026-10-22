package br.edu.helpdesk.service;

import br.edu.helpdesk.entity.Account;
import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class AttendanceArchive {
 private final DeskRepository repo;
 private static final List<String> COLUMNS=List.of("chamado_id","tecnico_id","tecnico_nome","finalizado_por_id","finalizado_por_nome","filial_id","filial_nome","titulo","descricao","solicitante_nome","solicitante_email","solicitante_telefone","cidade","bloco","sala","setor","categoria","prioridade","status","abertura","inicio_ciclo","atendimento","finalizacao","sla_resposta","sla_resolucao","solucao","ultimo_evento_id","encerramento_estimado","cancelado_por_solicitante","motivo_cancelamento");
 private static final String SELECT="SELECT a.*,a.id registro_id,a.chamado_id id,a.solicitante_nome solicitante,a.tecnico_nome tecnico,(SELECT c.status FROM chamados c WHERE c.id=a.chamado_id) status_atual FROM atendimentos_chamado a";
 public AttendanceArchive(DeskRepository repo){this.repo=repo;}
 private String scope(Account actor){
  if(!actor.support())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"O histórico de atendimentos é exclusivo da equipe de TI.");
  return actor.master()?"1=1":actor.admin()?"a.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id="+actor.id()+")":"a.tecnico_id="+actor.id();
 }
 public void capture(long ticketId,Account actor){capture(ticketId,actor.id(),actor.nome(),!actor.admin());}
 public void captureRequester(long ticketId,String name){capture(ticketId,null,name,false);}
 private void capture(long ticketId,Long actorId,String actorName,boolean assign){
  var row=repo.one("SELECT c.*,c.id chamado_id,COALESCE(c.solicitante_nome,u.nome) solicitante_nome,u.email solicitante_email,u.telefone solicitante_telefone,f.nome filial_nome,t.nome tecnico_nome FROM chamados c LEFT JOIN usuarios u ON u.id=c.solicitante_id LEFT JOIN usuarios t ON t.id=c.tecnico_id JOIN filiais f ON f.id=c.filial_id WHERE c.id=?",ticketId);
  if(row.get("tecnico_id")==null&&assign){row.put("tecnico_id",actorId);row.put("tecnico_nome",actorName);}
  row.put("finalizado_por_id",actorId);row.put("finalizado_por_nome",actorName);row.put("encerramento_estimado",false);
  row.put("ultimo_evento_id",repo.one("SELECT COALESCE(MAX(id),0) last_id FROM auditoria WHERE chamado_id=?",ticketId).get("last_id"));
  // Rebind normalized timestamp strings as temporal values for PostgreSQL.
  for(String field:List.of("abertura","inicio_ciclo","atendimento","finalizacao"))if(row.get(field)!=null)row.put(field,java.time.OffsetDateTime.parse(row.get(field).toString()));
  long id=repo.insert("INSERT INTO atendimentos_chamado("+String.join(",",COLUMNS)+") VALUES ("+String.join(",",Collections.nCopies(COLUMNS.size(),"?"))+")",COLUMNS.stream().map(row::get).toArray());
  repo.update("INSERT INTO atendimento_anexos(atendimento_id,anexo_id) SELECT ?,id FROM anexos WHERE chamado_id=?",id,ticketId);
 }
 private void decorate(Map<String,Object> row){row.put("protocolo",String.format("#%06d",((Number)row.get("id")).longValue()));row.put("somente_leitura",true);row.put("posicao_fila",null);TicketTiming.decorate(row,Instant.now());}
 public List<Map<String,Object>> list(Account actor){var rows=repo.rows(SELECT+" WHERE "+scope(actor)+" ORDER BY a.finalizacao DESC,a.id DESC");rows.forEach(this::decorate);return rows;}
 public Map<String,Object> record(long id,Account actor){
  var row=repo.one(SELECT+" WHERE a.id=? AND "+scope(actor),id);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Atendimento não encontrado.");decorate(row);
  var current=repo.one("SELECT status FROM chamados WHERE id=?",row.get("chamado_id"));row.put("status_atual",current.get("status"));
  var latest=repo.one("SELECT MAX(id) last_id FROM atendimentos_chamado WHERE chamado_id=?",row.get("chamado_id"));
  row.put("pode_reabrir",actor.admin()&&Set.of("FINALIZADO","CANCELADO").contains(current.get("status"))&&((Number)latest.get("last_id")).longValue()==id);
  return row;
 }
 public List<Map<String,Object>> history(long id,Account actor){var row=record(id,actor);return repo.rows("SELECT a.*,COALESCE(a.autor_nome,u.nome) autor FROM auditoria a LEFT JOIN usuarios u ON u.id=a.autor_id WHERE a.chamado_id=? AND a.id<=? ORDER BY a.id",row.get("chamado_id"),row.get("ultimo_evento_id"));}
 public List<Map<String,Object>> attachments(long id,Account actor){record(id,actor);return repo.rows("SELECT x.id,x.nome,x.tamanho,x.criado_em FROM anexos x JOIN atendimento_anexos a ON a.anexo_id=x.id WHERE a.atendimento_id=? ORDER BY x.criado_em,x.id",id);}
 public Map<String,Object> download(long id,String attachment,Account actor){record(id,actor);var row=repo.one("SELECT x.nome,x.conteudo FROM anexos x JOIN atendimento_anexos a ON a.anexo_id=x.id WHERE a.atendimento_id=? AND x.id=?",id,attachment);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return row;}
}
