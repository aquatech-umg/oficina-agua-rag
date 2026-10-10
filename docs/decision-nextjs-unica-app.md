# AQ-105: Una sola aplicación Next.js para consulta pública y chatbot

- **Fecha:** 9 de octubre de 2026
- **Responsables:** Pablo Mauricio López Carrillo y Manuel
- **Origen:** actualización de la Clase 10 en Jira (AQ-86 y AQ-105)
- **Tarea principal:** AQ-77 (Base de la Fase 2)

## Contexto

En la arquitectura de la Clase 10, Next.js aparece como un solo bloque BFF con flechas hacia consulta-service y hacia el servicio del chatbot. Había que decidir si la consulta pública y el chatbot viven en una sola aplicación Next.js o en dos, y en qué repositorio, antes de que cada uno creara su proyecto (AQ-86 y AQ-93).

Estado al momento de decidir:

- El repositorio oficina-agua-rag ya contiene `web-bff`, un proyecto Next.js 16.4.0 con la ruta `api/chat`.
- consulta-service (Spring Boot) vive en el repositorio oficina-agua-consulta.
- rag-service escucha en el puerto 8081 (verificado en su `application.yaml`).

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| **A. Una sola app en oficina-agua-rag (web-bff)** | Reutiliza el web-bff que ya funciona. Un solo proyecto que levantar y desplegar. Coincide con el bloque único del diagrama de la Clase 10. | El repositorio queda con trabajo de dos servicios. Riesgo de conflictos en archivos compartidos. La consulta depende de PRs en un repositorio que no es el suyo. |
| **B. Dos apps Next.js, una por repositorio** | Cada equipo trabaja de forma independiente. | Duplica configuración, puertos y despliegue. Contradice el bloque único del diagrama. |
| **C. Una sola app en oficina-agua-consulta** | Cumple literalmente el criterio original de AQ-86 ("en su repo"). | Obliga a mover o rehacer el web-bff, que ya existe y funciona. El chatbot pasaría a depender del repositorio de consulta. |

## Decisión

**Se adopta la opción A: una sola aplicación Next.js, el `web-bff`, en el repositorio oficina-agua-rag.**

Motivos:

1. El diagrama de la Clase 10 la dibuja como un único bloque BFF.
2. Ya existe y evita duplicar trabajo.
3. Es el menor cambio posible: la consulta pública se agrega como una ruta más.

## Consecuencias

**Arquitectura y puertos**

- web-bff en el puerto 3000, que llama a consulta-service y a rag-service.
- rag-service en el 8081.
- consulta-service pasa del 8081 al 8082 para no chocar con rag-service. El cambio está acordado y pendiente de aplicar.

**Quién mantiene qué**

- Rutas de consulta pública (`/consulta` y las que se agreguen): Pablo.
- Ruta `api/chat` y componentes del chat: Manuel.
- Archivos compartidos (`layout.tsx`, `page.tsx`, `package.json`, `next.config.ts`): se cambian en PR pequeños y avisando al otro.
- Todo cambio va en rama `feature/AQ-XX-nombre` y PR contra `main` de oficina-agua-rag.

**Ya realizado**

- La ruta `/consulta` (AQ-86) está en `main` de oficina-agua-rag, con formulario que valida el formato del código del contador.

## Pendientes

- Cambiar el puerto de consulta-service a 8082.
- Conectar `/consulta` con consulta-service.
- Definir dónde se verifica el token de hCaptcha (AQ-88): en web-bff o en consulta-service.
- Confirmar con Marvin que AQ-86 se da por cumplido con la app compartida, porque su criterio decía "creado en su repo".
- Revisar el diagrama actualizado de la Clase 10, que no se tuvo a la vista al escribir este documento; la decisión se apoya en la descripción de Jira.

## Fuentes

- Descripción de AQ-86 y AQ-105 en Jira (Clase 10).
- Repositorio oficina-agua-rag: `web-bff/package.json` y `rag-service/src/main/resources/application.yaml`.