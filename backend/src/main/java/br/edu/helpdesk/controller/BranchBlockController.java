package br.edu.helpdesk.controller;
import br.edu.helpdesk.service.BranchBlockService;
import br.edu.helpdesk.dto.Requests.Block;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/blocos")
public class BranchBlockController {
 private final BranchBlockService service;
 public BranchBlockController(BranchBlockService service){this.service=service;}
 @GetMapping Object list(@RequestParam(required=false) Long filial_id){return service.list(filial_id);}
 @PostMapping Object create(@Valid @RequestBody Block data){return service.save(null,data);}
 @PutMapping("/{id}") Object update(@PathVariable long id,@Valid @RequestBody Block data){return service.save(id,data);}
}
