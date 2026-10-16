package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:attendance-history;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@history.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
@Transactional
class AttendanceHistoryTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired ObjectMapper mapper;
 long branch(String name){return repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES (?,?,'PR')",name,name);}
 long actor(String name,String profile,long branch){long id=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?,?, ?,?)",name,name+"@history.test","unused",profile,branch);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branch);return id;}
 MockHttpSession session(long id){var s=new MockHttpSession();s.setAttribute("uid",id);s.setAttribute("version",repo.account(id).versaoSessao());return s;}
 MockHttpSession master(){return session(repo.account("master@history.test").id());}
 long ticket(long branch){return repo.insert("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,prioridade,sla_resposta,sla_resolucao,origem) VALUES ('Impressora','Falha de impressão','Maria',?,'Londrina','MEDIA',5,10,'QR')",branch);}
 JsonNode read(String path,MockHttpSession session)throws Exception{return mapper.readTree(mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 JsonNode act(long ticket,MockHttpSession session,String action,String solution)throws Exception{return mapper.readTree(mvc.perform(post("/api/chamados/"+ticket+"/acoes").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("acao",action,"solucao",solution)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 String upload(long ticket,MockHttpSession session,String name)throws Exception{mvc.perform(multipart("/api/chamados/"+ticket+"/anexos").file(new MockMultipartFile("arquivo",name,"text/plain",name.getBytes())).session(session).with(csrf())).andExpect(status().isOk());return read("/api/chamados/"+ticket+"/anexos",session).get(read("/api/chamados/"+ticket+"/anexos",session).size()-1).get("id").asText();}
 @Test void closureLeavesActiveQueueAndOnlyOwnerOrScopedAdminCanConsultIt()throws Exception{
  long a=branch("A"),b=branch("B"),one=actor("one","SUPORTE",a),two=actor("two","SUPORTE",a),adminA=actor("admin-a","ADMIN",a),adminB=actor("admin-b","ADMIN",b),id=ticket(a);
  var first=session(one);var second=session(two);assertEquals(1,read("/api/chamados",second).size());act(id,first,"assumir","");
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(second).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"finalizar\",\"solucao\":\"Forjado\"}")).andExpect(status().isForbidden());
  long record=act(id,first,"finalizar","Driver reinstalado").get("registro_atendimento").asLong();
  assertEquals(0,read("/api/chamados",first).size());assertEquals(0,read("/api/chamados",master()).size());
  assertEquals(1,read("/api/atendimentos",first).size());assertEquals(0,read("/api/atendimentos",second).size());assertEquals(1,read("/api/atendimentos",session(adminA)).size());assertEquals(0,read("/api/atendimentos",session(adminB)).size());assertEquals(1,read("/api/atendimentos",master()).size());
  assertEquals("Driver reinstalado",read("/api/atendimentos/"+record,first).get("solucao").asText());
  for(String path:List.of("/api/chamados/"+id,"/api/chamados/"+id+"/historico","/api/chamados/"+id+"/anexos","/api/atendimentos/"+record,"/api/atendimentos/"+record+"/historico","/api/atendimentos/"+record+"/anexos"))mvc.perform(get(path).session(second)).andExpect(status().isNotFound());
  mvc.perform(put("/api/atendimentos/"+record).session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Alterada\"}")).andExpect(status().isMethodNotAllowed());
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"reabrir\"}")).andExpect(status().isForbidden());
  assertEquals("Driver reinstalado",read("/api/atendimentos/"+record,first).get("solucao").asText());
 }
 @Test void reopenedTicketKeepsPreviousSolutionFilesHistoryAndTechnicianLink()throws Exception{
  long a=branch("A"),one=actor("original","SUPORTE",a),two=actor("replacement","SUPORTE",a),id=ticket(a);var first=session(one);var second=session(two);
  act(id,first,"assumir","");String oldFile=upload(id,first,"evidencia-original.txt");long original=act(id,first,"finalizar","Solução original").get("registro_atendimento").asLong();
  int oldEvents=read("/api/atendimentos/"+original+"/historico",first).size();String originalDate=read("/api/atendimentos/"+original,first).get("finalizacao").asText();
  mvc.perform(multipart("/api/chamados/"+id+"/anexos").file(new MockMultipartFile("arquivo","alteracao.txt","text/plain","x".getBytes())).session(first).with(csrf())).andExpect(status().isBadRequest());
  var reopened=act(id,master(),"reabrir","");assertNull(repo.one("SELECT tecnico_id FROM chamados WHERE id=?",id).get("tecnico_id"));assertEquals("",reopened.get("solucao").asText());assertEquals(1,read("/api/chamados",second).size());
  assertEquals(1,read("/api/atendimentos",first).size());act(id,second,"assumir","");String newFile=upload(id,second,"evidencia-nova.txt");long replacement=act(id,second,"finalizar","Solução posterior").get("registro_atendimento").asLong();
  var preserved=read("/api/atendimentos/"+original,first);assertEquals("Solução original",preserved.get("solucao").asText());assertEquals("original",preserved.get("tecnico").asText());assertEquals(originalDate,preserved.get("finalizacao").asText());assertEquals(oldEvents,read("/api/atendimentos/"+original+"/historico",first).size());assertEquals(1,read("/api/atendimentos/"+original+"/anexos",first).size());
  mvc.perform(get("/api/atendimentos/"+original+"/anexos/"+oldFile).session(first)).andExpect(status().isOk()).andExpect(content().bytes("evidencia-original.txt".getBytes()));mvc.perform(get("/api/atendimentos/"+original+"/anexos/"+newFile).session(first)).andExpect(status().isNotFound());
  mvc.perform(get("/api/atendimentos/"+replacement).session(first)).andExpect(status().isNotFound());assertEquals(1,read("/api/atendimentos",first).size());assertEquals(1,read("/api/atendimentos",second).size());assertEquals(2,read("/api/atendimentos",master()).size());
 }
 @Test void technicianRetainsOwnRecordsAfterMovingUnitsButDoesNotGainOtherRecords()throws Exception{
  long a=branch("Original"),b=branch("Nova"),one=actor("moved","SUPORTE",a),other=actor("other","SUPORTE",a),id=ticket(a);var session=session(one);act(id,session,"assumir","");long record=act(id,session,"finalizar","Serviço feito").get("registro_atendimento").asLong();
  repo.update("DELETE FROM usuario_filiais WHERE usuario_id=?",one);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",one,b);repo.update("UPDATE usuarios SET filial_id=?,versao_sessao=versao_sessao+1 WHERE id=?",b,one);
  mvc.perform(get("/api/atendimentos").session(session)).andExpect(status().isUnauthorized());
  assertEquals(1,read("/api/atendimentos",session(one)).size());assertEquals("Serviço feito",read("/api/atendimentos/"+record,session(one)).get("solucao").asText());
  mvc.perform(get("/api/chamados/"+id).session(session(one))).andExpect(status().isNotFound());mvc.perform(get("/api/atendimentos/"+record).session(session(other))).andExpect(status().isNotFound());
 }
 @Test void cancellationBeforePickupIsArchivedForActingTechnician()throws Exception{
  long a=branch("Cancelamento"),one=actor("cancel-tech","SUPORTE",a),id=ticket(a);var session=session(one);act(id,session,"cancelar","");assertEquals(0,read("/api/chamados",session).size());var history=read("/api/atendimentos",session);assertEquals(1,history.size());assertEquals("CANCELADO",history.get(0).get("status").asText());
 }
}
