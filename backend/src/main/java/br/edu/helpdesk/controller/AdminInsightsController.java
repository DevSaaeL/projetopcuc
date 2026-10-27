package br.edu.helpdesk.controller;
import br.edu.helpdesk.service.AdminInsightsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminInsightsController {
 private final AdminInsightsService service;
 public AdminInsightsController(AdminInsightsService service){this.service=service;}
 @GetMapping("/recorrencias") Object recurrence(@RequestParam Map<String,String> filters){return service.recurrence(filters);}
 @GetMapping("/saude") Object health(){return service.health();}
}
