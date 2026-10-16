package br.edu.helpdesk;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;
class AttendanceHistoryMigrationTest {
 @Test void existingClosedTicketAndAttachmentAreBackfilled()throws Exception{
  String url="jdbc:h2:mem:attendance-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
  Flyway.configure().dataSource(url,"sa","").target("9").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var sql=db.createStatement()){
   sql.executeUpdate("INSERT INTO filiais(nome,cidade,estado) VALUES ('Unidade','Londrina','PR')");
   sql.executeUpdate("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES ('Técnico original','legacy@example.org','unused','SUPORTE',1)");
   sql.executeUpdate("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,prioridade,sla_resposta,sla_resolucao,origem,status,tecnico_id,atendimento,finalizacao,solucao) VALUES ('Impressora','Falha','Maria',1,'Londrina','MEDIA',5,10,'QR','FINALIZADO',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Solução antiga')");
   sql.executeUpdate("INSERT INTO auditoria(autor_id,chamado_id,acao,descricao) VALUES (1,1,'FINALIZADO','Solução antiga')");
   try(var insert=db.prepareStatement("INSERT INTO anexos(id,chamado_id,autor_id,nome,tamanho,conteudo) VALUES ('arquivo',1,1,'evidencia.txt',1,?)")){insert.setBytes(1,new byte[]{1});insert.executeUpdate();}
  }
  Flyway.configure().dataSource(url,"sa","").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var sql=db.createStatement()){
   try(var rows=sql.executeQuery("SELECT tecnico_nome,solucao,ultimo_evento_id,finalizado_por_nome FROM atendimentos_chamado")){assertTrue(rows.next());assertEquals("Técnico original",rows.getString(1));assertEquals("Solução antiga",rows.getString(2));assertEquals(1,rows.getLong(3));assertEquals("Técnico original",rows.getString(4));}
   try(var rows=sql.executeQuery("SELECT anexo_id FROM atendimento_anexos")){assertTrue(rows.next());assertEquals("arquivo",rows.getString(1));}
   try(var rows=sql.executeQuery("SELECT solucao FROM chamados WHERE id=1")){assertTrue(rows.next());assertEquals("Solução antiga",rows.getString(1));}
  }
 }
}
