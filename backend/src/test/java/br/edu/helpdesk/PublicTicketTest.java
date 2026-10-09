package br.edu.helpdesk;
import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:public-qr;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=public-admin@test.invalid","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
class PublicTicketTest {
 @Autowired MockMvc mvc; @Autowired DeskRepository repo; @Autowired ObjectMapper mapper;
 MockHttpSession session(long id){var s=new MockHttpSession();s.setAttribute("uid",id);s.setAttribute("version",0);return s;}
 @Test void anonymousQrKeepsLocationPrivateDataAndSupportScopeProtected()throws Exception {
  long unit=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Londrina','Londrina','PR')");
  long other=repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES ('Curitiba','Curitiba','PR')");
  long tech=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES ('Técnico','tech@test.invalid','unused','SUPORTE',?)",unit);
  long foreign=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES ('Outro','other@test.invalid','unused','SUPORTE',?)",other);
  repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",tech,unit);repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",foreign,other);
  String qr=UUID.randomUUID().toString();repo.update("INSERT INTO qrcodes(id,nome,filial_id,bloco,sala) VALUES (?,'Laboratório',?,'B','12')",qr,unit);
  mvc.perform(get("/api/public/qrcodes/"+qr)).andExpect(status().isOk()).andExpect(jsonPath("$.filial_nome").value("Londrina")).andExpect(jsonPath("$.filial_id").doesNotExist());
  var body=new HashMap<String,Object>(Map.of("qrId",qr,"nome","Maria Silva","descricao","Computador não liga","requestId",UUID.randomUUID().toString(),"filialId",other,"sala","999"));
  String json=mapper.writeValueAsString(body);
  mvc.perform(post("/api/public/chamados").contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
  var response=mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk()).andExpect(jsonPath("$.protocolo").isNotEmpty()).andExpect(jsonPath("$.descricao").doesNotExist()).andReturn().getResponse().getContentAsString();
  mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk()).andExpect(content().json(response));
  var tickets=repo.rows("SELECT * FROM chamados");assertEquals(1,tickets.size());var ticket=tickets.getFirst();long id=((Number)ticket.get("id")).longValue();assertNull(ticket.get("solicitante_id"));assertEquals("Maria Silva",ticket.get("solicitante_nome"));assertEquals(unit,((Number)ticket.get("filial_id")).longValue());assertEquals("12",ticket.get("sala"));assertEquals("Solicitação via QR Code",ticket.get("titulo"));assertEquals("Computador não liga",ticket.get("descricao"));
  mvc.perform(get("/api/chamados")).andExpect(status().isUnauthorized());mvc.perform(get("/api/chamados/"+id)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/chamados/"+id).session(session(foreign))).andExpect(status().isNotFound());
  mvc.perform(get("/api/chamados/"+id).session(session(tech))).andExpect(status().isOk()).andExpect(jsonPath("$.solicitante").value("Maria Silva"));
  mvc.perform(get("/api/chamados/"+id+"/historico").session(session(tech))).andExpect(jsonPath("$[0].autor").value("Maria Silva"));
  mvc.perform(get("/api/notificacoes").session(session(tech))).andExpect(jsonPath("$[0].titulo").value("Novo chamado"));
  mvc.perform(get("/api/notificacoes").session(session(foreign))).andExpect(content().json("[]"));
  mvc.perform(post("/api/chamados/"+id+"/acoes").session(session(tech)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"assumir\"}")).andExpect(status().isOk());
  body.put("nome"," ");body.put("requestId",UUID.randomUUID().toString());mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
  repo.update("UPDATE qrcodes SET ativo=false WHERE id=?",qr);
  mvc.perform(get("/api/public/qrcodes/"+qr)).andExpect(status().isNotFound());
  body.put("nome","Maria");mvc.perform(post("/api/public/chamados").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isNotFound());
 }
}
