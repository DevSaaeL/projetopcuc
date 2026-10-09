package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:technician-password;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@temporary.test","app.bootstrap-password=OnlyForTests-123456","app.bootstrap-microsoft-object-id=66666666-6666-6666-6666-666666666666","app.microsoft.enabled=true","app.microsoft.tenant-id=11111111-1111-1111-1111-111111111111","app.microsoft.client-id=22222222-2222-2222-2222-222222222222","app.microsoft.client-secret=TestOnlySecret","app.microsoft.redirect-uri=https://helpdesk.example.org/login/oauth2/code/microsoft"})
@AutoConfigureMockMvc
class TechnicianPasswordTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired DeskRepository repo;
 JsonNode body(org.springframework.test.web.servlet.MvcResult response)throws Exception{return mapper.readTree(response.getResponse().getContentAsString());}
 @Test void createsTemporaryCredentialAndRequiresStrongPasswordBeforeUsingSystem()throws Exception{
  var admin=repo.account("master@temporary.test");var session=new MockHttpSession();session.setAttribute("uid",admin.id());session.setAttribute("version",admin.versaoSessao());
  var branch=body(mvc.perform(post("/api/filiais").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"PUC Curitiba\",\"cidade\":\"Curitiba\",\"estado\":\"PR\",\"ativo\":true}")).andExpect(status().isOk()).andReturn());
  long branchId=branch.get("id").asLong();
  var created=body(mvc.perform(post("/api/usuarios").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Técnico Novo\",\"email\":\"tech@temporary.test\",\"perfil\":\"SUPORTE\",\"filialIds\":["+branchId+"],\"ativo\":true}")).andExpect(status().isOk()).andReturn());
  String temporary=created.get("senhaTemporaria").asText();assertTrue(temporary.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$"));assertTrue(repo.account("tech@temporary.test").senhaTemporaria());assertTrue(repo.account("tech@temporary.test").senhaLocalAtiva());
  var login=body(mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"tech@temporary.test\",\"password\":\""+temporary+"\"}")).andExpect(status().isOk()).andReturn());assertTrue(login.get("mustChangePassword").asBoolean());
  var tech=(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"tech@temporary.test\",\"password\":\""+temporary+"\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();
  mvc.perform(get("/api/auth/me").session(tech)).andExpect(status().isOk()).andExpect(jsonPath("$.senha_temporaria").value(true));
  mvc.perform(get("/api/chamados").session(tech)).andExpect(status().isForbidden());
  mvc.perform(put("/api/auth/primeira-senha").session(tech).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nova\":\"weakpass\"}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/auth/primeira-senha").session(tech).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nova\":\"StrongPass1!\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").session(tech)).andExpect(status().isOk()).andExpect(jsonPath("$.senha_temporaria").value(false));
  mvc.perform(get("/api/chamados").session(tech)).andExpect(status().isOk());
 }
}
