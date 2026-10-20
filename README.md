# oficina-agua-rag

## DynamoDB - historial del chatbot

El rag-service utiliza estas variables de entorno:

- `DYNAMODB_ENDPOINT`: endpoint de DynamoDB Local. En desarrollo usar `http://localhost:8010`. En AWS se deja sin definir.
- `DYNAMODB_TABLE_NAME`: nombre de la tabla. Por defecto `chatbot-historial`.
- `AWS_REGION`: región de AWS. Por defecto `us-east-1`.

Para desarrollo local en PowerShell:

```powershell
$env:DYNAMODB_ENDPOINT="http://localhost:8010"
$env:DYNAMODB_TABLE_NAME="chatbot-historial"
$env:AWS_REGION="us-east-1"
```

Cuando `DYNAMODB_ENDPOINT` está configurado, el cliente usa credenciales locales `local/local`.
Cuando no existe endpoint configurado, utiliza `DefaultCredentialsProvider` para AWS.

## Réplica del monolito - datos públicos del chatbot (AQ-126)

El rag-service lee tarifas, servicios, formas de pago, avisos, preguntas frecuentes y datos de contacto
desde la réplica de solo lectura de `oficina-agua-infra` y los actualiza cada 2 minutos.

- `RAG_REPLICA_PASSWORD`: contraseña del usuario de solo lectura (`DB_LECTURA_PASSWORD` del `.env` de infra). No se sube a Git.
- `RAG_REPLICA_USUARIO`: usuario de solo lectura. Por defecto `lectura`.
- `RAG_REPLICA_URL`: por defecto `jdbc:mariadb://localhost:3307/oficina_agua?connectTimeout=3000`.
- `RAG_REPLICA_INTERVALO`: cada cuánto se relee la réplica. Por defecto `PT2M`.
- `RAG_REPLICA_HABILITADA`: `false` para no leer la réplica.

Detalles en `rag-service/docs/replica-rag.md`.