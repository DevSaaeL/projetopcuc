package br.edu.helpdesk.config;
import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.security.SessionFilter;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain security(HttpSecurity http,DeskRepository repo,@Value("${app.allowed-origins}") String origins)throws Exception{
  var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));cors.setAllowCredentials(true);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",cors);
  return http.cors(c->c.configurationSource(source))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/api/auth/register").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Autenticação necessária.\"}");}).accessDeniedHandler((req,res,x)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Acesso negado ou sessão expirada.\"}");}))
   .addFilterBefore(new SessionFilter(repo),AnonymousAuthenticationFilter.class).build();
 }
}

