package com.payflex.auth.security;

import com.nimbusds.jose.JOSEException;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.util.stream.Stream;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

  private final JwkProvider jwkProvider;
  @Autowired
  public SecurityConfig(JwkProvider jwkProvider){
    this.jwkProvider = jwkProvider;
  }

  @Bean
  SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(auth -> auth
            .pathMatchers("/auth/**", "/.well-known/**", "/actuator/health").permitAll()
            .pathMatchers("/me/**").authenticated()
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return http.build();
  }

  @Bean
  public NimbusReactiveJwtDecoder jwtDecoder() throws JOSEException {
    RSAPublicKey publicKey = (RSAPublicKey) jwkProvider.getRsaKey().toPublicKey();
    return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
    gac.setAuthorityPrefix("ROLE_");
    gac.setAuthoritiesClaimName("roles");

    var delegate = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
    delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
      var roleAuths = gac.convert(jwt);
      // Agregar scopes como autoridades SCOPE_*
      var scopeClaim = jwt.getClaimAsStringList("scopes");
      if (scopeClaim != null) {
        var scopeAuths = scopeClaim.stream().map(s -> "SCOPE_" + s).map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList();
        return Stream.concat(roleAuths.stream(), scopeAuths.stream()).toList();
      }
      return roleAuths;
    });
    return new ReactiveJwtAuthenticationConverterAdapter(delegate);
  }
}
