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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:ticket-topics;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@topics.test","app.bootstrap-password=OnlyForTests-123456","app.public-url=https://example.org/pages/novo-chamado.html"})
@AutoConfigureMockMvc
@Transactional
class TicketTopicsTest {
 @Autowired MockMvc mvc;@Autowired DeskRepository repo;@Autowired ObjectMapper mapper;
 MockHttpSession session(long id){var s=new MockHttpSession();s.setAttribute("uid",id);s.setAttribute("version",0);return s;}
 MockHttpSession master(){return session(repo.account("master@topics.test").id());}
 long branch(String name)throws Exception{return mapper.readTree(mvc.perform(post("/api/filiais").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("nome",name,"cidade",name,"estado","PR","ativo",true)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();}
 long actor(long branch,String name,String profile){long id=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?,?, ?,?)",name,name+"@topics.test","unused",profile,branch);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branch);return id;}
 String qr(long branch){String id=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES (?,'Sala',?,'A','10')",id,branch);return id;}
 long topic(long branch,String name){return ((Number)repo.one("SELECT id FROM assuntos_chamado WHERE filial_id=? AND nome=?",branch,name).get("id")).longValue();}
 String submission(String qr,long topic,String request)throws Exception{return mapper.writeValueAsString(Map.of("qrId",qr,"nome","Maria","assuntoId",topic,"requestId",request));}
 @Test void bulkCreationIncludesEverySelectedBranchWithoutDuplicates()throws Exception{
  long a=branch("Lote A"),b=branch("Lote B"),c=branch("Lote C");
  mvc.perform(post("/api/assuntos/lote").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialIds",List.of(a,b,a),"nome","  Suporte   ao laboratório  ","ativo",true))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  var saved=repo.rows("SELECT filial_id,nome,ativo FROM assuntos_chamado WHERE nome_chave='suporte ao laboratório'");
  assertEquals(2,saved.size());assertEquals(Set.of(a,b),new HashSet<>(saved.stream().map(row->((Number)row.get("filial_id")).longValue()).toList()));
  for(var row:saved){assertEquals("Suporte ao laboratório",row.get("nome"));assertEquals(true,row.get("ativo"));}
  assertTrue(repo.rows("SELECT id FROM assuntos_chamado WHERE filial_id=? AND nome='Suporte ao laboratório'",c).isEmpty());
 }
 @Test void bulkCreationRejectsForeignBranchesDuplicatesAndEmptySelectionWithoutPartialWrites()throws Exception{
  long a=branch("Permissão A"),b=branch("Permissão B"),admin=actor(a,"bulk-admin","ADMIN");
  mvc.perform(post("/api/assuntos/lote").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialIds",List.of(a,b),"nome","Assunto proibido","ativo",true)))).andExpect(status().isForbidden());
  assertTrue(repo.rows("SELECT id FROM assuntos_chamado WHERE nome='Assunto proibido'").isEmpty());
  repo.update("INSERT INTO assuntos_chamado(filial_id,nome,nome_chave) VALUES (?,'Duplicado','duplicado')",b);
  mvc.perform(post("/api/assuntos/lote").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialIds",List.of(a,b),"nome","Duplicado","ativo",true)))).andExpect(status().isBadRequest());
  assertEquals(1,repo.rows("SELECT id FROM assuntos_chamado WHERE nome='Duplicado'").size());
  mvc.perform(post("/api/assuntos/lote").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialIds",List.of(),"nome","Sem filial","ativo",true)))).andExpect(status().isBadRequest());
  long tech=actor(a,"bulk-tech","SUPORTE");
  mvc.perform(post("/api/assuntos/lote").session(session(tech)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialIds",List.of(a),"nome","Sem permissão","ativo",true)))).andExpect(status().isForbidden());
 }
 @Test void defaultsAndBranchAdministrationAreIsolated()throws Exception{
  long a=branch("Londrina"),b=branch("Curitiba"),admin=actor(a,"admin-local","ADMIN"),tech=actor(a,"tech-local","SUPORTE");
  var publicLocation=mapper.readTree(mvc.perform(get("/api/public/qrcodes/"+qr(a))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  var names=new ArrayList<String>();publicLocation.get("assuntos").forEach(t->names.add(t.get("nome").asText()));assertEquals(9,names.size());assertEquals(new HashSet<>(TicketTopicService.DEFAULTS),new HashSet<>(names));var sorted=new ArrayList<>(names);sorted.sort(java.text.Collator.getInstance(Locale.forLanguageTag("pt-BR")));assertEquals(sorted,names);
  String custom=mapper.writeValueAsString(Map.of("filialId",a,"nome","Acesso ao laboratório","ativo",true));
  mvc.perform(post("/api/assuntos").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(custom)).andExpect(status().isOk());
  mvc.perform(post("/api/assuntos").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(custom)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/assuntos").session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialId",b,"nome","Assunto exclusivo","ativo",true)))).andExpect(status().isForbidden());
  mvc.perform(put("/api/assuntos/"+topic(b,"Projetor")).session(session(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialId",a,"nome","Forjado","ativo",true)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/assuntos").session(session(tech))).andExpect(status().isForbidden());mvc.perform(get("/api/assuntos")).andExpect(status().isUnauthorized());
  assertEquals(9,repo.rows("SELECT id FROM assuntos_chamado WHERE filial_id=?",b).size());assertEquals(10,repo.rows("SELECT id FROM assuntos_chamado WHERE filial_id=?",a).size());
 }
 @Test void publicSubmissionUsesOnlyActiveTopicFromQrUnitAndPreservesOldTicket()throws Exception{
  long a=branch("Unidade A"),b=branch("Unidade B"),topic=topic(a,"Projetor");String qr=qr(a),request=UUID.randomUUID().toString(),json=submission(qr,topic,request);
  mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(submission(qr,topic(b,"Projetor"),UUID.randomUUID().toString()))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("qrId",qr,"nome","Maria","descricao","Livre","requestId",request)))).andExpect(status().isBadRequest());
  for(int i=0;i<2;i++)mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
  var tickets=repo.rows("SELECT * FROM chamados");assertEquals(1,tickets.size());assertEquals("Projetor",tickets.getFirst().get("titulo"));assertEquals("Assunto selecionado: Projetor",tickets.getFirst().get("descricao"));assertEquals(topic,((Number)tickets.getFirst().get("assunto_id")).longValue());
  mvc.perform(put("/api/assuntos/"+topic).session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("filialId",a,"nome","Projetor da sala","ativo",false)))).andExpect(status().isOk());
  mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(submission(qr,topic,UUID.randomUUID().toString()))).andExpect(status().isBadRequest());
  assertEquals("Projetor",repo.one("SELECT titulo FROM chamados").get("titulo"));
  var location=mapper.readTree(mvc.perform(get("/api/public/qrcodes/"+qr)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());assertEquals(8,location.get("assuntos").size());
 }
 @Test void signedInCreationUsesCanonicalTopicAndQrLocation()throws Exception{
  long a=branch("A"),b=branch("B"),topic=topic(a,"Impressora");String qr=qr(a);
  var body=Map.of("qrId",qr,"assuntoId",topic,"filialId",b,"titulo","Texto adulterado","descricao","Descrição adulterada");
  mvc.perform(post("/api/chamados").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Impressora")).andExpect(jsonPath("$.filial_id").value(a));
  mvc.perform(post("/api/chamados").session(master()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("qrId",qr,"assuntoId",topic)))).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Impressora"));
 }
}
