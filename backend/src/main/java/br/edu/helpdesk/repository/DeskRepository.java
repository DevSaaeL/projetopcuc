package br.edu.helpdesk.repository;
import br.edu.helpdesk.entity.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository
public class DeskRepository {
 private final JdbcTemplate jdbc;
 public DeskRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public List<Map<String,Object>> rows(String sql,Object...args) {
  return jdbc.query(sql,(rs,n)->{ var row=new LinkedHashMap<String,Object>(); var meta=rs.getMetaData();
   for(int i=1;i<=meta.getColumnCount();i++){Object v=rs.getObject(i); if(v instanceof OffsetDateTime d)v=d.toInstant().toString(); if(v instanceof Timestamp d)v=d.toInstant().toString();row.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT),v);}return row;},args);
 }
 public Map<String,Object> one(String sql,Object...args){var list=rows(sql,args);return list.isEmpty()?null:list.getFirst();}
 public int update(String sql,Object...args){return jdbc.update(sql,args);}
 public long insert(String sql,Object...args){var key=new GeneratedKeyHolder(); jdbc.update(c->{var s=c.prepareStatement(sql,new String[]{"id"}); for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);return s;},key);return Objects.requireNonNull(key.getKey()).longValue();}
 public Account account(String email){return jdbc.query("SELECT * FROM usuarios WHERE email=?",(rs,n)->new Account(rs.getLong("id"),rs.getString("nome"),rs.getString("email"),rs.getString("senha_hash"),rs.getString("perfil"),(Long)rs.getObject("filial_id"),rs.getBoolean("ativo"),rs.getInt("versao_sessao"),rs.getBoolean("senha_temporaria"),rs.getBoolean("senha_local_ativa")),email).stream().findFirst().orElse(null);}
 public java.util.List<Long> branchIds(long userId){return rows("SELECT filial_id FROM usuario_filiais WHERE usuario_id=? ORDER BY filial_id",userId).stream().map(v->((Number)v.get("filial_id")).longValue()).toList();}
 public Account account(long id){var row=one("SELECT email FROM usuarios WHERE id=?",id); return row==null?null:account((String)row.get("email"));}
}
