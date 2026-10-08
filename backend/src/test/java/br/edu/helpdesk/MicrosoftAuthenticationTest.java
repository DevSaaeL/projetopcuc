package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.security.MicrosoftLoginSuccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:microsoft-auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@microsoft.test","app.bootstrap-password=OnlyForTests-123456","app.bootstrap-microsoft-object-id=66666666-6666-6666-6666-666666666666","app.microsoft.enabled=true","app.microsoft.tenant-id=11111111-1111-1111-1111-111111111111","app.microsoft.client-id=22222222-2222-2222-2222-222222222222","app.microsoft.client-secret=TestOnlySecret","app.microsoft.redirect-uri=https://helpdesk.example.org/login/oauth2/code/microsoft"})
@AutoConfigureMockMvc
class MicrosoftAuthenticationTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired MicrosoftLoginSuccess success;
 static final String TENANT="11111111-1111-1111-1111-111111111111",OID="33333333-3333-3333-3333-333333333333";
 MockHttpServletRequest request(){return new MockHttpServletRequest();}
 MockHttpServletResponse complete(String tenant,String oid,String email,MockHttpServletRequest request)throws Exception{
  var claims=new HashMap<String,Object>();claims.put("sub",oid);claims.put("oid",oid);claims.put("tid",tenant);claims.put("preferred_username",email);claims.put("roles",List.of("MASTER_ADMIN"));
  var principal=new DefaultOidcUser(List.of(),new OidcIdToken("verified-in-test",Instant.now(),Instant.now().plusSeconds(600),claims));
  var response=new MockHttpServletResponse();success.onAuthenticationSuccess(request,response,new UsernamePasswordAuthenticationToken(principal,null,List.of()));return response;
 }
 @Test void loginUsesSingleTenantAndDisablesLocalCredentials()throws Exception{
  var redirect=mvc.perform(get("/oauth2/authorization/microsoft")).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
  assertTrue(redirect.startsWith("https://login.microsoftonline.com/"+TENANT+"/oauth2/v2.0/authorize?"));assertTrue(redirect.contains("state="));assertTrue(redirect.contains("nonce="));assertTrue(redirect.contains("response_type=code"));assertTrue(redirect.contains("redirect_uri=https://helpdesk.example.org/login/oauth2/code/microsoft"));
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"master@microsoft.test\",\"password\":\"OnlyForTests-123456\"}")).andExpect(status().isGone());
  mvc.perform(get("/login/oauth2/code/microsoft").param("code","forged").param("state","forged")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pages/login.html?sso=failed"));
  mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
 }
 @Test void immutableIdentityAndTenantControlAccessRatherThanEmailOrTokenRoles()throws Exception{
  long branch=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('PUC Londrina','Londrina','PR')");
  long uid=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id,microsoft_object_id) VALUES ('Samuel','samuel@microsoft.test','unusable','SUPORTE',?,?)",branch,OID);
  repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",uid,branch);
  var request=request();assertEquals("/pages/login.html?sso=success",complete(TENANT,OID,"changed-name@microsoft.test",request).getRedirectedUrl());
  assertEquals(uid,request.getSession().getAttribute("uid"));
  mvc.perform(get("/api/auth/me").session((MockHttpSession)request.getSession())).andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("SUPORTE")).andExpect(jsonPath("$.filial_ids[0]").value(branch));
  assertEquals("/pages/login.html?sso=unassigned",complete("44444444-4444-4444-4444-444444444444",OID,"samuel@microsoft.test",request()).getRedirectedUrl());
  assertEquals("/pages/login.html?sso=unassigned",complete(TENANT,"55555555-5555-5555-5555-555555555555","samuel@microsoft.test",request()).getRedirectedUrl());
  repo.update("UPDATE usuarios SET ativo=false WHERE id=?",uid);
  mvc.perform(get("/api/auth/me").session((MockHttpSession)request.getSession())).andExpect(status().isUnauthorized());
  assertEquals("/pages/login.html?sso=unassigned",complete(TENANT,OID,"samuel@microsoft.test",request()).getRedirectedUrl());
 }
}
