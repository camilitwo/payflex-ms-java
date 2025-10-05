package com.payflex.auth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MerchantServiceClient {

  private static final Logger log = LoggerFactory.getLogger(MerchantServiceClient.class);
  private final WebClient webClient;

  public MerchantServiceClient(@Value("${merchant.service.url}") String merchantServiceUrl) {
    this.webClient = WebClient.builder()
        .baseUrl(merchantServiceUrl)
        .build();
  }

  public Mono<Map<String, Object>> createMerchant(Map<String, Object> request) {
    log.info("Calling merchant-service to create merchant payloadKeys={}", request.keySet());
    return webClient.post()
        .uri("/merchants")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchangeToMono(response -> {
          if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnNext(body -> log.info("merchant-service 2xx merchantId={} keys={}", body.get("merchantId"), body.keySet()));
          }
          return response.bodyToMono(String.class)
              .defaultIfEmpty("<empty-body>")
              .flatMap(body -> {
                log.error("merchant-service error status={} body={}", response.statusCode(), body);
                return Mono.error(new IllegalStateException("merchant-service status=" + response.statusCode() + " body=" + body));
              });
        })
        .timeout(java.time.Duration.ofSeconds(10))
        .doOnError(err -> log.error("merchant-service invocation failed: {}", err.getMessage(), err))
        .onErrorResume(err -> {
          // devolvemos Mono.empty para no cortar el flujo de auth, pero ya quedó logueado
            return Mono.empty();
        });
  }
}
