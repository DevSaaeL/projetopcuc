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
import org.springframework.beans.factory.ObjectProvider;
import br.edu.helpdesk.security.MicrosoftLoginSuccess;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain security(HttpSecurity http,DeskRepository repo,@Value("${app.allowed-origins}") String origins,ObjectProvider<ClientRegistrationRepository> clients,MicrosoftLoginSuccess microsoftSuccess)throws Exception{
  var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));cors.setAllowCredentials(true);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",cors);
  http.cors(c->c.configurationSource(source))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/api/auth/register","/api/auth/providers").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Autenticação necessária.\"}");}).accessDeniedHandler((req,res,x)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Acesso negado ou sessão expirada.\"}");}))
   .securityContext(c->c.securityContextRepository(new NullSecurityContextRepository()))
   .requestCache(c->c.requestCache(new NullRequestCache()))
   .addFilterBefore(new SessionFilter(repo),AnonymousAuthenticationFilter.class);
  var registrations=clients.getIfAvailable();
  if(registrations!=null)http.oauth2Login(o->o.clientRegistrationRepository(registrations).loginPage("/pages/login.html").successHandler(microsoftSuccess).failureHandler((req,res,error)->{var session=req.getSession(false);if(session!=null)session.invalidate();res.sendRedirect("/pages/login.html?sso=failed");}));
  return http.build();
 }
}

