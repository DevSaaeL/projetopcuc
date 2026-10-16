package br.edu.helpdesk;

import br.edu.helpdesk.repository.DeskRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.http.MediaType;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:queue-logs;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=master@queue.test","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
@Transactional
class QueueAndLogsTest {
 @Autowired DeskRepository repo; @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 long branch(String name){return repo.insert("INSERT INTO filiais(nome,cidade,estado) VALUES (?,?,'PR')",name,name);}
 long user(String name,String role,long...branches){long id=repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id) VALUES (?,?,?, ?,?)",name,name+"@queue.test","unused",role,branches[0]);for(long branch:branches)repo.update("INSERT INTO usuario_filiais(usuario_id,filial_id) VALUES (?,?)",id,branch);return id;}
 MockHttpSession session(long id){var session=new MockHttpSession();session.setAttribute("uid",id);session.setAttribute("version",repo.account(id).versaoSessao());return session;}
 long ticket(long branch,String name,int age,String status){return repo.insert("INSERT INTO chamados(titulo,descricao,solicitante_nome,filial_id,cidade,prioridade,status,sla_resposta,sla_resolucao,origem,abertura) VALUES (?,?,'Solicitante',?,'Curitiba','MEDIA',?,5,10,'QR',?)",name,"Descrição",branch,status,OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(age).withNano(0));}
 JsonNode read(String path,MockHttpSession session)throws Exception{return mapper.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void fifoUsesOpeningThenIdAndKeepsOtherUnitsOut()throws Exception{
  long a=branch("A"),b=branch("B"),c=branch("C"),tech=user("tech-fifo","SUPORTE",a,b);var session=session(tech);
  long first=ticket(a,"Primeiro",400,"ABERTO"),tie=ticket(b,"Segundo",400,"ABERTO"),newest=ticket(a,"Terceiro",1,"ABERTO"),active=ticket(b,"Em atendimento",600,"EM_ATENDIMENTO"),closed=ticket(a,"Encerrado",1000,"CANCELADO"),foreign=ticket(c,"Outra unidade",2000,"ABERTO");
  repo.update("UPDATE chamados SET abertura=(SELECT abertura FROM chamados WHERE id=?) WHERE id=?",first,tie);
  repo.update("UPDATE chamados SET prioridade='CRITICA' WHERE id=?",newest);
  var rows=read("/api/chamados",session);assertEquals(List.of(first,tie,newest,active),mapper.convertValue(rows,java.util.List.class).stream().map(row->((Number)((Map<?,?>)row).get("id")).longValue()).toList());
  assertEquals(1,rows.get(0).get("posicao_fila").asInt());assertEquals(2,rows.get(1).get("posicao_fila").asInt());assertEquals(3,rows.get(2).get("posicao_fila").asInt());assertTrue(rows.get(3).get("posicao_fila").isNull());
  assertEquals(2,read("/api/chamados/"+tie,session).get("posicao_fila").asInt());
  assertEquals(newest,read("/api/dashboard",session).get("chamados").get(0).get("id").asLong());
  mvc.perform(get("/api/chamados/"+foreign).session(session)).andExpect(status().isNotFound());
 }
 void audit(Long actor,Long ticket,String name,String action,String description){repo.insert("INSERT INTO auditoria(autor_id,chamado_id,autor_nome,acao,descricao) VALUES (?,?,?,?,?)",actor,ticket,name,action,description);}
 @Test void logsFilterByUserDatesAndActionWithoutLeakingAnotherUnit()throws Exception{
  long a=branch("Londrina"),b=branch("Curitiba"),tech=user("shared-tech","SUPORTE",a,b),other=user("foreign-tech","SUPORTE",b),admin=user("local-admin","ADMIN",a);
  long ta=ticket(a,"Unidade A",100,"ABERTO"),tb=ticket(b,"Unidade B",90,"ABERTO");
  audit(tech,ta,null,"EM_ATENDIMENTO","Atendeu A");audit(tech,tb,null,"EM_ATENDIMENTO","Atendeu B");audit(tech,null,null,"LOGIN","Entrou");audit(other,tb,null,"LOGIN","Entrada de outro usuário");audit(null,ta,"Maria QR","ABERTO","Abertura pública");
  var local=read("/api/logs",session(admin));assertEquals(2,local.get("total").asInt());assertFalse(local.toString().contains("Atendeu B"));assertFalse(local.toString().contains("foreign-tech"));assertFalse(local.toString().contains("senha_hash"));
  var filtered=read("/api/logs?usuario_id="+tech+"&acao=EM_ATENDIMENTO",session(admin));assertEquals(1,filtered.get("total").asInt());assertEquals("Atendeu A",filtered.get("registros").get(0).get("descricao").asText());
  assertEquals(1,read("/api/logs?usuario_id=publico",session(admin)).get("total").asInt());
  var own=read("/api/logs",session(tech));assertEquals(3,own.get("total").asInt());assertEquals(1,own.get("usuarios").size());
  assertEquals(0,read("/api/logs?usuario_id="+other,session(tech)).get("total").asInt());
  var master=session(repo.account("master@queue.test").id());assertEquals(5,read("/api/logs",master).get("total").asInt());
  assertEquals(0,read("/api/logs?inicio=2000-01-01&fim=2000-01-01",master).get("total").asInt());
  mvc.perform(get("/api/logs?inicio=2026-10-16&fim=2026-10-01").session(master)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/logs?usuario_id=x").session(master)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/logs?pagina=0").session(master)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/logs")).andExpect(status().isUnauthorized());
 }
 @Test void paginationDoesNotDropOrDuplicateEventsWithIdenticalDates()throws Exception{
  long a=branch("Paginação"),tech=user("page-tech","SUPORTE",a);var date=OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
  for(int i=0;i<55;i++){audit(tech,null,null,"LOGIN","Evento "+i);}
  repo.update("UPDATE auditoria SET criado_em=? WHERE autor_id=?",date,tech);
  var session=session(tech);var first=read("/api/logs",session);var second=read("/api/logs?pagina=2",session);assertEquals(55,first.get("total").asInt());assertEquals(50,first.get("registros").size());assertEquals(5,second.get("registros").size());
  var ids=new HashSet<Long>();for(var page:List.of(first,second))for(var row:page.get("registros"))assertTrue(ids.add(row.get("id").asLong()));assertEquals(55,ids.size());
 }
 @Test void loginProfileEmailChangeAndLogoutAreAuditedWithoutCredentials()throws Exception{
  long master=repo.account("master@queue.test").id();
  var session=(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"master@queue.test\",\"password\":\"OnlyForTests-123456\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();
  mvc.perform(put("/api/auth/perfil").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Novo nome\",\"email\":\"updated@queue.test\",\"telefone\":\"\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
  var records=repo.rows("SELECT acao,descricao FROM auditoria WHERE autor_id=? ORDER BY id",master);
  assertEquals(List.of("LOGIN","PERFIL","LOGOUT"),records.stream().map(r->r.get("acao")).toList());
  assertFalse(records.toString().contains("OnlyForTests-123456"));
 }
}
