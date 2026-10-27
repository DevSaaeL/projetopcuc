package br.edu.helpdesk;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;
class BlockMigrationTest {
 @Test void importsLegacyLocationsWithoutChangingQrIdsOrMergingBranches()throws Exception{
  String url="jdbc:h2:mem:block-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
  Flyway.configure().dataSource(url,"sa","").target("12").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var s=db.createStatement()){
   s.executeUpdate("INSERT INTO filiais(nome,cidade,estado) VALUES ('A','Curitiba','PR'),('B','Londrina','PR')");
   s.executeUpdate("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES ('old-qr-a','Sala',1,' Central ','101'),('old-qr-b','Sala',1,'CENTRAL','102'),('old-qr-c','Sala',2,'Central','101')");
   s.executeUpdate("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,bloco,prioridade,sla_resposta,sla_resolucao,origem) VALUES ('Antigo','Descrição original','Maria',1,'Curitiba','central','MEDIA',5,10,'QR')");
  }
  Flyway.configure().dataSource(url,"sa","").load().migrate();
  try(var db=DriverManager.getConnection(url,"sa","");var s=db.createStatement()){
   try(var r=s.executeQuery("SELECT COUNT(*) FROM blocos")){r.next();assertEquals(2,r.getInt(1));}
   try(var r=s.executeQuery("SELECT COUNT(*) FROM qrcodes WHERE id LIKE 'old-qr-%' AND bloco_id IS NOT NULL")){r.next();assertEquals(3,r.getInt(1));}
   try(var r=s.executeQuery("SELECT COUNT(DISTINCT bloco_id) FROM qrcodes WHERE filial_id=1")){r.next();assertEquals(1,r.getInt(1));}
   try(var r=s.executeQuery("SELECT descricao,bloco_id FROM chamados")){r.next();assertEquals("Descrição original",r.getString(1));assertNotNull(r.getObject(2));}
  }
 }
}
