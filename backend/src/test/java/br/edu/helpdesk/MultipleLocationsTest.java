package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:multiple-locations;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@locations.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
class MultipleLocationsTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired DeskRepository repo;
 JsonNode body(MvcResult result)throws Exception{return mapper.readTree(result.getResponse().getContentAsString());}
 MockHttpSession session(long id){var session=new MockHttpSession();session.setAttribute("uid",id);session.setAttribute("version",repo.account(id).versaoSessao());return session;}
 JsonNode create(MockHttpSession session,String path,Object data)throws Exception{var result=body(mvc.perform(post(path).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data))).andExpect(status().isOk()).andReturn());if(path.equals("/api/usuarios"))repo.update("UPDATE usuarios SET senha_temporaria=false WHERE id=?",result.get("id").asLong());return result;}
 Map<String,Object> user(String name,String role,List<Long> locations){return Map.of("nome",name,"email",name+"@locations.test","perfil",role,"filialIds",locations,"ativo",true);}
 @Test void technicianWithTwoLocationsCannotAccessAThirdAndChangesRevokeSessions()throws Exception{
  var master=session(repo.account("master@locations.test").id());
  assertEquals(0,body(mvc.perform(get("/api/filiais").session(master)).andReturn()).size());
  var locations=new ArrayList<Long>();for(String city:List.of("Londrina","Curitiba","Maringá"))locations.add(create(master,"/api/filiais",Map.of("nome","PUC "+city,"cidade",city,"estado","PR","ativo",true)).get("id").asLong());
  long technicianId=create(master,"/api/usuarios",user("samuel","SUPORTE",locations.subList(0,2))).get("id").asLong();var technician=session(technicianId);
  long adminId=create(master,"/api/usuarios",user("admin-local","ADMIN",List.of(locations.get(0)))).get("id").asLong();var admin=session(adminId);
  var tickets=new ArrayList<Long>();for(long branch:locations)tickets.add(create(master,"/api/chamados",Map.of("titulo","Falha","descricao","Sem conexão","filialId",branch)).get("id").asLong());
  assertEquals(2,body(mvc.perform(get("/api/chamados").session(technician)).andExpect(status().isOk()).andReturn()).size());
  assertEquals(2,body(mvc.perform(get("/api/filiais").session(technician)).andReturn()).size());
  assertEquals(2,body(mvc.perform(get("/api/notificacoes").session(technician)).andReturn()).size());
  assertEquals(2,body(mvc.perform(get("/api/dashboard").session(technician)).andReturn()).get("total").asInt());
  assertEquals(0,body(mvc.perform(get("/api/relatorios").session(technician).param("filial_id",locations.get(2).toString())).andReturn()).get("total").asInt());
  for(long ticket:tickets.subList(0,2))mvc.perform(get("/api/chamados/"+ticket).session(technician)).andExpect(status().isOk());
  long foreign=tickets.get(2);
  for(String suffix:List.of("","/mensagens","/historico","/anexos","/anexos/unknown"))mvc.perform(get("/api/chamados/"+foreign+suffix).session(technician)).andExpect(status().isNotFound());
  mvc.perform(post("/api/chamados/"+foreign+"/acoes").session(technician).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/chamados/"+foreign+"/mensagens").session(technician).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\"Teste\"}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/chamados").session(technician).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("titulo","Forjado","descricao","Teste","filialId",locations.get(2))))).andExpect(status().isForbidden());
  mvc.perform(post("/api/usuarios").session(master).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(user("sem-local","SUPORTE",List.of())))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(user("fora-do-escopo","SUPORTE",List.of(locations.get(1)))))).andExpect(status().isForbidden());
  // A local administrator may not edit a technician whose other assignments exceed their scope.
  mvc.perform(put("/api/usuarios/"+technicianId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(user("samuel","SUPORTE",List.of(locations.get(0)))))).andExpect(status().isForbidden());
  for(var actor:List.of(admin,technician)){
   mvc.perform(post("/api/filiais").session(actor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Outra\",\"cidade\":\"Outra\",\"estado\":\"PR\",\"ativo\":true}")).andExpect(status().isForbidden());
   mvc.perform(put("/api/filiais/"+locations.get(0)).session(actor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Alterada\",\"cidade\":\"Outra\",\"estado\":\"PR\",\"ativo\":true}")).andExpect(status().isForbidden());
  }
  mvc.perform(put("/api/usuarios/"+technicianId).session(master).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(user("samuel","SUPORTE",List.of(locations.get(0)))))).andExpect(status().isOk());
  mvc.perform(get("/api/chamados").session(technician)).andExpect(status().isUnauthorized());var refreshed=session(technicianId);
  assertEquals(1,body(mvc.perform(get("/api/chamados").session(refreshed)).andReturn()).size());
  assertEquals(1,body(mvc.perform(get("/api/notificacoes").session(refreshed)).andReturn()).size());
  mvc.perform(get("/api/chamados/"+tickets.get(1)).session(refreshed)).andExpect(status().isNotFound());
 }
}
