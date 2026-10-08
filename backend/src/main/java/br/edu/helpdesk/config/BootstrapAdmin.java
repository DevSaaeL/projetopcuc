package br.edu.helpdesk.config;
import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class BootstrapAdmin {
 @Bean @org.springframework.core.annotation.Order(0) ApplicationRunner bootstrap(DeskRepository repo,PasswordEncoder encoder,@Value("${app.bootstrap-email}") String email,@Value("${app.bootstrap-password}") String password,@Value("${app.bootstrap-microsoft-object-id}") String objectId,@Value("${app.microsoft.enabled}") boolean microsoftEnabled){
  return args->{if(!repo.rows("SELECT id FROM usuarios").isEmpty())return;
   if(email.isBlank()||(microsoftEnabled?objectId.isBlank():password.isBlank())){System.getLogger("helpdesk").log(System.Logger.Level.WARNING,"Banco vazio. Configure BOOTSTRAP_EMAIL e BOOTSTRAP_PASSWORD para criar o primeiro administrador.");return;}
   if(!email.contains("@")||(!microsoftEnabled&&(password.length()<12||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)))throw new IllegalStateException("Credenciais iniciais inválidas: e-mail e senha entre 12 caracteres e 72 bytes.");
   if(!objectId.isBlank())java.util.UUID.fromString(objectId);
   repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil,microsoft_object_id) VALUES (?,?,?,'MASTER_ADMIN',?)","Administrador",email.trim().toLowerCase(java.util.Locale.ROOT),encoder.encode(microsoftEnabled?java.util.UUID.randomUUID().toString():password),objectId.isBlank()?null:objectId.toLowerCase(java.util.Locale.ROOT));
  };
 }
}
