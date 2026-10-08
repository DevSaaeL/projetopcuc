package br.edu.helpdesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.client.registration.*;
import java.net.URI;
import java.util.UUID;

@Configuration
@ConditionalOnProperty(name="app.microsoft.enabled",havingValue="true")
public class MicrosoftConfig {
 @Bean @org.springframework.core.annotation.Order(1) org.springframework.boot.ApplicationRunner verifyMicrosoftAdmin(br.edu.helpdesk.repository.DeskRepository repo){return args->{if(repo.one("SELECT id FROM usuarios WHERE perfil='MASTER_ADMIN' AND ativo=true AND microsoft_object_id IS NOT NULL")==null)throw new IllegalStateException("Vincule um administrador mestre ao ID do objeto Microsoft antes de ativar MICROSOFT_ENABLED.");};}
 @Bean ClientRegistrationRepository microsoftRegistration(
  @Value("${app.microsoft.tenant-id}") String tenant,
  @Value("${app.microsoft.client-id}") String client,
  @Value("${app.microsoft.client-secret}") String secret,
  @Value("${app.microsoft.redirect-uri}") String redirect){
  UUID.fromString(tenant);UUID.fromString(client);
  if(secret.isBlank())throw new IllegalStateException("Configure MICROSOFT_CLIENT_SECRET.");
  URI uri=URI.create(redirect);
  if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!"/login/oauth2/code/microsoft".equals(uri.getPath()))throw new IllegalStateException("Configure uma MICROSOFT_REDIRECT_URI HTTPS válida.");
  String authority="https://login.microsoftonline.com/"+tenant;
  return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("microsoft")
   .clientId(client).clientSecret(secret).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
   .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(redirect)
   .scope("openid","profile","email").authorizationUri(authority+"/oauth2/v2.0/authorize")
   .tokenUri(authority+"/oauth2/v2.0/token").jwkSetUri(authority+"/discovery/v2.0/keys")
   .issuerUri(authority+"/v2.0").userNameAttributeName("sub").clientName("Microsoft").build());
 }
}
