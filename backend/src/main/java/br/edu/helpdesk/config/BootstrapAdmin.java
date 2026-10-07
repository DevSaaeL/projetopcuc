package br.edu.helpdesk.config;
import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class BootstrapAdmin {
 @Bean ApplicationRunner bootstrap(DeskRepository repo,PasswordEncoder encoder,@Value("${app.bootstrap-email}") String email,@Value("${app.bootstrap-password}") String password){
  return args->{if(!repo.rows("SELECT id FROM usuarios").isEmpty())return;
   if(email.isBlank()||password.isBlank()){System.getLogger("helpdesk").log(System.Logger.Level.WARNING,"Banco vazio. Configure BOOTSTRAP_EMAIL e BOOTSTRAP_PASSWORD para criar o primeiro administrador.");return;}
   if(password.length()<12||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||!email.contains("@"))throw new IllegalStateException("Credenciais iniciais inválidas: e-mail e senha entre 12 caracteres e 72 bytes.");
   repo.insert("INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES (?,?,?,'MASTER_ADMIN')","Administrador",email.trim().toLowerCase(java.util.Locale.ROOT),encoder.encode(password));
  };
 }
}
