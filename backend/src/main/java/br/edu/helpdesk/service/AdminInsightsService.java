package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;

@Service
public class AdminInsightsService {
 private final DeskService desk;private final DeskRepository repo;
 public AdminInsightsService(DeskService desk,DeskRepository repo){this.desk=desk;this.repo=repo;}
 private void requireAdmin(){if(!desk.current().admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Este painel é exclusivo para administradores.");}
 public Map<String,Object> recurrence(Map<String,String> filters){
  requireAdmin();
  @SuppressWarnings("unchecked") var tickets=(List<Map<String,Object>>)desk.metrics(filters).get("chamados");
  return Map.of("total",tickets.size(),"por_local",group(tickets,List.of("filial_id","filial_nome","bloco","sala")),"por_assunto",group(tickets,List.of("filial_id","filial_nome","categoria")));
 }
 private List<Map<String,Object>> group(List<Map<String,Object>> tickets,List<String> fields){
  var groups=new LinkedHashMap<List<Object>,Map<String,Object>>();
  for(var ticket:tickets){
   var key=fields.stream().map(field->Objects.requireNonNullElse(ticket.get(field),"")).toList();
   var row=groups.computeIfAbsent(key,k->{var value=new LinkedHashMap<String,Object>();for(int i=0;i<fields.size();i++)value.put(fields.get(i),k.get(i));value.put("total",0);value.put("ativos",0);return value;});
   row.put("total",(Integer)row.get("total")+1);
   if(!Set.of("FINALIZADO","CANCELADO").contains(ticket.get("status")))row.put("ativos",(Integer)row.get("ativos")+1);
  }
  return groups.values().stream().sorted(Comparator.<Map<String,Object>>comparingInt(row->(Integer)row.get("total")).reversed().thenComparing(row->row.toString())).limit(20).toList();
 }
 public Map<String,Object> health(){
  requireAdmin();var result=new LinkedHashMap<String,Object>();long start=System.nanoTime();
  boolean database=true;try{repo.one("SELECT 1 AS ok");}catch(Exception e){database=false;}
  result.put("aplicacao","ONLINE");result.put("banco",database?"ONLINE":"INDISPONIVEL");
  result.put("status",database?"SAUDAVEL":"DEGRADADO");result.put("banco_latencia_ms",(System.nanoTime()-start)/1_000_000);
  result.put("verificado_em",Instant.now().toString());result.put("tempo_ativo_segundos",ManagementFactory.getRuntimeMXBean().getUptime()/1000);
  result.put("versao",Objects.toString(getClass().getPackage().getImplementationVersion(),"desenvolvimento"));
  try{var migration=repo.one("SELECT version FROM flyway_schema_history WHERE success=true AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1");result.put("migracao",migration==null?"Não identificada":migration.get("version"));}catch(Exception e){result.put("migracao","Não disponível");}
  return result;
 }
}
