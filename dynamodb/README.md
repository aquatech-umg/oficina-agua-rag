# AQ-95 — Historial de conversaciones del chatbot en DynamoDB

## Objetivo

Definir el modelo del historial de conversaciones del chatbot RAG y crear la tabla correspondiente en DynamoDB Local.

La persistencia del historial será realizada por `rag-service` después de generar cada respuesta. El BFF de Next.js no escribe directamente en DynamoDB.

---

## DynamoDB Local

El servicio de DynamoDB Local se ejecuta mediante Docker.

### Endpoint local

```text
http://localhost:8010