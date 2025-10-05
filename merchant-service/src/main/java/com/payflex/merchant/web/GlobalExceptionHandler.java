package com.payflex.merchant.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    private Map<String,Object> baseBody(ServerWebExchange exchange, HttpStatus status){
        Map<String,Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("path", exchange.getRequest().getPath().value());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleValidation(MethodArgumentNotValidException ex, ServerWebExchange exchange){
        log.error("[ERR][VALIDATION] {} invalid fields={} msg={}", exchange.getRequest().getPath(), ex.getBindingResult().getErrorCount(), ex.getMessage());
        var body = baseBody(exchange, HttpStatus.BAD_REQUEST);
        body.put("validationErrors", ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField()+": "+fe.getDefaultMessage())
            .collect(Collectors.toList()));
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleIntegrity(DataIntegrityViolationException ex, ServerWebExchange exchange){
        log.error("[ERR][INTEGRITY] path={} msg={} cause={}", exchange.getRequest().getPath(), ex.getMessage(), ex.getMostSpecificCause().getMessage());
        var body = baseBody(exchange, HttpStatus.BAD_REQUEST);
        body.put("message", "Integrity constraint violation");
        body.put("detail", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleBadSql(BadSqlGrammarException ex, ServerWebExchange exchange){
        String sql = ex.getSql();
        String sqlState = "N/A";
        Throwable cause = ex.getCause();
        if (cause instanceof io.r2dbc.spi.R2dbcException re) {
            sqlState = re.getSqlState();
        }
        log.error("[ERR][SQL] path={} sqlState={} sql={} msg={}", exchange.getRequest().getPath(), sqlState, sql, ex.getMessage());
        var body = baseBody(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
        body.put("message", "SQL error");
        body.put("detail", ex.getMessage());
        body.put("sql", sql);
        body.put("sqlState", sqlState);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleIllegal(IllegalArgumentException ex, ServerWebExchange exchange){
        log.error("[ERR][ILLEGAL] path={} msg={}", exchange.getRequest().getPath(), ex.getMessage());
        var body = baseBody(exchange, HttpStatus.BAD_REQUEST);
        body.put("message", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleWebFluxBind(WebExchangeBindException ex, ServerWebExchange exchange){
        log.error("[ERR][BIND] path={} fieldErrors={} msg={}", exchange.getRequest().getPath(), ex.getFieldErrors().size(), ex.getMessage());
        var body = baseBody(exchange, HttpStatus.BAD_REQUEST);
        body.put("validationErrors", ex.getFieldErrors().stream()
            .map(fe -> fe.getField()+": "+fe.getDefaultMessage()+" (rejected="+fe.getRejectedValue()+")")
            .collect(Collectors.toList()));
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<Map<String,Object>>> handleOther(Throwable ex, ServerWebExchange exchange){
        log.error("[ERR][UNEXPECTED] path={} class={} msg={}", exchange.getRequest().getPath(), ex.getClass().getName(), ex.getMessage(), ex);
        var body = baseBody(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
        body.put("message", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body));
    }
}
