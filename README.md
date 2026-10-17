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
