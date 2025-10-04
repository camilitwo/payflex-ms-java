package com.payflex.orchestrator.web;

import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;

import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Order(-10)
public class IdempotencyFilter implements org.springframework.web.server.WebFilter {

  private final StringRedisTemplate redis;
  public IdempotencyFilter(StringRedisTemplate redis){ this.redis = redis; }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
      return chain.filter(exchange);
    }
    String key = exchange.getRequest().getHeaders().getFirst("Idempotency-Key");
    if (key == null || key.isBlank()) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Idempotency-Key"));
    }
    String redisKey = "idem:" + key;
    return Mono.defer(() -> {
      Boolean set = redis.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
      if (Boolean.FALSE.equals(set)) {
        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Duplicated request"));
      }
      return chain.filter(exchange);
    });
  }
}
