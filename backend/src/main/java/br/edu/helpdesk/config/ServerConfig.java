package br.edu.helpdesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfig {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> connector(
            @Value("${app.connector-protocol:org.apache.coyote.http11.Http11NioProtocol}") String protocol) {
        return factory -> factory.setProtocol(protocol);
    }
}
