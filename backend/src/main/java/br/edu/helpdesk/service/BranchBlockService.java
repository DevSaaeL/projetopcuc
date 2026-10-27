package br.edu.helpdesk.service;

import br.edu.helpdesk.entity.Account;
import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.dto.Requests.Block;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
@Transactional
public class BranchBlockService {
 private final DeskRepository repo;
 public BranchBlockService(DeskRepository repo){this.repo=repo;}
 private Account actor(){var auth=SecurityContextHolder.getContext().getAuthentication();var user=auth==null?null:repo.account(auth.getName());if(user==null||!user.ativo())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);if(!user.admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente administradores podem gerenciar blocos.");return user;}
 private void branch(Account user,long id){if(!user.master()&&!repo.branchIds(user.id()).contains(id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Filial fora dos seus locais de atuação.");var row=repo.one("SELECT ativo FROM filiais WHERE id=?",id);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Filial não encontrada.");if(!Boolean.TRUE.equals(row.get("ativo")))throw DeskService.bad("Filial inativa.");}
 public List<Map<String,Object>> list(Long branchId){var user=actor();if(branchId!=null)branch(user,branchId);String sql="SELECT b.id,b.filial_id,b.nome,b.ativo,f.nome filial_nome FROM blocos b JOIN filiais f ON f.id=b.filial_id WHERE f.ativo=true";var args=new ArrayList<Object>();if(!user.master()){sql+=" AND b.filial_id IN (SELECT filial_id FROM usuario_filiais WHERE usuario_id=?)";args.add(user.id());}if(branchId!=null){sql+=" AND b.filial_id=?";args.add(branchId);}return repo.rows(sql+" ORDER BY f.nome,b.nome,b.id",args.toArray());}
 public Map<String,Object> save(Long id,Block data){
  var user=actor();branch(user,data.filialId());String name=data.nome().trim();if(name.isEmpty())throw DeskService.bad("Informe o nome do bloco.");String key=name.toLowerCase(Locale.ROOT);
  if(id!=null){var old=repo.one("SELECT filial_id FROM blocos WHERE id=?",id);if(old==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);branch(user,((Number)old.get("filial_id")).longValue());if(!Objects.equals(((Number)old.get("filial_id")).longValue(),data.filialId()))throw DeskService.bad("A filial de um bloco existente não pode ser alterada.");}
  var existing=repo.one("SELECT id FROM blocos WHERE filial_id=? AND nome_chave=?",data.filialId(),key);if(existing!=null&&!Objects.equals(((Number)existing.get("id")).longValue(),id))throw DeskService.bad("Já existe um bloco com esse nome nesta filial. Edite ou reative o cadastro existente.");
  if(id==null)id=repo.insert("INSERT INTO blocos(filial_id,nome,nome_chave,ativo) VALUES (?,?,?,?)",data.filialId(),name,key,data.ativo());
  else{repo.update("UPDATE blocos SET nome=?,nome_chave=?,ativo=? WHERE id=?",name,key,data.ativo(),id);repo.update("UPDATE qrcodes SET bloco=? WHERE bloco_id=?",name,id);repo.update("UPDATE chamados SET bloco=? WHERE bloco_id=? AND status NOT IN ('FINALIZADO','CANCELADO')",name,id);}
  repo.update("INSERT INTO auditoria(autor_id,autor_nome,acao,descricao) VALUES (?,?,'BLOCO',?)",user.id(),user.nome(),"Bloco "+id+" atualizado na filial "+data.filialId()+".");return repo.one("SELECT id,filial_id,nome,ativo FROM blocos WHERE id=?",id);
 }
 public Map<String,Object> resolveQr(long filialId,Long blockId,String name){
  var user=actor();branch(user,filialId);var block=blockId==null?repo.one("SELECT id,filial_id,nome,ativo FROM blocos WHERE filial_id=? AND nome_chave=?",filialId,name.trim().toLowerCase(Locale.ROOT)):repo.one("SELECT id,filial_id,nome,ativo FROM blocos WHERE id=?",blockId);
  if(block==null&&blockId==null)return save(null,new Block(filialId,name,true));
  if(block==null||((Number)block.get("filial_id")).longValue()!=filialId||!Boolean.TRUE.equals(block.get("ativo")))throw DeskService.bad("Selecione um bloco ativo da filial escolhida.");return block;
 }
 public List<Map<String,Object>> priorities(long userId){return repo.rows("SELECT b.id,b.nome,b.filial_id,f.nome filial_nome FROM usuario_blocos ub JOIN blocos b ON b.id=ub.bloco_id JOIN filiais f ON f.id=b.filial_id JOIN usuario_filiais uf ON uf.usuario_id=ub.usuario_id AND uf.filial_id=b.filial_id JOIN usuarios u ON u.id=ub.usuario_id WHERE ub.usuario_id=? AND b.ativo=true AND f.ativo=true AND u.perfil='SUPORTE' ORDER BY f.nome,b.nome",userId);}
 public Set<Long> priorityIds(Account user){if(!"SUPORTE".equals(user.perfil()))return Set.of();return new HashSet<>(priorities(user.id()).stream().map(block->((Number)block.get("id")).longValue()).toList());}
 public void assign(long userId,List<Long> requested,Set<Long> branches,String role){
  var ids=new LinkedHashSet<Long>();if(requested!=null)ids.addAll(requested);else if("SUPORTE".equals(role))for(var row:repo.rows("SELECT b.id,b.filial_id FROM usuario_blocos ub JOIN blocos b ON b.id=ub.bloco_id WHERE ub.usuario_id=?",userId))if(branches.contains(((Number)row.get("filial_id")).longValue()))ids.add(((Number)row.get("id")).longValue());
  if(!ids.isEmpty()&&!"SUPORTE".equals(role))throw DeskService.bad("Blocos prioritários são atribuídos ao perfil de técnico.");
  for(long id:ids){var block=repo.one("SELECT filial_id,ativo FROM blocos WHERE id=?",id);if(block==null||!branches.contains(((Number)block.get("filial_id")).longValue())||(requested!=null&&!Boolean.TRUE.equals(block.get("ativo"))))throw DeskService.bad("Os blocos prioritários devem pertencer às filiais atribuídas ao técnico e estar ativos.");}
  repo.update("DELETE FROM usuario_blocos WHERE usuario_id=?",userId);for(long id:ids)repo.update("INSERT INTO usuario_blocos(usuario_id,bloco_id) VALUES (?,?)",userId,id);
 }
}
