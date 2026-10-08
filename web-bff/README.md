# Web BFF — Chatbot RAG Oficina del Agua

Aplicación Next.js que funciona como Backend-for-Frontend (BFF) para la Feature 2 — Chatbot RAG del proyecto Oficina del Agua.

## AQ-93

Esta implementación corresponde a:

**AQ-93 — Tutorial Next.js capítulos 1-4 y ruta BFF mock de chat**

Criterio:

> Proyecto Next.js con una ruta de chat que responde datos simulados según el contrato OpenAPI.

## Funcionalidades implementadas

- Proyecto Next.js con TypeScript.
- App Router.
- Endpoint `POST /api/chat`.
- Respuesta simulada del chatbot.
- Generación automática de `conversacionId` mediante UUID.
- Reutilización del `conversacionId`.
- Campo `fuentes` compatible con el contrato OpenAPI.
- Validación de mensajes vacíos o con solo espacios.
- Validación de longitud máxima de 1000 caracteres.
- Validación del formato UUID de `conversacionId`.
- Manejo de cuerpos JSON inválidos.
- Respuestas HTTP `400` para solicitudes incorrectas.

## Tecnologías

- Next.js 16
- React
- TypeScript
- Node.js
- npm
- ESLint

## Requisitos

Antes de ejecutar el proyecto se debe contar con:

- Node.js instalado.
- npm instalado.

Versiones utilizadas durante el desarrollo:

```text
Node.js v22.15.0
npm 10.9.2
```

## Instalación

Desde la raíz del repositorio, ingresar al proyecto:

```bash
cd web-bff
```

Instalar las dependencias:

```bash
npm install
```

## Ejecución local

Iniciar el servidor de desarrollo:

```bash
npm run dev
```

La aplicación estará disponible en:

```text
http://localhost:3000
```

## Endpoint de chat

El endpoint implementado es:

```text
POST /api/chat
```

## Primera solicitud

En la primera petición no se debe enviar `conversacionId`.

Ejemplo:

```json
{
  "mensaje": "Como se calcula el consumo de agua?"
}
```

Ejemplo de prueba con PowerShell:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:3000/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"mensaje":"Como se calcula el consumo de agua?"}'
```

## Respuesta esperada

```json
{
  "respuesta": "Esta es una respuesta simulada del chatbot de la Oficina Municipal de Agua. La integración con el servicio RAG se realizará posteriormente.",
  "conversacionId": "f9d64c10-f556-4649-a81b-1cf965d392c7",
  "fuentes": [
    {
      "titulo": "Contenido de referencia simulado",
      "fragmento": "Respuesta temporal utilizada para probar el contrato entre Next.js y el servicio RAG."
    }
  ]
}
```

## Continuar una conversación

En los siguientes mensajes se reutiliza el `conversacionId` recibido anteriormente.

Ejemplo:

```json
{
  "mensaje": "Y que pasa si consumo mas de la capacidad?",
  "conversacionId": "f9d64c10-f556-4649-a81b-1cf965d392c7"
}
```

## Manejo de errores

### Mensaje vacío

Respuesta HTTP:

```text
400 Bad Request
```

Respuesta JSON:

```json
{
  "codigo": "MENSAJE_INVALIDO",
  "mensaje": "El mensaje no puede estar vacío."
}
```

### Mensaje con solo espacios

Si el campo `mensaje` contiene únicamente espacios, se considera inválido y devuelve:

```text
400 Bad Request
```

### Mensaje mayor de 1000 caracteres

Respuesta HTTP:

```text
400 Bad Request
```

Respuesta JSON:

```json
{
  "codigo": "MENSAJE_INVALIDO",
  "mensaje": "El mensaje no puede superar los 1000 caracteres."
}
```

### UUID inválido

Si se envía un `conversacionId` que no tiene formato UUID válido:

```json
{
  "mensaje": "Hola",
  "conversacionId": "12345"
}
```

La respuesta es:

```json
{
  "codigo": "CONVERSACION_INVALIDA",
  "mensaje": "El conversacionId debe tener un formato UUID válido."
}
```

Respuesta HTTP:

```text
400 Bad Request
```

### JSON inválido

Si el cuerpo de la petición no contiene un JSON válido, se devuelve:

```json
{
  "codigo": "PETICION_INVALIDA",
  "mensaje": "El cuerpo de la petición debe ser JSON válido."
}
```

Respuesta HTTP:

```text
400 Bad Request
```

## Verificación del proyecto

Ejecutar ESLint:

```bash
npm run lint
```

Generar el build de producción:

```bash
npm run build
```

Verificar las dependencias utilizadas en producción:

```bash
npm audit --omit=dev
```

Resultado obtenido durante las pruebas:

```text
found 0 vulnerabilities
```

Durante el desarrollo de AQ-93, `npm run lint` y `npm run build` finalizaron correctamente.

El build reconoce la ruta:

```text
ƒ /api/chat
```

como una ruta dinámica de Next.js.

## Estado actual

Actualmente `POST /api/chat` devuelve una respuesta simulada para validar el contrato entre el BFF y el servicio RAG.

El flujo actual es:

```text
Cliente
   ↓
Next.js BFF
   ↓
POST /api/chat
   ↓
Respuesta simulada
```

La integración real se realizará posteriormente con el servicio RAG desarrollado en Spring Boot.

El flujo esperado será:

```text
Cliente
   ↓
Next.js BFF
   ↓
Spring Boot RAG
   ↓
pgvector / embeddings
   ↓
LLM
   ↓
Respuesta
```

Al integrar el servicio RAG, el mensaje deberá enviarse normalizado con `trim()` para mantener las mismas reglas de validación entre ambos componentes.

## Nota sobre la conversación

El `conversacionId` debe omitirse en el primer mensaje.

Cuando el servicio devuelve un `conversacionId`, este debe reutilizarse en los mensajes posteriores para mantener la misma conversación.