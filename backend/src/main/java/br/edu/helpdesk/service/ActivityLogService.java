package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ActivityLogService {
 private final DeskRepository repo;
 private final DeskService desk;
 private static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
 public ActivityLogService(DeskRepository repo,DeskService desk){this.repo=repo;this.desk=desk;}
 public Map<String,Object> search(String userId,String action,String start,String end,int page){
  var actor=desk.current();
  if(page<1||page>100000)throw DeskService.bad("Página inválida.");
  List<Map<String,Object>> users=actor.admin()?desk.users().stream().map(u->{var item=new LinkedHashMap<String,Object>();for(String key:List.of("id","nome","email","perfil"))item.put(key,u.get(key));return (Map<String,Object>)item;}).toList():List.of(Map.of("id",actor.id(),"nome",actor.nome(),"email",actor.email(),"perfil",actor.perfil()));
  StringBuilder where=new StringBuilder(" WHERE ");var args=new ArrayList<Object>();
  if(actor.master())where.append("1=1");
  else if(!actor.admin()){where.append("a.autor_id=? AND (a.chamado_id IS NULL OR c.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?))");args.add(actor.id());args.add(actor.id());}
  else {
   // Ticket events are scoped by the ticket's unit, even for multi-unit technicians.
   // Account events are visible only for self or users entirely managed by this admin.
   where.append("((a.chamado_id IS NOT NULL AND c.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?)) OR (a.chamado_id IS NULL AND (a.autor_id=? OR (u.perfil<>'MASTER_ADMIN' AND EXISTS (SELECT 1 FROM usuario_filiais uf WHERE uf.usuario_id=u.id) AND NOT EXISTS (SELECT 1 FROM usuario_filiais uf WHERE uf.usuario_id=u.id AND uf.filial_id NOT IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?))))))");
   args.add(actor.id());args.add(actor.id());args.add(actor.id());
  }
  if(userId!=null&&!userId.isBlank()){
   if("publico".equals(userId)){where.append(" AND a.autor_id IS NULL");}
   else{try{long id=Long.parseLong(userId);if(id<=0)throw new NumberFormatException();where.append(" AND a.autor_id=?");args.add(id);}catch(NumberFormatException e){throw DeskService.bad("Usuário inválido.");}}
  }
  if(action!=null&&!action.isBlank()){if(action.length()>80)throw DeskService.bad("Ação inválida.");where.append(" AND a.acao=?");args.add(action);}
  try{
   LocalDate from=start==null||start.isBlank()?null:LocalDate.parse(start),to=end==null||end.isBlank()?null:LocalDate.parse(end);
   if(from!=null&&to!=null&&to.isBefore(from))throw DeskService.bad("A data final deve ser igual ou posterior à inicial.");
   if(from!=null){where.append(" AND a.criado_em>=?");args.add(from.atStartOfDay(ZONE).toOffsetDateTime());}
   if(to!=null){where.append(" AND a.criado_em<?");args.add(to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());}
  }catch(DateTimeParseException e){throw DeskService.bad("Período inválido.");}
  String joins=" FROM auditoria a LEFT JOIN usuarios u ON u.id=a.autor_id LEFT JOIN chamados c ON c.id=a.chamado_id";
  long count=((Number)repo.one("SELECT COUNT(*) total"+joins+where,args.toArray()).get("total")).longValue();
  int actualPage=(int)Math.min(page,Math.max(1,(count+49)/50));var rowArgs=new ArrayList<>(args);rowArgs.add(50);rowArgs.add((actualPage-1)*50);
  var rows=repo.rows("SELECT a.id,a.autor_id,COALESCE(a.autor_nome,u.nome,'Sistema') autor,u.email,u.perfil,a.chamado_id,c.titulo chamado_titulo,a.acao,a.descricao,a.criado_em"+joins+where+" ORDER BY a.criado_em DESC,a.id DESC LIMIT ? OFFSET ?",rowArgs.toArray());
  return Map.of("registros",rows,"total",count,"pagina",actualPage,"por_pagina",50,"usuarios",users);
 }
}
