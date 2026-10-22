package br.edu.helpdesk;
import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:requester-cancel;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@cancel.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc @Transactional
class RequesterCancellationTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired ObjectMapper mapper;
 String key="a".repeat(64);long unit,tech,id;MockHttpSession support,master;
 @BeforeEach void setup()throws Exception{
  unit=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Unidade','Londrina','PR')");br.edu.helpdesk.service.TicketTopicService.seed(repo,unit);
  tech=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES ('Tecnico','tech@cancel.test','unused','SUPORTE',?)",unit);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",tech,unit);
  support=session(tech);master=session(((Number)repo.one("SELECT id FROM usuarios WHERE perfil='MASTER_ADMIN'").get("id")).longValue());
  String qr=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES (?,'Sala',?,'A','1')",qr,unit);
  long topic=((Number)repo.one("SELECT id FROM assuntos_chamado WHERE filial_id=? LIMIT 1",unit).get("id")).longValue();
  var data=Map.of("qrId",qr,"nome","Maria","assuntoId",topic,"requestId",UUID.randomUUID().toString(),"chave",key);
  var result=mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data))).andExpect(status().isOk()).andReturn();id=mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
 }
 MockHttpSession session(long id){var s=new MockHttpSession();s.setAttribute("uid",id);s.setAttribute("version",0);return s;}
 String body(String key,String reason)throws Exception{return mapper.writeValueAsString(Map.of("chave",key,"motivo",reason));}
 @Test void ownerCancellationPreservesReasonAuthorAndArchiveAndNotifiesTeam()throws Exception{
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(support).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isOk());
  for(int i=0;i<2;i++)mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(key,"Problema resolvido"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
  mvc.perform(get("/api/chamados/"+id).session(support)).andExpect(jsonPath("$.cancelado_por_solicitante").value(true)).andExpect(jsonPath("$.motivo_cancelamento").value("Problema resolvido")).andExpect(jsonPath("$.chave_cancelamento_hash").doesNotExist());
  mvc.perform(get("/api/chamados/"+id+"/historico").session(support)).andExpect(jsonPath("$[2].autor").value("Maria")).andExpect(jsonPath("$[2].acao").value("CANCELADO_SOLICITANTE"));
  var archive=repo.rows("SELECT * FROM atendimentos_chamado WHERE chamado_id=?",id);assertEquals(1,archive.size());assertEquals("Maria",archive.getFirst().get("finalizado_por_nome"));assertEquals("Problema resolvido",archive.getFirst().get("motivo_cancelamento"));
  assertEquals(1,repo.rows("SELECT id FROM notificacoes WHERE usuario_id=? AND titulo='Chamado cancelado pelo solicitante'",tech).size());
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(master).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"reabrir\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(key,"Outra razão"))).andExpect(status().isNotFound());
  assertEquals("Problema resolvido",repo.one("SELECT motivo_cancelamento FROM atendimentos_chamado WHERE chamado_id=?",id).get("motivo_cancelamento"));
 }
 @Test void wrongKeysMissingCsrfAndInvalidReasonsCannotCancel()throws Exception{
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").contentType(MediaType.APPLICATION_JSON).content(body(key,"Resolvido"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("b".repeat(64),"Resolvido"))).andExpect(status().isNotFound());
  for(String reason:List.of("   ","x".repeat(1001)))mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(key,reason))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/public/chamados/"+(id+999)+"/consultar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(key,""))).andExpect(status().isNotFound());
  assertEquals("ABERTO",repo.one("SELECT status FROM chamados WHERE id=?",id).get("status"));
 }
 @Test void completedTicketCannotBeCancelled()throws Exception{
  repo.update("UPDATE chamados SET status='FINALIZADO',finalizacao=CURRENT_TIMESTAMP WHERE id=?",id);
  mvc.perform(post("/api/public/chamados/"+id+"/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(key,"Resolvido"))).andExpect(status().isConflict());
 }
}
