package br.edu.helpdesk.service;
import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.dto.Requests.Login;
import br.edu.helpdesk.dto.Requests.Register;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class AuthService {
 @org.springframework.beans.factory.annotation.Value("${app.microsoft.enabled}") private boolean microsoftEnabled;
 private final DeskRepository r;private final PasswordEncoder encoder;private final String dummyHash;
 private final Map<String,Attempt> attempts=new HashMap<>();
 private record Attempt(int count,long start){}
 public AuthService(DeskRepository r,PasswordEncoder encoder){this.r=r;this.encoder=encoder;dummyHash=encoder.encode(UUID.randomUUID().toString());}
 public Map<String,Object> register(Register data){throw new ResponseStatusException(HttpStatus.GONE,"O cadastro é feito pelo administrador; as credenciais pertencem à Microsoft.");}
 public void establishSession(br.edu.helpdesk.entity.Account user,HttpServletRequest req){req.getSession();req.changeSessionId();var session=req.getSession();session.setAttribute("uid",user.id());session.setAttribute("version",user.versaoSessao());}
 public synchronized void login(Login data,HttpServletRequest req){if(microsoftEnabled)throw new ResponseStatusException(HttpStatus.GONE,"Entre com a Microsoft.");String email=data.username().trim().toLowerCase(Locale.ROOT);String key=req.getRemoteAddr()+":"+email;long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->now-e.getValue().start()>900000);var attempt=attempts.getOrDefault(key,new Attempt(0,now));if(attempt.count()>=10)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Muitas tentativas. Aguarde 15 minutos.");
  var user=r.account(email);boolean valid=encoder.matches(data.password(),user==null?dummyHash:user.senhaHash());if(user==null||!valid||!user.ativo()){if(attempts.size()<10000)attempts.put(key,new Attempt(attempt.count()+1,attempt.start()));throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuário ou senha inválidos.");}
  attempts.remove(key);establishSession(user,req);
 }
}
