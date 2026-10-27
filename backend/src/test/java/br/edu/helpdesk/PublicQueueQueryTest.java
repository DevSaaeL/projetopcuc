package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:queue-query-budget;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=","app.bootstrap-password="})
@Transactional
class PublicQueueQueryTest {
 @MockitoSpyBean DeskRepository repo;@Autowired PublicTicketService service;
 @Test void roomQueueUsesThreeReadsRegardlessOfTheNumberOfTickets(){
  long branch=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Londrina','Londrina','PR')");TicketTopicService.seed(repo,branch);
  var topics=repo.rows("SELECT id FROM assuntos_chamado WHERE filial_id=? ORDER BY id",branch);
  String room=UUID.randomUUID().toString(),otherRoom=UUID.randomUUID().toString();
  for(String qr:List.of(room,otherRoom))repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES (?,'Sala',?,'Central',?)",qr,branch,qr.equals(room)?"101":"102");
  service.create(otherRoom,"Outro local",((Number)topics.getFirst().get("id")).longValue(),UUID.randomUUID().toString(),null);
  for(int i=0;i<8;i++)service.create(room,"Pessoa "+i,((Number)topics.get(i).get("id")).longValue(),UUID.randomUUID().toString(),null);
  clearInvocations(repo);
  var location=service.location(room);
  @SuppressWarnings("unchecked") var active=(List<Map<String,Object>>)location.get("chamados_ativos");
  assertEquals(8,active.size());assertEquals(2,((Number)active.getFirst().get("posicao_fila")).intValue());assertEquals(9,((Number)active.getLast().get("posicao_fila")).intValue());
  long reads=mockingDetails(repo).getInvocations().stream().filter(call->call.getMethod().getName().equals("rows")).count();
  assertEquals(3,reads,"A consulta da sala deve carregar localização, assuntos e fila em lote, sem consultas por chamado.");
  for(var ticket:active){assertNotNull(ticket.get("acompanhamento_token"));assertFalse(ticket.containsKey("solicitante_nome"));assertFalse(ticket.containsKey("chave_cancelamento_hash"));}
  clearInvocations(repo);
  var tracked=service.tracking(active.getFirst().get("acompanhamento_token").toString());
  assertEquals(2,((Number)tracked.get("posicao_fila")).intValue());
  assertEquals(1,mockingDetails(repo).getInvocations().stream().filter(call->call.getMethod().getName().equals("rows")).count(),"A atualização do link público deve consultar ticket e fila juntos.");
 }
}
