# Web BFF — Chatbot RAG Oficina del Agua

Aplicación desarrollada con Next.js que funciona como Backend-for-Frontend (BFF) para la Feature 2 del proyecto Oficina del Agua.

Este componente recibe las solicitudes del usuario desde la interfaz web y, posteriormente, será el encargado de comunicarse con el servicio RAG desarrollado en Spring Boot.

## Historia AQ-93

Esta implementación corresponde a la historia:

**AQ-93 — Tutorial Next.js capítulos 1-4 y ruta BFF mock de chat**

Criterio principal:

> Proyecto Next.js con una ruta de chat que responde datos simulados según el contrato OpenAPI.

## Funcionalidades implementadas

Actualmente el BFF incluye:

- Proyecto Next.js con TypeScript.
- App Router de Next.js.
- Endpoint `POST /api/chat`.
- Respuesta simulada del chatbot.
- Generación automática de `conversacionId` mediante UUID.
- Reutilización del `conversacionId` en mensajes posteriores.
- Campo `fuentes` compatible con el contrato OpenAPI.
- Validación de mensajes vacíos.
- Validación de longitud máxima de 1000 caracteres.
- Validación del formato UUID de `conversacionId`.
- Manejo de cuerpos JSON inválidos.
- Respuestas HTTP `400` para solicitudes incorrectas.

## Tecnologías

- Next.js
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