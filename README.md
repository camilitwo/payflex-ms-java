# PayFlex MVP (Java 21 + Spring Boot 3, Maven)

Módulos:
- **auth-bff**: Emite JWT RS256, expone `/.well-known/jwks.json`, maneja login/refresh/logout con cookie HttpOnly.
- **api-gateway**: Spring Cloud Gateway que valida JWT por JWKS y enruta a `payment-orchestrator`.
- **payment-orchestrator**: Resource Server, autorización por `merchantId`, filtro de idempotencia (Redis).

## Requisitos
- Java 21 (o ajusta `java.version` a 17)
- Maven 3.9+
- Docker (opcional para Redis/PocketBase)

## Levantar dependencias (opcional)
```bash
docker compose -f docker/docker-compose.yml up -d
```

## Compilar todo
```bash
mvn clean install -DskipTests
```

## Ejecutar servicios (en 3 terminales)

**1) Auth BFF**
```bash
cd auth-bff
mvn spring-boot:run
```
Puerto: 8081

**2) API Gateway**
```bash
cd api-gateway
mvn spring-boot:run
```
Puerto: 8080

**3) Payment Orchestrator**
```bash
cd payment-orchestrator
mvn spring-boot:run
```
Puerto: 8082

> **Nota macOS**: Los módulos incluyen `netty-resolver-dns-native-macos` para resolver correctamente DNS en Apple Silicon.

## Probar flujo

1) **Login**
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflex.local","password":"secret"}'
```
Guarda `accessToken` de la respuesta. El refresh queda en cookie HttpOnly.

2) **Crear Intent** (vía Gateway)
```bash
ACCESS="pega-el-accessToken"
curl -X POST http://localhost:8080/api/payments/intents \
  -H "Authorization: Bearer $ACCESS" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"merchantId":"mrc_abc123","amount":"10000","currency":"CLP"}'
```

3) **Consultar Intent**
```bash
curl -X GET "http://localhost:8080/api/payments/intents/pi_demo?merchantId=mrc_abc123" \
  -H "Authorization: Bearer $ACCESS"
```

## PocketBase Integration

1. **Configurar PocketBase** en `auth-bff/src/main/resources/application.yml`:
```yaml
pocketbase:
  url: http://localhost:8090
  collection: users
  merchantField: merchantId
  rolesField: roles
```

2. **Crear colección en PocketBase** llamada `users` con campos:
   - `email` (tipo: email, auth field)
   - `password` (tipo: password, auth field)
   - `merchantId` (tipo: text)
   - `roles` (tipo: json, array de strings, ej: `["MERCHANT_ADMIN"]`)

3. **Levantar PocketBase** localmente en puerto 8090 y crear un usuario de prueba.

4. **Login** con PocketBase:
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflex.local","password":"secret"}'
```

El BFF autenticará contra PocketBase y emitirá tu **JWT RS256** con `merchantId` y `roles` desde el registro PB.

## Seguridad (resumen)
- JWT **RS256** con **kid**, `iss`, `aud`, `sub`, `merchantId`, `roles`, `scopes`, `exp≤10m`.
- **Gateway** y **Orchestrator** validan por **JWKS** del `auth-bff` (endpoint `/.well-known/jwks.json`).
- **Idempotency-Key** obligatorio en `POST /payments/**` (almacenado en Redis por 24h).
- Autorización por `SCOPE_payments:write` / `SCOPE_payments:read` + validación de `merchantId`.

## Arquitectura

```
Cliente → API Gateway (8080) → Payment Orchestrator (8082)
            ↓                        ↓
        Auth BFF (8081)          Redis (idempotencia)
            ↓
        PocketBase (8090)
```

## Correcciones aplicadas

### POMs
- ✅ Configuración correcta de dependencyManagement con Spring Boot 3.3.3 y Spring Cloud 2023.0.3
- ✅ Agregada dependencia `netty-resolver-dns-native-macos` para macOS Apple Silicon
- ✅ Dependencias OAuth2 correctas (`spring-security-oauth2-resource-server`, `spring-security-oauth2-jose`)
- ✅ Redis con Lettuce para idempotencia

### Código
- ✅ `AuthController`: JWT con `JOSEObjectType.JWT` (no Base64URL)
- ✅ `PocketBaseClient`: Uso de `ParameterizedTypeReference<Map<String,Object>>()` para tipos genéricos
- ✅ `SecurityConfig` (payment-orchestrator): Configuración reactiva con `ServerHttpSecurity` (no `HttpSecurity`)
- ✅ `SecurityConfig` (api-gateway): Deshabilita CSRF y configura validación JWT con OAuth2 Resource Server
- ✅ `SecurityConfig` (auth-bff): Deshabilita CSRF y permite acceso público a endpoints de autenticación
- ✅ `IdempotencyFilter`: Uso de `HttpMethod` enum y `getMethod()`
- ✅ `application.yml`: Consolidación de secciones `spring:` duplicadas

