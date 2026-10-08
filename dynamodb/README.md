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
```

Dentro de la red interna de Docker, los servicios pueden acceder a DynamoDB mediante:

```text
http://dynamodb-chat:8000
```

---

## Tabla

Nombre:

```text
chatbot-historial
```

### Clave de partición

```text
conversacionId
```

Tipo:

```text
String
```

`conversacionId` identifica una conversación completa del chatbot.

### Clave de ordenamiento

```text
orden
```

Tipo:

```text
String
```

Formato:

```text
creadoEn#intercambioId
```

Ejemplo:

```text
2026-10-08T22:10:00.000Z#1e44c07f-3f14-454a-8f71-38ddcedb8c81
```

La fecha en formato ISO 8601 permite conservar el orden cronológico de los intercambios, mientras que `intercambioId` garantiza que la clave de ordenamiento sea única.

---

## Modelo de un intercambio

Cada ítem representa una pregunta del usuario y su respectiva respuesta generada por el servicio RAG.

Ejemplo:

```json
{
  "conversacionId": "7b2b8f5a-08a5-4c0e-9f63-2d7c92e72095",
  "orden": "2026-10-08T22:10:00.000Z#1e44c07f-3f14-454a-8f71-38ddcedb8c81",
  "creadoEn": "2026-10-08T22:10:00.000Z",
  "intercambioId": "1e44c07f-3f14-454a-8f71-38ddcedb8c81",
  "pregunta": "¿Cómo se calcula el consumo de agua?",
  "respuesta": "El consumo se determina a partir de la diferencia entre la lectura actual y la lectura anterior del contador.",
  "fuentes": [
    {
      "titulo": "Cálculo de consumo",
      "fragmento": "El consumo corresponde a la diferencia entre las lecturas registradas del contador."
    }
  ],
  "modelo": "qwen3:8b"
}
```

---

## Descripción de atributos

### `conversacionId`

UUID que identifica una conversación completa.

### `orden`

Clave de ordenamiento formada por:

```text
creadoEn#intercambioId
```

Permite almacenar y recuperar los intercambios de una conversación en orden cronológico.

### `creadoEn`

Fecha y hora en que ocurrió el intercambio.

Se utiliza formato ISO 8601.

Ejemplo:

```text
2026-10-08T22:10:00.000Z
```

### `intercambioId`

UUID único que identifica cada intercambio pregunta-respuesta.

### `pregunta`

Mensaje enviado por el usuario al chatbot.

### `respuesta`

Respuesta generada por el servicio RAG.

### `fuentes`

Lista de fuentes utilizadas para construir la respuesta.

Cada fuente puede contener:

```text
titulo
fragmento
```

### `modelo`

Modelo utilizado para generar la respuesta.

Ejemplo:

```text
qwen3:8b
```

---

## Responsabilidad de persistencia

La escritura del historial corresponde únicamente a `rag-service`.

El flujo esperado es:

```text
Usuario
   |
   v
Widget
   |
   v
Next.js BFF
   |
   v
rag-service
   |
   +----> PostgreSQL + pgvector
   |
   +----> LLM / Ollama
   |
   +----> DynamoDB Chatbot
```

Después de obtener la respuesta del modelo, `rag-service` debe almacenar el intercambio en la tabla:

```text
chatbot-historial
```

El BFF de Next.js no escribe directamente en DynamoDB.

Su responsabilidad continúa siendo:

```text
validar petición
        |
        v
reenviar al servicio RAG
        |
        v
recibir respuesta
        |
        v
