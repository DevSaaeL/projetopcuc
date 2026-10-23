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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:ticket-transfer;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@transfer.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc @Transactional
class TicketTransferTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired ObjectMapper mapper;
 long branch,other,first,second,third,admin,foreign,inactive,id;
 long actor(String name,String role,long unit){long v=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?, 'unused',?,?)",name,name+"@transfer.test",role,unit);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",v,unit);return v;}
 MockHttpSession session(long actor){var s=new MockHttpSession();s.setAttribute("uid",actor);s.setAttribute("version",0);return s;}
 @BeforeEach void setup(){
  branch=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Local','Londrina','PR')");other=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Outro','Curitiba','PR')");
  first=actor("Lucas","SUPORTE",branch);second=actor("Ana","SUPORTE",branch);third=actor("Pedro","SUPORTE",branch);admin=actor("Admin","ADMIN",branch);foreign=actor("Fora","SUPORTE",other);inactive=actor("Inativo","SUPORTE",branch);repo.update("UPDATE usuarios SET ativo=false WHERE id=?",inactive);
  id=repo.insert("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,prioridade,status,tecnico_id,abertura,atendimento,sla_resposta,sla_resolucao,origem) VALUES ('Impressora','Problema','Maria',?,'Londrina','MEDIA','EM_ATENDIMENTO',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,5,10,'QR')",branch,first);
 }
 String body(long target,long source,String reason)throws Exception{return mapper.writeValueAsString(Map.of("tecnicoId",target,"tecnicoAtualId",source,"motivo",reason));}
 @Test void ownerTransfersWithReasonWithoutResettingSlaAndOnlyNewOwnerCanFinish()throws Exception{
  var before=repo.one("SELECT * FROM chamados WHERE id=?",id);
  var list=mvc.perform(get("/api/chamados/"+id+"/tecnicos-transferencia").session(session(first))).andExpect(status().isOk()).andReturn();var options=mapper.readTree(list.getResponse().getContentAsString());assertEquals(2,options.size());assertFalse(options.get(0).has("email"));
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(first)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Troca de turno"))).andExpect(status().isOk()).andExpect(jsonPath("$.tecnico_id").value(second));
  var after=repo.one("SELECT * FROM chamados WHERE id=?",id);for(String key:List.of("abertura","atendimento","inicio_ciclo","status","sla_resposta","sla_resolucao"))assertEquals(before.get(key),after.get(key));
  var audit=repo.one("SELECT * FROM auditoria WHERE chamado_id=? AND acao='TRANSFERIDO'",id);assertEquals(first,((Number)audit.get("autor_id")).longValue());assertTrue(audit.get("descricao").toString().contains("Lucas"));assertTrue(audit.get("descricao").toString().contains("Ana"));assertTrue(audit.get("descricao").toString().contains("Troca de turno"));
  assertEquals(1,repo.rows("SELECT id FROM notificacoes WHERE usuario_id=? AND chamado_id=? AND titulo='Chamado transferido para você'",second,id).size());
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(session(first)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"finalizar\",\"solucao\":\"Resolvido\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(session(second)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"finalizar\",\"solucao\":\"Resolvido\"}")).andExpect(status().isOk());
  assertEquals(second,((Number)repo.one("SELECT tecnico_id FROM atendimentos_chamado WHERE chamado_id=?",id).get("tecnico_id")).longValue());
 }
 @Test void rejectsNonOwnerAndInvalidDestinationsAndRequiresReasonAndCsrf()throws Exception{
  mvc.perform(get("/api/chamados/"+id+"/tecnicos-transferencia").session(session(third))).andExpect(status().isForbidden());
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(third)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Motivo"))).andExpect(status().isForbidden());
  for(long target:List.of(first,foreign,inactive,admin))mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(first)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(target,first,"Motivo"))).andExpect(status().isBadRequest());
  for(String reason:List.of("   ","x".repeat(1001)))mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(first)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,reason))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(first)).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Motivo"))).andExpect(status().isForbidden());
  assertEquals(first,((Number)repo.one("SELECT tecnico_id FROM chamados WHERE id=?",id).get("tecnico_id")).longValue());
 }
 @Test void administratorCanReassignWaitingTicketButStaleTransferCannotOverwriteNewOwner()throws Exception{
  repo.update("UPDATE chamados SET status='AGUARDANDO_USUARIO' WHERE id=?",id);
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Redistribuição"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGUARDANDO_USUARIO"));
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(third,first,"Tela antiga"))).andExpect(status().isConflict());
  assertEquals(second,((Number)repo.one("SELECT tecnico_id FROM chamados WHERE id=?",id).get("tecnico_id")).longValue());
 }
 @Test void respectsUnitScopeAndRejectsClosedOrUnassignedTickets()throws Exception{
  long outsideAdmin=actor("OutroAdmin","ADMIN",other);
  mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(outsideAdmin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Fora de escopo"))).andExpect(status().isNotFound());
  for(String state:List.of("FINALIZADO","CANCELADO","ABERTO")){
   repo.update("UPDATE chamados SET status=? WHERE id=?",state,id);
   mvc.perform(post("/api/chamados/"+id+"/transferir").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(second,first,"Motivo"))).andExpect(status().isBadRequest());
  }
 }
}
