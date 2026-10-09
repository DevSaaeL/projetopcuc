package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class PublicTicketService {
 private final DeskRepository repo;
 @Value("${app.sla-response-minutes}") private int responseMinutes;
 @Value("${app.sla-resolution-minutes}") private int resolutionMinutes;
 public PublicTicketService(DeskRepository repo){this.repo=repo;}
 private Map<String,Object> qr(String id,boolean lock){
  var qr=repo.one("SELECT q.id,q.nome,q.filial_id,q.bloco,q.sala,f.nome filial_nome,f.cidade FROM qrcodes q JOIN filiais f ON f.id=q.filial_id WHERE q.id=? AND q.ativo=true AND f.ativo=true"+(lock?" FOR UPDATE":""),id);
  if(qr==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"QR Code inválido ou desativado. Procure a equipe de TI.");return qr;
 }
 public Map<String,Object> location(String id){var q=qr(id,false);q.remove("filial_id");return q;}
 @Transactional
 public Map<String,Object> create(String qrId,String name,String description,String requestId){
  var q=qr(qrId,true);
  var previous=repo.one("SELECT id,qr_id FROM chamados WHERE requisicao_id=?",requestId);
  if(previous!=null){if(!qrId.equals(previous.get("qr_id")))throw DeskService.bad("Solicitação inválida.");return receipt(((Number)previous.get("id")).longValue());}
  String title=description.trim().replaceAll("\\s+"," ");title=title.substring(0,Math.min(150,title.length()));
  long id=repo.insert("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,bloco,sala,prioridade,sla_resposta,sla_resolucao,origem,qr_id,requisicao_id) VALUES (?,?,?,?,?,?,?,'MEDIA',?,?,'QR',?,?)",title,description.trim(),name.trim(),q.get("filial_id"),q.get("cidade"),q.get("bloco"),q.get("sala"),responseMinutes,resolutionMinutes,qrId,requestId);
  repo.update("INSERT INTO auditoria(chamado_id,autor_nome,acao,descricao) VALUES (?,?,'ABERTO','Chamado aberto pelo QR Code, sem conta de acesso.')",id,name.trim());
  var targets=repo.rows("SELECT id FROM usuarios WHERE ativo=true AND (perfil='MASTER_ADMIN' OR (perfil IN ('ADMIN','SUPORTE') AND id IN (SELECT usuario_id FROM usuario_filiais WHERE filial_id=?)))",q.get("filial_id"));
  for(var target:targets)repo.update("INSERT INTO notificacoes(usuario_id,chamado_id,tipo,titulo) VALUES (?,?,'chamado','Novo chamado')",target.get("id"),id);
  return receipt(id);
 }
 private Map<String,Object> receipt(long id){return Map.of("protocolo",String.format("#%06d",id));}
}