devolver respuesta al cliente
```

---

## Manejo de fallos

Un fallo al guardar el historial en DynamoDB no debe impedir que el usuario reciba la respuesta del chatbot.

Flujo esperado:

```text
1. rag-service recibe la pregunta.
2. El servicio genera la respuesta.
3. Se intenta guardar el intercambio en DynamoDB.
4. Si DynamoDB responde correctamente, el historial queda almacenado.
5. Si DynamoDB falla, el error se registra en logs.
6. La respuesta del chatbot se devuelve normalmente al usuario.
```

Por lo tanto, la persistencia del historial se considera una operación secundaria respecto a la respuesta principal del chatbot.

---

## Configuración para `rag-service`

Para evitar valores fijos en el código, la integración deberá utilizar variables de entorno.

Ejemplo para ejecución local:

```env
DYNAMODB_ENDPOINT=http://localhost:8010
DYNAMODB_TABLE_NAME=chatbot-historial
AWS_REGION=us-east-1
```

Si `rag-service` se ejecuta dentro de la misma red de Docker Compose, el endpoint podrá configurarse como:

```env
DYNAMODB_ENDPOINT=http://dynamodb-chat:8000
DYNAMODB_TABLE_NAME=chatbot-historial
AWS_REGION=us-east-1
```

Para DynamoDB Local pueden utilizarse credenciales de desarrollo, por ejemplo:

```env
AWS_ACCESS_KEY_ID=local
AWS_SECRET_ACCESS_KEY=local
```

Estas credenciales son únicamente para el entorno local.

---

## Docker

DynamoDB Local se configura en `docker-compose.yml`.

Puerto acordado:

```text
Host: 8010
Contenedor: 8000
```

El puerto `8010` se utiliza para evitar conflictos con el monolito Laravel, que utiliza el puerto `8000`.

La información de DynamoDB Local se mantiene mediante el volumen:

```text
dynamodb-chat-data
```

La tabla se crea automáticamente mediante el servicio:

```text
dynamodb-chat-init
```

El inicializador:

```text
1. Espera a que DynamoDB Local esté disponible.
2. Verifica si la tabla chatbot-historial existe.
3. Si la tabla no existe, la crea.
4. Si ya existe, no intenta crearla nuevamente.
```

---

## Archivos de ejemplo

### `ejemplo-intercambio.json`

Contiene un ejemplo legible del modelo de un intercambio del chatbot.

### `item-ejemplo-aws.json`

Contiene el mismo intercambio utilizando el formato de atributos requerido por la operación de bajo nivel `PutItem` de DynamoDB.

Ejemplo de tipos utilizados:

```text
S = String
L = List
M = Map
```

### `query-conversacion.json`

Contiene los valores necesarios para consultar mediante `Query` todos los intercambios asociados a un `conversacionId`.

---

## Verificación de la tabla

La tabla fue creada correctamente en DynamoDB Local.

Configuración verificada:

```text
Tabla: chatbot-historial

Partition Key:
conversacionId

Sort Key:
orden

Estado:
ACTIVE

Billing Mode:
PAY_PER_REQUEST
```

---

## Prueba de inserción

Se insertó correctamente un intercambio de ejemplo mediante `PutItem`.

El documento almacenado contiene:

```text
conversacionId
orden
creadoEn
intercambioId
pregunta
respuesta
fuentes
modelo
```

Posteriormente se verificó el contenido de la tabla.

Resultado:

```text
Count: 1
ScannedCount: 1
```

---

## Prueba de consulta por conversación

También se realizó una consulta utilizando la clave de partición:

```text
conversacionId
```

La operación `Query` recuperó correctamente el intercambio correspondiente.

Resultado:

```text
Count: 1
ScannedCount: 1
```

Esto demuestra que el modelo permite recuperar los intercambios asociados a una conversación utilizando `conversacionId`.

Gracias a la clave de ordenamiento `orden`, los intercambios de una misma conversación pueden mantenerse organizados cronológicamente.

---

## Responsabilidades acordadas

### Manuel — AQ-95

Responsable de:

- Configurar DynamoDB Local.
- Utilizar el puerto `8010`.
- Crear la tabla `chatbot-historial`.
- Definir la Partition Key.
- Definir la Sort Key.
- Diseñar el modelo del documento.
- Crear un documento de ejemplo.
- Configurar la creación automática de la tabla.
- Documentar la configuración.
- Verificar la inserción de un intercambio.
- Verificar la consulta por `conversacionId`.

### Marvin — integración desde `rag-service`

Responsable de implementar:

- Cliente de DynamoDB desde Spring Boot.
- Escritura de cada intercambio.
- Persistencia de la pregunta.
- Persistencia de la respuesta.
- Persistencia de las fuentes.
- Registro del modelo utilizado.
- Generación de `intercambioId`.
- Generación de `creadoEn`.
- Construcción de la clave `orden`.
- Manejo de errores de persistencia.
- Registro del error en logs sin interrumpir la respuesta del chatbot.

---

## BFF de Next.js

El BFF no escribe directamente en DynamoDB.

Su responsabilidad permanece separada de la persistencia del historial.

Esto evita:

```text
duplicación de mensajes
formatos diferentes entre servicios
acoplamiento innecesario
credenciales de DynamoDB dentro del BFF
```

La persistencia queda centralizada en `rag-service`.

---

## Estado de AQ-95

La configuración base del historial se encuentra implementada y verificada localmente.

Se confirmó:

```text
DynamoDB Local funcionando en puerto 8010
Tabla chatbot-historial creada
Partition Key configurada
Sort Key configurada
Modelo del documento definido
Documento de ejemplo creado
Inserción de prueba exitosa
Consulta por conversacionId exitosa
Persistencia local mediante volumen Docker
```

La escritura automática de cada intercambio será integrada posteriormente desde `rag-service`.