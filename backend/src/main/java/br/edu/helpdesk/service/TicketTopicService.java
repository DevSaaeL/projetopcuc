package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.text.Collator;
import java.util.*;

@Service
@Transactional
public class TicketTopicService {
 public static final List<String> DEFAULTS=List.of("Caixa de Som","Computador (Hardware — Problema Físico)","Computador (Software — Problema de Sistema)","Computador sem Internet Cabeada","Impressora","Login","Microfone","Projetor","Sem acesso à rede Wi-Fi");
 private final DeskRepository repo;private final DeskService desk;
 public TicketTopicService(DeskRepository repo,DeskService desk){this.repo=repo;this.desk=desk;}
 public static void seed(DeskRepository repo,long branch){for(String name:DEFAULTS)repo.insert("INSERT INTO assuntos_chamado(filial_id,nome,nome_chave) VALUES (?,?,?)",branch,name,name.toLowerCase(Locale.ROOT));}
 private static List<Map<String,Object>> alphabetical(List<Map<String,Object>> rows){var collator=Collator.getInstance(Locale.forLanguageTag("pt-BR"));rows.sort((a,b)->{int unit=collator.compare(Objects.toString(a.get("filial_nome"),""),Objects.toString(b.get("filial_nome"),""));return unit!=0?unit:collator.compare(a.get("nome").toString(),b.get("nome").toString());});return rows;}
 public static List<Map<String,Object>> active(DeskRepository repo,long branch){return alphabetical(repo.rows("SELECT id,nome FROM assuntos_chamado WHERE filial_id=? AND ativo=true",branch));}
 public static Map<String,Object> selected(DeskRepository repo,long id,long branch){var topic=repo.one("SELECT id,nome FROM assuntos_chamado WHERE id=? AND filial_id=? AND ativo=true FOR UPDATE",id,branch);if(topic==null)throw DeskService.bad("Este assunto não está disponível para a unidade do chamado. Atualize a página e selecione outro assunto.");return topic;}
 private List<Map<String,Object>> branches(){if(!desk.current().admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente administradores podem gerenciar assuntos.");return desk.branches();}
 private void branch(long id){if(branches().stream().noneMatch(b->((Number)b.get("id")).longValue()==id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unidade fora dos seus locais de atuação.");}
 public List<Map<String,Object>> list(Long branchId){var branches=branches();if(branchId!=null)branch(branchId);var rows=new ArrayList<Map<String,Object>>();for(var unit:branches){long id=((Number)unit.get("id")).longValue();if(branchId==null||branchId==id)rows.addAll(repo.rows("SELECT a.id,a.nome,a.filial_id,a.ativo,f.nome filial_nome FROM assuntos_chamado a JOIN filiais f ON f.id=a.filial_id WHERE a.filial_id=?",id));}return alphabetical(rows);}
 public Map<String,Object> save(Long id,long branchId,String name,boolean active){
  branch(branchId);name=name.trim().replaceAll("\\s+"," ");if(name.isBlank()||name.length()>80)throw DeskService.bad("Informe um assunto com até 80 caracteres.");
  if(id!=null){var old=repo.one("SELECT filial_id FROM assuntos_chamado WHERE id=?",id);if(old==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);branch(((Number)old.get("filial_id")).longValue());if(((Number)old.get("filial_id")).longValue()!=branchId)throw DeskService.bad("A unidade de um assunto existente não pode ser alterada.");}
  String key=name.toLowerCase(Locale.ROOT);var existing=repo.one("SELECT id FROM assuntos_chamado WHERE filial_id=? AND nome_chave=?",branchId,key);if(existing!=null&&(id==null||((Number)existing.get("id")).longValue()!=id))throw DeskService.bad("Já existe um assunto com esse nome nesta unidade. Você pode reativá-lo na lista.");
  if(id==null)id=repo.insert("INSERT INTO assuntos_chamado(filial_id,nome,nome_chave,ativo) VALUES (?,?,?,?)",branchId,name,key,active);
  else repo.update("UPDATE assuntos_chamado SET nome=?,nome_chave=?,ativo=? WHERE id=?",name,key,active,id);
  desk.recordActivity("ASSUNTO","Assunto "+id+" da unidade "+branchId+" atualizado.");return repo.one("SELECT id,nome,filial_id,ativo FROM assuntos_chamado WHERE id=?",id);
 }
}
