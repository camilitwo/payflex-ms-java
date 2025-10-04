package com.payflex.orchestrator.web;

import com.payflex.orchestrator.security.MerchantAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentsController {
  private static final Logger log = LoggerFactory.getLogger(PaymentsController.class);

  private final MerchantAccess merchantAccess;
  public PaymentsController(MerchantAccess merchantAccess){ this.merchantAccess = merchantAccess; }

  @PostMapping(value="/intents", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String,Object>> createIntent(@RequestBody Mono<Map<String,Object>> bodyMono, Authentication auth){
    return bodyMono
        .doOnNext(b -> log.info("[createIntent] raw body={}", b))
        .flatMap(body -> {
          Object merchantIdObj = body.get("merchantId");
          Object amountObj = body.get("amount");
          Object currencyObj = body.getOrDefault("currency", "CLP");

          if (merchantIdObj == null || merchantIdObj.toString().isBlank()) {
            return Mono.just(Map.of(
                "error", "merchantId is required",
                "status", 400
            ));
          }
          if (amountObj == null || amountObj.toString().isBlank()) {
            return Mono.just(Map.of(
                "error", "amount is required",
                "status", 400
            ));
          }

          String merchantId = merchantIdObj.toString();
          merchantAccess.ensure(auth, merchantId);
          String currency = currencyObj == null || currencyObj.toString().isBlank() ? "CLP" : currencyObj.toString();

          return Mono.just(Map.of(
              "id", "pi_" + UUID.randomUUID(),
              "merchantId", merchantId,
              "amount", amountObj.toString(),
              "currency", currency,
              "status", "requires_payment_method"
          ));
        });
  }
}
