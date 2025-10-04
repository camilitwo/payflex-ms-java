package com.payflex.orchestrator.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
  @Bean
  SecurityWebFilterChain security(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(auth -> auth
            .pathMatchers("/actuator/health").permitAll()
            .pathMatchers(HttpMethod.POST, "/payments/**").hasAuthority("SCOPE_payments:write")
            .pathMatchers(HttpMethod.GET, "/payments/**").hasAuthority("SCOPE_payments:read")
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())));
    return http.build();
  }

  @Bean
  public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Collection<GrantedAuthority> authorities = new ArrayList<>();

      // Extraer scopes del JWT
      Object scopesObj = jwt.getClaim("scopes");
      if (scopesObj instanceof List<?>) {
        List<String> scopes = ((List<?>) scopesObj).stream()
            .filter(s -> s instanceof String)
            .map(String::valueOf)
            .collect(Collectors.toList());

        // Agregar scopes con prefijo SCOPE_
        scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
      }

      // Extraer roles del JWT
      Object rolesObj = jwt.getClaim("roles");
      if (rolesObj instanceof List<?>) {
        List<String> roles = ((List<?>) rolesObj).stream()
            .filter(r -> r instanceof String)
            .map(String::valueOf)
            .collect(Collectors.toList());

        // Agregar roles como autoridades
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
      }

      return authorities;
    });

    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
  }
}
