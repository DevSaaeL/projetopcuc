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
 private final DeskRepository r;private final PasswordEncoder encoder;private final String dummyHash;
 private final Map<String,Attempt> attempts=new HashMap<>();
 private record Attempt(int count,long start){}
 public AuthService(DeskRepository r,PasswordEncoder encoder){this.r=r;this.encoder=encoder;dummyHash=encoder.encode(UUID.randomUUID().toString());}
 public synchronized Map<String,Object> register(Register data){String email=data.email().trim().toLowerCase(Locale.ROOT);if(r.account(email)!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Já existe uma conta com este e-mail.");checkPassword(data.senha());String name=displayName(email);long id=r.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,filial_id,ativo) VALUES (?,?,?,?,NULL,true)",name,email,encoder.encode(data.senha()),"USUARIO");return Map.of("id",id,"email",email,"perfil","USUARIO");}
 private void checkPassword(String password){if(password==null||password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha deve ter ao menos 12 caracteres e no máximo 72 bytes.");}
 private String displayName(String email){String local=email.substring(0,email.indexOf('@')).replaceAll("[._+\\-]+"," ").trim();if(local.isBlank())return "Usuário";local=local.substring(0,1).toUpperCase(Locale.ROOT)+local.substring(1);return local.length()>160?local.substring(0,160):local;}
 public synchronized void login(Login data,HttpServletRequest req){String email=data.username().trim().toLowerCase(Locale.ROOT);String key=req.getRemoteAddr()+":"+email;long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->now-e.getValue().start()>900000);var attempt=attempts.getOrDefault(key,new Attempt(0,now));if(attempt.count()>=10)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Muitas tentativas. Aguarde 15 minutos.");
  var user=r.account(email);boolean valid=encoder.matches(data.password(),user==null?dummyHash:user.senhaHash());if(user==null||!valid||!user.ativo()){if(attempts.size()<10000)attempts.put(key,new Attempt(attempt.count()+1,attempt.start()));throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuário ou senha inválidos.");}
  attempts.remove(key);req.getSession();req.changeSessionId();var session=req.getSession();session.setAttribute("uid",user.id());session.setAttribute("version",user.versaoSessao());
 }
}
