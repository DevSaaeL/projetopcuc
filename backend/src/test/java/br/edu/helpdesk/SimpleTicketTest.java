package br.edu.helpdesk;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:simple-ticket;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=admin@test.invalid","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
class SimpleTicketTest {
 @Autowired br.edu.helpdesk.repository.DeskRepository repo; @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 private MockHttpSession login(String email)throws Exception {
  return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
   .content(mapper.writeValueAsString(java.util.Map.of("username",email,"password","OnlyForTests-123456"))))
   .andExpect(status().isOk()).andReturn().getRequest().getSession();
 }
 private JsonNode create(MockHttpSession session,String path,String json)throws Exception {
  return mapper.readTree(mvc.perform(post(path).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
 }
 @Test void simplifiedTicketDefaultsAndNotifiesOnlyItsSupportTeam()throws Exception {
  var admin=login("admin@test.invalid");
  long branch=create(admin,"/api/filiais","{\"nome\":\"Unidade A\",\"cidade\":\"Curitiba\",\"estado\":\"PR\",\"ativo\":true}").get("id").asLong();
  long other=create(admin,"/api/filiais","{\"nome\":\"Unidade B\",\"cidade\":\"Londrina\",\"estado\":\"PR\",\"ativo\":true}").get("id").asLong();
  for(var entry:java.util.Map.of("requester",branch,"technician",branch,"other-technician",other).entrySet()) {
   create(admin,"/api/usuarios",mapper.writeValueAsString(java.util.Map.of("nome",entry.getKey(),"email",entry.getKey()+"@test.invalid","perfil",entry.getKey().equals("requester")?"USUARIO":"SUPORTE","filialId",entry.getValue(),"ativo",true)));
  }
  repo.update("UPDATE usuarios SET senha_hash=? WHERE email<>'admin@test.invalid'",encoder.encode("OnlyForTests-123456"));
  var requester=login("requester@test.invalid");var technician=login("technician@test.invalid");var foreign=login("other-technician@test.invalid");
  var ticket=create(requester,"/api/chamados","{\"titulo\":\"Não consigo imprimir\",\"descricao\":\"A impressora está sem conexão\",\"filialId\":"+branch+"}");
  assertEquals("MEDIA",ticket.get("prioridade").asText());
  assertEquals("",ticket.get("setor").asText());
  long id=ticket.get("id").asLong();
  var notifications=mapper.readTree(mvc.perform(get("/api/notificacoes").session(technician)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  assertEquals(1,notifications.size());assertEquals(id,notifications.get(0).get("chamado_id").asLong());assertEquals("Novo chamado",notifications.get(0).get("titulo").asText());
  assertTrue(mapper.readTree(mvc.perform(get("/api/notificacoes").session(foreign)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isEmpty());
  assertEquals(1,mapper.readTree(mvc.perform(get("/api/chamados").session(technician)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).size());
  mvc.perform(post("/api/chamados").session(requester).with(csrf()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"titulo\":\"Inválido\",\"descricao\":\"Teste\",\"filialId\":"+branch+",\"prioridade\":\"URGENTE\"}")).andExpect(status().isBadRequest());
 }
}
