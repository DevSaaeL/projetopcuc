package br.edu.helpdesk;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;
class TicketTopicsMigrationTest {
 @Test void existingUnitsReceiveDefaultsWithoutChangingOldTickets()throws Exception{
  String url="jdbc:h2:mem:topic-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
  Flyway.configure().dataSource(url,"sa","").target("8").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var statement=db.createStatement()){
   statement.executeUpdate("INSERT INTO filiais(nome,cidade,estado) VALUES ('Unidade antiga','Londrina','PR'),('Outra unidade','Curitiba','PR')");
   statement.executeUpdate("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,prioridade,sla_resposta,sla_resolucao,origem) VALUES ('Chamado antigo','Descrição original','Maria',1,'Londrina','MEDIA',5,10,'QR')");
  }
  Flyway.configure().dataSource(url,"sa","").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var statement=db.createStatement()){
   try(var rows=statement.executeQuery("SELECT COUNT(*) FROM assuntos_chamado WHERE ativo=true")){assertTrue(rows.next());assertEquals(18,rows.getInt(1));}
   try(var rows=statement.executeQuery("SELECT titulo,descricao,assunto_id FROM chamados")){assertTrue(rows.next());assertEquals("Chamado antigo",rows.getString(1));assertEquals("Descrição original",rows.getString(2));assertNull(rows.getObject(3));}
  }
 }
}
