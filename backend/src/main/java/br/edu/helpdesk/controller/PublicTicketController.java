package br.edu.helpdesk.controller;
import br.edu.helpdesk.service.PublicTicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/public")
public class PublicTicketController {
 private final PublicTicketService service;
 public PublicTicketController(PublicTicketService service){this.service=service;}
 public record Submission(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String qrId,@NotBlank @Size(max=160) String nome,@NotBlank @Size(max=2000) String descricao,@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId){}
 @GetMapping("/qrcodes/{id}") Object location(@PathVariable String id){return service.location(id);}
 @PostMapping("/chamados") Object create(@Valid @RequestBody Submission data,HttpServletRequest request){
  var session=request.getSession();
  synchronized(session){
   long now=System.currentTimeMillis();Long start=(Long)session.getAttribute("publicTicketWindow");
   if(start==null||now-start>600000){session.setAttribute("publicTicketWindow",now);session.setAttribute("publicTicketCount",0);}
   int count=(Integer)session.getAttribute("publicTicketCount");
   if(count>=10)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde alguns minutos antes de enviar outro chamado.");
   session.setAttribute("publicTicketCount",count+1);
  }
  return service.create(data.qrId(),data.nome(),data.descricao(),data.requestId());
 }
}
