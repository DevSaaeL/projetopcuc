package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:local-auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@local.test","app.bootstrap-password=OnlyForTests-123456","app.microsoft.enabled=true"})
@AutoConfigureMockMvc
class LocalAuthenticationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired DeskRepository repo;
 @Test void localAdminLoginGenerationAndPasswordChangeInvalidateOtherSessions()throws Exception{
  String credentials="{\"username\":\"master@local.test\",\"password\":\"OnlyForTests-123456\"}";
  var session=(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials)).andExpect(status().isOk()).andReturn().getRequest().getSession();
  var other=(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials)).andExpect(status().isOk()).andReturn().getRequest().getSession();
  mvc.perform(get("/oauth2/authorization/microsoft")).andExpect(status().isNotFound());
  mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.microsoftEnabled").value(false));
  mvc.perform(post("/api/usuarios/gerar-senha").with(csrf())).andExpect(status().isUnauthorized());
  var generated=mapper.readTree(mvc.perform(post("/api/usuarios/gerar-senha").session(session).with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("senha").asText();
  assertEquals(16,generated.length());assertTrue(generated.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$"));
  mvc.perform(put("/api/auth/senha").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"wrong\",\"nova\":\"AnotherPass1!\",\"confirmacao\":\"AnotherPass1!\"}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/auth/senha").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"OnlyForTests-123456\",\"nova\":\"AnotherPass1!\",\"confirmacao\":\"AnotherPass1!\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").session(other)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials)).andExpect(status().isUnauthorized());
 }
}
