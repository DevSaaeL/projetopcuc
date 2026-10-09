package br.edu.helpdesk;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "app.bootstrap-email=master@test.invalid", "app.bootstrap-password=OnlyForTests-123456", "app.public-url=https://helpdesk.example.org/projeto/frontend/pages/novo-chamado.html"})
@AutoConfigureMockMvc
class WorkflowTest {
 @Autowired br.edu.helpdesk.repository.DeskRepository repo; @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 JsonNode body(MvcResult result)throws Exception{return mapper.readTree(result.getResponse().getContentAsString());}
 MockHttpSession login(String email)throws Exception{return login(email,"OnlyForTests-123456");}
 MockHttpSession login(String email,String password)throws Exception{var account=repo.account(email);if(account!=null&&"USUARIO".equals(account.perfil())){var session=new MockHttpSession();session.setAttribute("uid",account.id());session.setAttribute("version",account.versaoSessao());return session;}return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();}
 JsonNode create(MockHttpSession session,String url,String json)throws Exception{var data=mapper.readTree(json);if(url.equals("/api/usuarios")){((com.fasterxml.jackson.databind.node.ObjectNode)data).remove("senha");json=mapper.writeValueAsString(data);}var result=body(mvc.perform(post(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk()).andReturn());if(url.equals("/api/usuarios")){repo.update("UPDATE usuarios SET senha_hash=? WHERE id=?",encoder.encode("OnlyForTests-123456"),result.get("id").asLong());repo.update("UPDATE usuarios SET senha_temporaria=false WHERE id=?",result.get("id").asLong());}return result;}
 @Test void selfRegistrationAndLocalPasswordChangesAreDisabled()throws Exception{
  mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"public@test.invalid\",\"senha\":\"RegisterTest-123456\"}")).andExpect(status().isGone());
 }
 @Test void completeWorkflowAndIsolation()throws Exception{
  mvc.perform(get("/api/chamados")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"master@test.invalid\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  var master=login("master@test.invalid");
  long a=create(master,"/api/filiais","{\"nome\":\"Londrina\",\"cidade\":\"Londrina\",\"estado\":\"PR\",\"ativo\":true}").get("id").asLong();
  long b=create(master,"/api/filiais","{\"nome\":\"Curitiba\",\"cidade\":\"Curitiba\",\"estado\":\"PR\",\"ativo\":true}").get("id").asLong();
  String[] emails={"user@test.invalid","other@test.invalid","support@test.invalid","support2@test.invalid","foreign@test.invalid","admin@test.invalid"};String[] roles={"USUARIO","USUARIO","SUPORTE","SUPORTE","SUPORTE","ADMIN"};
  for(int i=0;i<emails.length;i++)create(master,"/api/usuarios","{\"nome\":\"Pessoa "+i+"\",\"email\":\""+emails[i]+"\",\"senha\":\"OnlyForTests-123456\",\"perfil\":\""+roles[i]+"\",\"filialId\":"+(i==4?b:a)+",\"ativo\":true}");
  var user=login(emails[0]);var other=login(emails[1]);var support=login(emails[2]);var support2=login(emails[3]);var foreign=login(emails[4]);var admin=login(emails[5]);
  mvc.perform(get("/api/usuarios").session(user)).andExpect(status().isForbidden());
  mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Escalate\",\"email\":\"escalate@test.invalid\",\"senha\":\"OnlyForTests-123456\",\"perfil\":\"MASTER_ADMIN\",\"ativo\":true}")).andExpect(status().isForbidden());
  var qr=create(master,"/api/qrcodes","{\"nome\":\"Sala 05\",\"filialId\":"+a+",\"bloco\":\"A\",\"sala\":\"05\"}");
  var t=create(user,"/api/chamados","{\"titulo\":\"Internet\",\"descricao\":\"Sem conexão\",\"prioridade\":\"ALTA\",\"filialId\":"+b+",\"bloco\":\"FORGED\",\"sala\":\"999\",\"qrId\":\""+qr.get("id").asText()+"\"}");long id=t.get("id").asLong();assertEquals(a,t.get("filial_id").asLong());assertEquals("05",t.get("sala").asText());
  assertFalse(t.has("senha_hash"));assertTrue(body(mvc.perform(get("/api/chamados").session(foreign)).andExpect(status().isOk()).andReturn()).isEmpty());
  for(var session:new MockHttpSession[]{foreign,other}){mvc.perform(get("/api/chamados/"+id).session(session)).andExpect(status().isNotFound());mvc.perform(get("/api/chamados/"+id+"/mensagens").session(session)).andExpect(status().isNotFound());mvc.perform(get("/api/chamados/"+id+"/anexos").session(session)).andExpect(status().isNotFound());}
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isForbidden());
  var taken=create(support,"/api/chamados/"+id+"/acoes","{\"acao\":\"assumir\"}");assertEquals("EM_ATENDIMENTO",taken.get("status").asText());assertFalse(taken.get("atendimento").isNull());
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(support2).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isConflict());
  mvc.perform(post("/api/chamados/"+id+"/mensagens").session(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\"Ainda sem conexão <script>alert(1)</script>\"}")).andExpect(status().isOk());
  var file=new org.springframework.mock.web.MockMultipartFile("arquivo","evidencia.txt","text/plain","Teste".getBytes());
  mvc.perform(multipart("/api/chamados/"+id+"/anexos").file(file).session(user).with(csrf())).andExpect(status().isOk());
  var attachment=body(mvc.perform(get("/api/chamados/"+id+"/anexos").session(user)).andReturn()).get(0).get("id").asText();
  mvc.perform(get("/api/chamados/"+id+"/anexos/"+attachment).session(foreign)).andExpect(status().isNotFound());
  mvc.perform(get("/api/chamados/"+id+"/anexos/"+attachment).session(user)).andExpect(status().isOk()).andExpect(content().bytes("Teste".getBytes()));
  var ended=create(support2,"/api/chamados/"+id+"/acoes","{\"acao\":\"finalizar\",\"solucao\":\"Cabo substituído\"}");assertEquals("FINALIZADO",ended.get("status").asText());assertFalse(ended.get("finalizacao").isNull());
  assertEquals(1,body(mvc.perform(get("/api/chamados/"+id+"/mensagens").session(user)).andReturn()).size());
  var notifications=body(mvc.perform(get("/api/notificacoes").session(user)).andReturn());assertFalse(notifications.isEmpty());long nid=notifications.get(0).get("id").asLong();mvc.perform(put("/api/notificacoes/"+nid+"/lida").session(other).with(csrf())).andExpect(status().isNotFound());
  assertEquals(0,body(mvc.perform(get("/api/dashboard").session(foreign)).andReturn()).get("total").asInt());assertEquals(1,body(mvc.perform(get("/api/relatorios").param("status","FINALIZADO").session(master)).andReturn()).get("total").asInt());
  mvc.perform(put("/api/auth/senha").session(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"OnlyForTests-123456\",\"nova\":\"AnotherTest-123456\"}")).andExpect(status().isGone());
  var secondSession=login(emails[0]);mvc.perform(post("/api/auth/encerrar-sessoes").session(user).with(csrf())).andExpect(status().isOk());mvc.perform(get("/api/auth/me").session(secondSession)).andExpect(status().isUnauthorized());
 }
}









