package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.service.TicketTopicService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:blocks;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=blocks@admin.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc @Transactional
class BlockPriorityTest {
 @Autowired MockMvc mvc; @Autowired DeskRepository repo; @Autowired ObjectMapper mapper;
 long branch(){long id=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES (?,'Curitiba','PR')",UUID.randomUUID().toString());TicketTopicService.seed(repo,id);return id;}
 long user(long branch,String role){String name=UUID.randomUUID().toString();long id=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?,'unused',?,?)",name,name+"@test.invalid",role,branch);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branch);return id;}
 MockHttpSession session(long id){var s=new MockHttpSession();s.setAttribute("uid",id);s.setAttribute("version",0);return s;}
 long block(long branch,String name){return repo.insert("INSERT INTO blocos(filial_id,nome,nome_chave) VALUES (?,?,?)",branch,name,name.toLowerCase(Locale.ROOT));}
 String body(long branch,List<Long> blocks){return "{\"nome\":\"Técnico\",\"email\":\"tech@test.invalid\",\"perfil\":\"SUPORTE\",\"ativo\":true,\"filialIds\":["+branch+"],\"blocoIds\":"+blocks+"}";}
 @Test void catalogIsScopedAndOnlyAdminsCanManageIt()throws Exception{
  long a=branch(),b=branch(),admin=user(a,"ADMIN"),tech=user(a,"SUPORTE");block(a,"Central");block(b,"Central");
  mvc.perform(get("/api/blocos").session(session(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/blocos?filial_id="+b).session(session(admin))).andExpect(status().isForbidden());
  mvc.perform(get("/api/blocos").session(session(tech))).andExpect(status().isForbidden());
  mvc.perform(post("/api/blocos").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"filialId\":"+a+",\"nome\":\" CENTRAL \",\"ativo\":true}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/blocos").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"filialId\":"+b+",\"nome\":\"Outro\",\"ativo\":true}")).andExpect(status().isForbidden());
 }
 @Test void assignmentsCannotExpandBranchAccessAndOldClientsPreserveValidAssignments()throws Exception{
  long a=branch(),b=branch(),admin=user(a,"MASTER_ADMIN"),tech=user(a,"SUPORTE"),own=block(a,"A"),other=block(b,"B");
  mvc.perform(put("/api/usuarios/"+tech).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(a,List.of(other)))).andExpect(status().isBadRequest());
  mvc.perform(put("/api/usuarios/"+tech).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(a,List.of(own)))).andExpect(status().isOk()).andExpect(jsonPath("$.blocos_prioritarios[0].id").value(own));
  String legacy=body(a,List.of()).replace(",\"blocoIds\":[]","");
  mvc.perform(put("/api/usuarios/"+tech).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(legacy)).andExpect(status().isOk()).andExpect(jsonPath("$.blocos_prioritarios.length()").value(1));
  mvc.perform(put("/api/usuarios/"+tech).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(legacy.replace("["+a+"]","["+b+"]"))).andExpect(status().isOk()).andExpect(jsonPath("$.blocos_prioritarios.length()").value(0));
  assertTrue(repo.rows("SELECT * FROM usuario_blocos WHERE usuario_id=?",tech).isEmpty());
 }
 @Test void ownAreaIsPersonalizedButColleaguesCanStillCoverAndPublicQueueIsUnchanged()throws Exception{
  long a=branch(),b=branch(),ana=user(a,"SUPORTE"),bruno=user(a,"SUPORTE"),own=block(a,"A"),other=block(a,"B");
  repo.update("INSERT INTO usuario_blocos(usuario_id,bloco_id) VALUES (?,?)",ana,own);
  long topic=((Number)repo.one("SELECT id FROM assuntos_chamado WHERE filial_id=? AND nome='Impressora'",a).get("id")).longValue();
  List<Long> calls=new ArrayList<>();List<String> tokens=new ArrayList<>();
  for(long block:List.of(own,other)){
   String qr=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,bloco_id,sala) VALUES (?,'Sala',?,?,?,'101')",qr,a,block==own?"A":"B",block);
   var response=mapper.readTree(mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("qrId",qr,"nome","Solicitante","assuntoId",topic,"requestId",UUID.randomUUID().toString())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
   tokens.add(response.get("acompanhamento_token").asText());calls.add(((Number)repo.one("SELECT id FROM chamados WHERE acompanhamento_token=?",tokens.getLast()).get("id")).longValue());
  }
  mvc.perform(get("/api/chamados/"+calls.getFirst()).session(session(ana))).andExpect(jsonPath("$.minha_area").value(true));
  mvc.perform(get("/api/chamados/"+calls.getLast()).session(session(ana))).andExpect(jsonPath("$.minha_area").value(false));
  mvc.perform(get("/api/chamados/"+calls.getFirst()).session(session(bruno))).andExpect(jsonPath("$.minha_area").value(false));
  mvc.perform(get("/api/chamados").session(session(ana))).andExpect(jsonPath("$.length()").value(2));
  mvc.perform(get("/api/chamados/"+calls.getFirst()).session(session(user(b,"SUPORTE")))) .andExpect(status().isNotFound());
  mvc.perform(post("/api/chamados/"+calls.getFirst()+"/acoes").session(session(bruno)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/public/acompanhamento/"+tokens.getLast())).andExpect(jsonPath("$.posicao_fila").value(2));
  repo.update("UPDATE blocos SET ativo=false WHERE id=?",own);
  mvc.perform(get("/api/chamados/"+calls.getFirst()).session(session(ana))).andExpect(jsonPath("$.minha_area").value(false));
 }
 @Test void qrUsesCanonicalBlockAndRenamePreservesPrintedLink()throws Exception{
  long a=branch(),b=branch(),admin=user(a,"MASTER_ADMIN"),own=block(a,"Central"),other=block(b,"Fôlego");
  // The production URL is deliberately validated before QR creation; use an existing legacy QR here.
  String qr=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,bloco_id,sala) VALUES (?,'Sala',?,'Central',?,'101')",qr,a,own);
  mvc.perform(put("/api/blocos/"+own).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"filialId\":"+a+",\"nome\":\"Central Novo\",\"ativo\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/public/qrcodes/"+qr)).andExpect(status().isOk()).andExpect(jsonPath("$.bloco").value("Central Novo"));
  mvc.perform(put("/api/blocos/"+own).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"filialId\":"+b+",\"nome\":\"Central Novo\",\"ativo\":true}")).andExpect(status().isBadRequest());
  repo.update("UPDATE blocos SET ativo=false WHERE id=?",own);
  mvc.perform(post("/api/usuarios").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(a,List.of(own)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/usuarios").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(b,List.of(other)).replace("SUPORTE","ADMIN").replace("tech@test.invalid","admin2@test.invalid"))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/public/qrcodes/"+qr)).andExpect(status().isOk());
 }
}
