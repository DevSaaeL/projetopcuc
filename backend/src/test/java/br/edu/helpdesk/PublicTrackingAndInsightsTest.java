package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tracking-insights;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=tracking@admin.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
@Transactional
class PublicTrackingAndInsightsTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired ObjectMapper mapper;@Autowired PublicTicketService publicTickets;
 long branch(String name){long id=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES (?,?,'PR')",name,name);TicketTopicService.seed(repo,id);return id;}
 long topic(long branch,String name){return ((Number)repo.one("SELECT id FROM assuntos_chamado WHERE filial_id=? AND nome=?",branch,name).get("id")).longValue();}
 String qr(long branch,String room){String id=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES (?, ?,?,'Central',?)",id,room,branch,room);return id;}
 MockHttpSession session(long id){var session=new MockHttpSession();session.setAttribute("uid",id);session.setAttribute("version",0);return session;}
 long user(long branch,String role){String name=UUID.randomUUID().toString();long id=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?, 'unused',?,?)",name,name+"@test.invalid",role,branch);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branch);return id;}
 JsonNode submit(String qr,long topic)throws Exception{return mapper.readTree(mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("qrId",qr,"nome","Nome privado","assuntoId",topic,"requestId",UUID.randomUUID().toString())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void duplicateRemainsBlockedAfterPickupAndQueueUpdatesAfterClosure()throws Exception{
  long a=branch("Fila A"),b=branch("Fila B"),tech=user(a,"SUPORTE");String firstQr=qr(a,"101"),secondQr=qr(a,"102");
  var first=submit(firstQr,topic(a,"Impressora"));long firstId=((Number)repo.one("SELECT id FROM chamados WHERE acompanhamento_token=?",first.get("acompanhamento_token").asText()).get("id")).longValue();
  repo.update("UPDATE chamados SET abertura=?,inicio_ciclo=? WHERE id=?",OffsetDateTime.now().minusMinutes(1),OffsetDateTime.now().minusMinutes(1),firstId);
  var second=submit(secondQr,topic(a,"Impressora"));assertEquals(2,second.get("posicao_fila").asInt());
  submit(qr(b,"101"),topic(b,"Impressora"));
  mvc.perform(post("/api/chamados/"+firstId+"/acoes").session(session(tech)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isOk());
  var duplicate=submit(qr(a,"101"),topic(a,"Impressora"));assertTrue(duplicate.get("ja_existente").asBoolean());assertEquals(first.get("protocolo"),duplicate.get("protocolo"));assertEquals(1,duplicate.get("posicao_fila").asInt());
  assertFalse(duplicate.toString().contains("Nome privado"));assertFalse(duplicate.has("solicitante_nome"));assertFalse(duplicate.has("tecnico_id"));
  mvc.perform(get("/api/public/acompanhamento/"+second.get("acompanhamento_token").asText())).andExpect(status().isOk()).andExpect(jsonPath("$.posicao_fila").value(2));
  mvc.perform(post("/api/chamados/"+firstId+"/acoes").session(session(tech)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"finalizar\",\"solucao\":\"Resolvido\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/public/acompanhamento/"+second.get("acompanhamento_token").asText())).andExpect(jsonPath("$.posicao_fila").value(1));
  mvc.perform(get("/api/public/acompanhamento/"+first.get("acompanhamento_token").asText())).andExpect(jsonPath("$.status").value("FINALIZADO")).andExpect(jsonPath("$.posicao_fila").isEmpty());
  var reopened=submit(firstQr,topic(a,"Impressora"));assertNotEquals(first.get("protocolo"),reopened.get("protocolo"));
  mvc.perform(get("/api/public/acompanhamento/"+UUID.randomUUID())).andExpect(status().isNotFound());
 }
 @Test void duplicateDefinitionSeparatesTopicsRoomsAndBlocks()throws Exception{
  long a=branch("Locais");String q=qr(a,"101");var first=submit(q,topic(a,"Impressora"));
  assertNotEquals(first.get("protocolo"),submit(q,topic(a,"Projetor")).get("protocolo"));
  assertNotEquals(first.get("protocolo"),submit(qr(a,"102"),topic(a,"Impressora")).get("protocolo"));
  String another=qr(a,"101");repo.update("UPDATE qrcodes SET bloco='Fôlego' WHERE id=?",another);
  assertNotEquals(first.get("protocolo"),submit(another,topic(a,"Impressora")).get("protocolo"));
  mvc.perform(get("/api/public/qrcodes/"+q)).andExpect(jsonPath("$.chamados_ativos.length()").value(2)).andExpect(jsonPath("$.chamados_ativos[0].solicitante_nome").doesNotExist());
 }
 @Test void duplicateRequesterCannotCancelOriginalAndPublicTrackingDoesNotExposePrivateReason()throws Exception{
  long unit=branch("Cancelamento protegido"),topic=topic(unit,"Impressora");String qr=qr(unit,"101"),ownerKey="a".repeat(64),otherKey="b".repeat(64);
  var first=publicTickets.create(qr,"Primeiro solicitante",topic,UUID.randomUUID().toString(),ownerKey);
  var duplicate=publicTickets.create(qr,"Outra pessoa",topic,UUID.randomUUID().toString(),otherKey);
  assertEquals(first.get("protocolo"),duplicate.get("protocolo"));assertEquals(true,duplicate.get("ja_existente"));assertFalse(duplicate.containsKey("id"));assertFalse(duplicate.containsKey("motivo_cancelamento"));
  long id=((Number)first.get("id")).longValue();
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("chave",otherKey,"motivo","Não sou o autor")))).andExpect(status().isNotFound());
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("chave",ownerKey,"motivo","Motivo privado")))).andExpect(status().isOk());
  mvc.perform(get("/api/public/acompanhamento/"+first.get("acompanhamento_token"))).andExpect(jsonPath("$.status").value("CANCELADO")).andExpect(jsonPath("$.motivo_cancelamento").doesNotExist());
  assertNotEquals(first.get("protocolo"),publicTickets.create(qr,"Nova solicitação",topic,UUID.randomUUID().toString(),otherKey).get("protocolo"));
 }
 @Test void adminInsightsRespectBranchScopeAndRejectTechnicians()throws Exception{
  long a=branch("Recorrência A"),b=branch("Recorrência B"),admin=user(a,"ADMIN"),tech=user(a,"SUPORTE");
  String local=qr(a,"101");submit(local,topic(a,"Impressora"));submit(local,topic(a,"Projetor"));submit(qr(b,"999"),topic(b,"Impressora"));
  mvc.perform(get("/api/admin/recorrencias")).andExpect(status().isUnauthorized());
  for(String path:List.of("/api/admin/recorrencias","/api/admin/saude"))mvc.perform(get(path).session(session(tech))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/recorrencias").session(session(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.por_local[0].total").value(2)).andExpect(jsonPath("$.por_local[0].sala").value("101"));
  mvc.perform(get("/api/admin/recorrencias?filial_id="+b).session(session(admin))).andExpect(jsonPath("$.total").value(0));
  mvc.perform(get("/api/admin/recorrencias?inicio=2026-12-31&fim=2026-01-01").session(session(admin))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/admin/saude").session(session(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SAUDAVEL")).andExpect(jsonPath("$.migracao").value("13")).andExpect(jsonPath("$.banco_latencia_ms").isNumber());
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void simultaneousSubmissionsCreateOnlyOneTicket()throws Exception{
  long unit=branch("Concorrência"),topic=topic(unit,"Impressora");String first=qr(unit,"101"),second=qr(unit,"101");
  try(var executor=Executors.newFixedThreadPool(2)){
   var gate=new CountDownLatch(1);
   var one=executor.submit(()->{gate.await();return publicTickets.create(first,"Pessoa A",topic,UUID.randomUUID().toString(),null);});
   var two=executor.submit(()->{gate.await();return publicTickets.create(second,"Pessoa B",topic,UUID.randomUUID().toString(),null);});
   gate.countDown();assertEquals(one.get(10,TimeUnit.SECONDS).get("protocolo"),two.get(10,TimeUnit.SECONDS).get("protocolo"));
  }
  assertEquals(1,repo.rows("SELECT id FROM chamados WHERE filial_id=?",unit).size());
 }
}
