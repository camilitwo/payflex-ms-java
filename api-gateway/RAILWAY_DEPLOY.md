# Despliegue del API Gateway en Railway

## Requisitos Previos
- Cuenta en Railway (https://railway.app)
- Los servicios `auth-bff` y `payment-orchestrator` ya desplegados en Railway

## Pasos para Desplegar

### 1. Crear un Nuevo Servicio en Railway

1. Ve a tu proyecto en Railway
2. Haz clic en "New Service"
3. Selecciona "GitHub Repo" y conecta este repositorio
4. Railway detectará automáticamente el Dockerfile

### 2. Configurar el Dockerfile Personalizado

**IMPORTANTE**: Como este es un proyecto multi-módulo Maven, debes configurar Railway para usar el Dockerfile correcto:

1. Ve a "Settings" en el servicio
2. En "Build" busca la sección **"Dockerfile Path"**
3. Configura: `Dockerfile.api-gateway`
4. **NO configures "Root Directory"** (déjalo vacío o en `/`)
5. Guarda los cambios

### 3. Configurar Variables de Entorno

En el panel de Railway, ve a la pestaña "Variables" y configura las siguientes:

```bash
# Puerto (Railway lo asigna automáticamente, pero puedes dejarlo por defecto)
PORT=8080

# URL del servicio Auth BFF (URL pública de Railway)
# Ejemplo: https://auth-bff-production.up.railway.app
AUTH_BFF_URL=https://tu-auth-bff.up.railway.app

# URL del servicio Payment Orchestrator (URL interna o pública)
# Ejemplo: https://payment-orchestrator-production.up.railway.app
ORCHESTRATOR_URL=https://tu-orchestrator.up.railway.app

# Nivel de logging (opcional, por defecto DEBUG)
LOG_LEVEL=INFO
```

### 4. Desplegar

1. Railway comenzará el despliegue automáticamente
2. Espera a que termine el build (puede tomar 5-10 minutos la primera vez)
3. Una vez completado, Railway te dará una URL pública

### 5. Verificar el Despliegue

Verifica que el servicio esté funcionando:

```bash
curl https://tu-api-gateway.up.railway.app/actuator/health
```

Deberías recibir:
```json
{
  "status": "UP"
}
```

## Configuración de Dominio Personalizado (Opcional)

1. Ve a "Settings" > "Domains"
2. Haz clic en "Generate Domain" o agrega tu dominio personalizado
3. Railway generará un dominio como: `api-gateway-production.up.railway.app`

## URLs Importantes para Compartir

Después del despliegue, necesitarás compartir estas URLs con otros servicios:

- **URL Pública del Gateway**: Para que los clientes frontend se conecten
- **Health Check**: `/actuator/health`
- **Info Endpoint**: `/actuator/info`

## Notas Importantes

1. **Orden de Despliegue**: Asegúrate de desplegar en este orden:
   - auth-bff (primero)
   - payment-orchestrator
   - api-gateway (último)

2. **URLs Internas vs Públicas**: 
   - Usa URLs públicas de Railway para la comunicación entre servicios
   - Railway proporciona networking interno, pero es más simple usar URLs públicas

3. **Logs**: Accede a los logs en tiempo real desde el panel de Railway

4. **Costos**: Railway ofrece $5 de crédito gratis mensualmente

5. **Dockerfile en la raíz**: El proyecto usa `Dockerfile.api-gateway` en la raíz del repositorio para evitar problemas con rutas relativas en proyectos multi-módulo

## Troubleshooting

### El servicio no inicia
- Verifica que todas las variables de entorno estén configuradas
- Revisa los logs en Railway para ver errores específicos
- Asegúrate de que el Dockerfile Path esté configurado como `Dockerfile.api-gateway`
- Verifica que Root Directory esté vacío o en `/`

### Error "not found" al construir
- Este error ocurre cuando Railway intenta usar rutas relativas `../`
- Solución: Usa el archivo `Dockerfile.api-gateway` en la raíz del proyecto
- Configura "Dockerfile Path" en Settings

### Error de conexión a otros servicios
- Verifica que las URLs de `AUTH_BFF_URL` y `ORCHESTRATOR_URL` sean correctas
- Asegúrate de incluir `https://` en las URLs
- Verifica que los otros servicios estén corriendo

### Build falla por versión de Java
- El proyecto usa Java 21, asegúrate de que Railway esté usando la imagen correcta
- El Dockerfile ya especifica `eclipse-temurin-21`

## Monitoreo

Railway proporciona:
- Métricas de CPU y memoria
- Logs en tiempo real
- Reinicio automático en caso de fallo
- Deployments automáticos en cada push a la rama principal
