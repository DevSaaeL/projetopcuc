package br.edu.helpdesk.security;
import br.edu.helpdesk.repository.DeskRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.IOException;
import java.util.List;
public class SessionFilter extends OncePerRequestFilter {
 private final DeskRepository repo;
 public SessionFilter(DeskRepository repo){this.repo=repo;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  var session=req.getSession(false);
  if(session!=null && session.getAttribute("uid") instanceof Long id){
   var user=repo.account(id);
   if(user!=null && user.ativo() && Integer.valueOf(user.versaoSessao()).equals(session.getAttribute("version"))){
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.email(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.perfil()))));
   }else session.invalidate();
  }
  chain.doFilter(req,res);
 }
}
