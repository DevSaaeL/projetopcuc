package br.edu.helpdesk.controller;
import br.edu.helpdesk.service.*;
import br.edu.helpdesk.dto.Requests.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController
@RequestMapping("/api")
public class ApiController {
 private final DeskService service;private final AuthService auth;private final ProfilePhotoService photos;private final ActivityLogService logs;private final TicketTopicService topics;private final AttendanceArchive archive;
 public ApiController(DeskService service,AuthService auth,ProfilePhotoService photos,ActivityLogService logs,TicketTopicService topics,AttendanceArchive archive){this.service=service;this.auth=auth;this.photos=photos;this.logs=logs;this.topics=topics;this.archive=archive;}
 @GetMapping("/auth/providers") Object providers(){return Map.of("microsoftEnabled",false,"selfRegistration",false);}
 @GetMapping("/auth/csrf") Object csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/auth/login") Object login(@Valid @RequestBody Login data,HttpServletRequest req){return Map.of("ok",true,"mustChangePassword",auth.login(data,req));}
 @PostMapping("/auth/register") ResponseEntity<?> register(@Valid @RequestBody Register data){return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(data));}
 @PostMapping("/auth/logout") void logout(HttpServletRequest req){var s=req.getSession(false);if(s!=null){if(s.getAttribute("uid")!=null)service.recordActivity("LOGOUT","Saída do sistema.");s.invalidate();}}
 @PostMapping("/auth/foto") Object photo(@RequestParam MultipartFile arquivo)throws Exception{return photos.save(arquivo);}
 @GetMapping("/auth/foto") ResponseEntity<byte[]> photo(){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(photos.read());}
 @GetMapping("/auth/me") Object me(){return service.me();}
 @PutMapping("/auth/senha") void password(@Valid @RequestBody Password data,HttpServletRequest req){service.password(data);req.getSession().invalidate();}
 @PutMapping("/auth/primeira-senha") void firstPassword(@Valid @RequestBody InitialPassword data,HttpServletRequest req){service.initialPassword(data);req.getSession().setAttribute("version",service.current().versaoSessao());req.changeSessionId();}
 @PutMapping("/auth/perfil") Object profile(@Valid @RequestBody Profile data){return service.profile(data);}
 @PutMapping("/auth/preferencias") void prefs(@RequestBody Map<String,Boolean> data)throws Exception{service.preferences(data);}
 @PostMapping("/auth/encerrar-sessoes") void end(HttpServletRequest req){service.endSessions();req.getSession().invalidate();}
 @GetMapping("/auth/atividades") Object activities(){return service.activities();}
 @GetMapping("/config") Object config(){String url;try{url=service.publicUrl();}catch(Exception e){url="";}return Map.of("publicFrontendUrl",url);}
 @GetMapping("/filiais") Object branches(){return service.branches();}
 @PostMapping("/filiais") Object branch(@Valid @RequestBody Branch data){return service.saveBranch(null,data);}
 @PutMapping("/filiais/{id}") Object branch(@PathVariable long id,@Valid @RequestBody Branch data){return service.saveBranch(id,data);}
 @DeleteMapping("/filiais/{id}") void branch(@PathVariable long id){service.deleteBranch(id);}
 @GetMapping("/usuarios") Object users(){return service.users();}
 @PostMapping("/usuarios/gerar-senha") Object generatePassword(){return service.generatePassword();}
 @PostMapping("/usuarios") Object user(@Valid @RequestBody User data){return service.saveUser(null,data);}
 @PutMapping("/usuarios/{id}") Object user(@PathVariable long id,@Valid @RequestBody User data){return service.saveUser(id,data);}
 @DeleteMapping("/usuarios/{id}") void user(@PathVariable long id){service.deactivateUser(id);}
 @GetMapping("/assuntos") Object topics(@RequestParam(required=false) Long filial_id){return topics.list(filial_id);}
 @PostMapping("/assuntos") Object topic(@Valid @RequestBody Topic data){return topics.save(null,data.filialId(),data.nome(),data.ativo());}
 @PostMapping("/assuntos/lote") Object topics(@Valid @RequestBody Topics data){return topics.saveAll(data.filialIds(),data.nome(),data.ativo());}
 @PutMapping("/assuntos/{id}") Object topic(@PathVariable long id,@Valid @RequestBody Topic data){return topics.save(id,data.filialId(),data.nome(),data.ativo());}
 @GetMapping("/qrcodes") Object qrs(){return service.qrs();}
 @GetMapping("/qrcodes/{id}") Object qr(@PathVariable String id){return service.qr(id);}
 @PostMapping("/qrcodes") Object qr(@Valid @RequestBody Qr data){return service.createQr(data);}
 @DeleteMapping("/qrcodes/{id}") void deleteQr(@PathVariable String id){service.deleteQr(id);}
 @GetMapping("/chamados") Object tickets(@RequestParam(defaultValue="false") boolean meus,@RequestParam(defaultValue="ativos") String visao){return meus?archive.list(service.current()):service.visibleTickets(visao);}
 @GetMapping("/atendimentos") Object attendances(){return archive.list(service.current());}
 @GetMapping("/atendimentos/{id}") Object attendance(@PathVariable long id){return archive.record(id,service.current());}
 @GetMapping("/atendimentos/{id}/historico") Object attendanceHistory(@PathVariable long id){return archive.history(id,service.current());}
 @GetMapping("/chamados/{id}") Object ticket(@PathVariable long id){return service.ticket(id);}
 @PostMapping("/chamados") Object create(@Valid @RequestBody Ticket data){return service.createTicket(data);}
 @PostMapping("/chamados/{id}/acoes") Object action(@PathVariable long id,@Valid @RequestBody Action data){return service.action(id,data);}
 @GetMapping("/chamados/{id}/tecnicos-transferencia") Object transferCandidates(@PathVariable long id){return service.transferCandidates(id);}
 @PostMapping("/chamados/{id}/transferir") Object transfer(@PathVariable long id,@Valid @RequestBody Transfer data){return service.transfer(id,data);}
 @GetMapping("/chamados/{id}/historico") Object history(@PathVariable long id){return service.history(id);}
 @GetMapping("/notificacoes") Object notifications(){return service.notifications();}
 @PutMapping("/notificacoes/lidas") void read(){service.readNotification(null);}
 @PutMapping("/notificacoes/{id}/lida") void read(@PathVariable long id){service.readNotification(id);}
 @DeleteMapping("/notificacoes") void clear(){service.clearNotifications();}
 @GetMapping("/dashboard") Object dashboard(){return service.dashboard();}
 @GetMapping("/logs") Object logs(@RequestParam(required=false) String usuario_id,@RequestParam(required=false) String acao,@RequestParam(required=false) String inicio,@RequestParam(required=false) String fim,@RequestParam(defaultValue="1") int pagina){return logs.search(usuario_id,acao,inicio,fim,pagina);}
 @GetMapping("/relatorios") Object reports(@RequestParam Map<String,String> filters){return service.metrics(filters);}
}
