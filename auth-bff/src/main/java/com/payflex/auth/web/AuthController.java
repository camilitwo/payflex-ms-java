package com.payflex.auth.web;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JOSEObjectType;
import com.payflex.auth.pb.PocketBaseClient;
import com.payflex.auth.pb.PocketBaseProperties;
import com.payflex.auth.security.JwkProvider;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Validated
@RestController
public class AuthController {

  private final JwkProvider jwkProvider;
  private final PocketBaseClient pb;
  private final PocketBaseProperties cfg;

  public AuthController(JwkProvider jwkProvider, PocketBaseClient pb, PocketBaseProperties cfg){
    this.jwkProvider = jwkProvider;
    this.pb = pb;
    this.cfg = cfg;
  }

  @Value("${auth.issuer}") String issuer;
  @Value("${auth.audience}") String audience;
  @Value("${auth.access-token-ttl-minutes}") long accessTtlMin;
  @Value("${auth.refresh-token-ttl-days}") long refreshTtlDays;
  @Value("${auth.cookie.name}") String cookieName;

  record LoginReq(@NotBlank String email, @NotBlank String password) {}
  record TokenRes(String accessToken, long expiresInSeconds) {}

  @GetMapping("/.well-known/jwks.json")
  public Map<String,Object> jwks(){ return jwkProvider.jwksJson(); }

  @PostMapping("/auth/login")
  public Mono<ResponseEntity<TokenRes>> login(@RequestBody LoginReq req){
    return pb.authWithPassword(req.email(), req.password())
        .map(res -> {
          // PocketBase response has "record" object
          @SuppressWarnings("unchecked")
          Map<String,Object> record = (Map<String, Object>) res.get("record");
          String userId = (String) record.get("id");
          // merchantId & roles mapping
          String merchantId = (String) record.getOrDefault(cfg.getMerchantField(), null);
          // Si no tiene merchantId, generar uno basado en el userId
          if (merchantId == null || merchantId.isBlank()) {
            merchantId = "mrc_" + userId.substring(0, Math.min(8, userId.length()));
          }
          Object rolesObj = record.get(cfg.getRolesField());
          List<String> roles = rolesObj instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of("MERCHANT_ADMIN");
          List<String> scopes = List.of("payments:read","payments:write");

          var now = Instant.now();
          var accessExp = now.plusSeconds(accessTtlMin * 60);

          var accessJwt = signJwt(userId, merchantId, roles, scopes, now, accessExp);
          var refreshOpaque = UUID.randomUUID().toString(); // TODO: persist later (Redis)

          ResponseCookie refreshCookie = ResponseCookie.from(cookieName, refreshOpaque)
              .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(refreshTtlDays*86400).build();

          return ResponseEntity.ok()
              .header("Set-Cookie", refreshCookie.toString())
              .body(new TokenRes(accessJwt, accessExp.getEpochSecond() - now.getEpochSecond()));
        });
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<TokenRes> refresh(@CookieValue(name = "session", required = false) String refresh){
    if (refresh == null || refresh.isBlank()) return ResponseEntity.status(401).build();
    // Without Redis, we just issue a new access for demo purposes (do not use in prod).
    var now = Instant.now();
    var accessExp = now.plusSeconds(accessTtlMin * 60);
    var accessJwt = signJwt("user-unknown","mrc_unknown", List.of("MERCHANT_ADMIN"), List.of("payments:read","payments:write"), now, accessExp);
    return ResponseEntity.ok(new TokenRes(accessJwt, accessExp.getEpochSecond() - now.getEpochSecond()));
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Void> logout(){
    ResponseCookie delete = ResponseCookie.from(cookieName, "")
        .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0).build();
    return ResponseEntity.noContent().header("Set-Cookie", delete.toString()).build();
  }

  private String signJwt(String userId, String merchantId, List<String> roles, List<String> scopes, Instant iat, Instant exp){
    try{
      Map<String,Object> claims = new LinkedHashMap<>();
      claims.put("iss", issuer);
      claims.put("aud", audience);
      claims.put("sub", userId);
      claims.put("merchantId", merchantId);
      claims.put("roles", roles);
      claims.put("scopes", scopes);
      claims.put("jti", UUID.randomUUID().toString());
      claims.put("iat", iat.getEpochSecond());
      claims.put("nbf", iat.getEpochSecond());
      claims.put("exp", exp.getEpochSecond());

      var header = new JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256)
          .keyID(jwkProvider.getRsaKey().getKeyID())
          .type(JOSEObjectType.JWT)
          .build();

      var jws = new JWSObject(header, new Payload(claims));
      var signer = new RSASSASigner(jwkProvider.getRsaKey().toPrivateKey());
      jws.sign(signer);
      return jws.serialize();
    }catch(Exception e){ throw new IllegalStateException(e); }
  }
}
