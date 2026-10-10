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
   }else {session.invalidate();session=null;}
  }
  if(session!=null && session.getAttribute("uid") instanceof Long id){var user=repo.account(id);if(user!=null&&user.senhaTemporaria()&&req.getRequestURI().startsWith("/api/")&&!((req.getMethod().equals("GET")&&(req.getRequestURI().equals("/api/auth/me")||req.getRequestURI().equals("/api/auth/csrf")))||(req.getMethod().equals("PUT")&&req.getRequestURI().equals("/api/auth/primeira-senha"))||(req.getMethod().equals("POST")&&(req.getRequestURI().equals("/api/auth/logout")||req.getRequestURI().equals("/api/auth/csrf"))))){res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Defina uma nova senha para continuar usando o sistema.\"}");return;}}
  chain.doFilter(req,res);
 }
}
