# Merchant Service - Guía de Configuración

Este servicio gestiona la creación y administración de comercios en PostgreSQL, sincronizando los datos cuando se registra un nuevo usuario en el sistema.

## 📋 Requisitos Previos

- PostgreSQL 14+ instalado y corriendo
- Java 21
- Maven 3.8+

## 🚀 Configuración de PostgreSQL

### 1. Inicializar PostgreSQL (si no está instalado)

**En macOS con Homebrew:**
```bash
# Instalar PostgreSQL
brew install postgresql@14

# Iniciar el servicio
brew services start postgresql@14

# Verificar que está corriendo
psql --version
```

### 2. Crear la Base de Datos

```bash
# Conectar a PostgreSQL
psql -U postgres

# O si tienes un usuario diferente
psql -U tu_usuario

# Crear la base de datos
CREATE DATABASE payflex;

# Salir
\q
```

### 3. Verificar la Conexión

```bash
psql -U postgres -d payflex -c "SELECT version();"
```

## 🔧 Configuración del Servicio

El servicio está configurado en `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payflex
    username: postgres
    password: postgres
  
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/payflex
    username: postgres
    password: postgres
```

**⚠️ IMPORTANTE:** Cambia el username y password según tu configuración de PostgreSQL.

## 📦 Compilar el Proyecto

Desde la raíz del proyecto:

```bash
# Compilar todo el proyecto incluyendo merchant-service
mvn clean package -DskipTests

# Solo compilar merchant-service
cd merchant-service
mvn clean package -DskipTests
```

## ▶️ Ejecutar el Servicio

### Opción 1: Con Maven
```bash
cd merchant-service
mvn spring-boot:run
```

### Opción 2: Con JAR
```bash
java -jar merchant-service/target/merchant-service-1.0.0.jar
```

El servicio se iniciará en **http://localhost:8083**

## 🗄️ Migraciones de Base de Datos

Las migraciones se ejecutan automáticamente al iniciar el servicio usando **Flyway**.

Las tablas que se crearán son:
- `merchants` - Información de los comercios
- `merchant_users` - Usuarios asociados a cada comercio
- `merchant_payment_configs` - Configuración de pagos
- `merchant_balances` - Saldo disponible y pendiente

Para verificar que las tablas se crearon:
```bash
psql -U postgres -d payflex -c "\dt"
```

## 🔗 Integración con Auth-BFF

El `auth-bff` está configurado para llamar automáticamente al `merchant-service` cuando se registra un nuevo usuario.

Configuración en `auth-bff/src/main/resources/application.yml`:
```yaml
merchant:
  service:
    url: http://localhost:8083
```

### Flujo de Registro

1. Usuario se registra en `/auth/register`
2. Se crea el usuario en PocketBase
3. Se obtiene el `userId` y `merchantId`
4. Se llama a `merchant-service` para crear:
   - Registro en tabla `merchants`
   - Registro en tabla `merchant_users`
   - Balance inicial en `merchant_balances`

## 🧪 Probar el Servicio

### Registro de Usuario Completo

```bash
curl --location 'http://localhost:8081/auth/register' \
--header 'Content-Type: application/json' \
--data '{
    "email": "comercio@ejemplo.com",
    "password": "secret123",
    "passwordConfirm": "secret123",
    "name": "Mi Comercio"
}'
```

### Verificar en PostgreSQL

```bash
# Ver comercios creados
psql -U postgres -d payflex -c "SELECT * FROM merchants;"

# Ver usuarios creados
psql -U postgres -d payflex -c "SELECT * FROM merchant_users;"

# Ver balances
psql -U postgres -d payflex -c "SELECT * FROM merchant_balances;"
```

### API Directa del Merchant Service

```bash
# Obtener información de un comercio
curl http://localhost:8083/merchants/mrc_abc123

# Verificar si existe un comercio
curl http://localhost:8083/merchants/mrc_abc123/exists
```

## 📊 Estructura de la Base de Datos

### Tabla: merchants
```sql
- id (VARCHAR 50) PK
- business_name (VARCHAR 255)
- legal_name (VARCHAR 255)
- tax_id (VARCHAR 50) UNIQUE
- email (VARCHAR 255) UNIQUE
- phone (VARCHAR 50)
- website (VARCHAR 255)
- status (VARCHAR 20) DEFAULT 'active'
- onboarding_completed (BOOLEAN)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

### Tabla: merchant_users
```sql
- id (VARCHAR 50) PK
- merchant_id (VARCHAR 50) FK -> merchants(id)
- email (VARCHAR 255) UNIQUE
- name (VARCHAR 255)
- phone (VARCHAR 50)
- role (VARCHAR 50) DEFAULT 'MERCHANT_ADMIN'
- status (VARCHAR 20) DEFAULT 'active'
- email_verified (BOOLEAN)
- last_login_at (TIMESTAMP)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

### Tabla: merchant_balances
```sql
- id (SERIAL) PK
- merchant_id (VARCHAR 50) UNIQUE FK -> merchants(id)
- available_balance (DECIMAL 15,2)
- pending_balance (DECIMAL 15,2)
- currency (VARCHAR 3) DEFAULT 'CLP'
- updated_at (TIMESTAMP)
```

## 🐛 Troubleshooting

### Error: "could not connect to server"
```bash
# Verificar que PostgreSQL está corriendo
brew services list | grep postgresql

# Reiniciar PostgreSQL
brew services restart postgresql@14
```

### Error: "database does not exist"
```bash
# Crear la base de datos
psql -U postgres -c "CREATE DATABASE payflex;"
```

### Error: "password authentication failed"
```bash
# Editar application.yml con tus credenciales correctas
# O cambiar la contraseña de PostgreSQL:
psql -U postgres -c "ALTER USER postgres PASSWORD 'nueva_password';"
```

### Ver logs del servicio
```bash
# Los logs aparecerán en la consola
# Para nivel DEBUG, agregar en application.yml:
logging:
  level:
    com.payflex.merchant: DEBUG
    org.springframework.r2dbc: DEBUG
```

## 🔒 Seguridad en Producción

⚠️ **NO usar en producción sin cambiar:**

1. Contraseñas de base de datos
2. Usar variables de entorno para credenciales
3. Configurar SSL para PostgreSQL
4. Implementar rate limiting
5. Agregar autenticación entre microservicios

## 📚 Endpoints Disponibles

- `POST /merchants` - Crear comercio (uso interno del auth-bff)
- `GET /merchants/{merchantId}` - Obtener información de comercio
- `GET /merchants/{merchantId}/exists` - Verificar si existe comercio
- `GET /actuator/health` - Health check

## 🔄 Orden de Inicio de Servicios

1. PostgreSQL
2. PocketBase (puerto 8090)
3. Merchant Service (puerto 8083)
4. Auth BFF (puerto 8081)
5. Payment Orchestrator (puerto 8082)
6. API Gateway (puerto 8080)

