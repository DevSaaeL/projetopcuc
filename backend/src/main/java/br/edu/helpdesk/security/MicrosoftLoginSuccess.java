package br.edu.helpdesk.security;

import br.edu.helpdesk.repository.DeskRepository;
import br.edu.helpdesk.service.AuthService;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
public class MicrosoftLoginSuccess implements AuthenticationSuccessHandler {
 private final DeskRepository repo;private final AuthService auth;private final String tenant;
 public MicrosoftLoginSuccess(DeskRepository repo,AuthService auth,@Value("${app.microsoft.tenant-id}") String tenant){this.repo=repo;this.auth=auth;this.tenant=tenant;}
 @Override public void onAuthenticationSuccess(HttpServletRequest req,HttpServletResponse res,Authentication authentication)throws IOException {
  if(!(authentication.getPrincipal() instanceof OidcUser principal)||tenant.isBlank()||!tenant.equalsIgnoreCase(principal.getClaimAsString("tid"))){deny(req,res);return;}
  String oid=principal.getClaimAsString("oid");
  try{UUID.fromString(oid);}catch(Exception e){deny(req,res);return;}
  var user=repo.microsoftAccount(oid.toLowerCase(java.util.Locale.ROOT));
  if(user==null||!user.ativo()||(!user.master()&&repo.branchIds(user.id()).isEmpty())){deny(req,res);return;}
  auth.establishSession(user,req);
  // Permissions come only from the local administrator's assignment, never email or token roles.
  res.sendRedirect("/pages/login.html?sso=success");
 }
 private void deny(HttpServletRequest req,HttpServletResponse res)throws IOException{SecurityContextHolder.clearContext();var session=req.getSession(false);if(session!=null)session.invalidate();res.sendRedirect("/pages/login.html?sso=unassigned");}
}
