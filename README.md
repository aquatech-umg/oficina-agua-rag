Nombre: Marvin Alexander Cámbara Alonzo  
Carnet: 0905-23-17848  
Repositorio: https://github.com/aquatech-umg/oficina-agua-rag  
Modulo asignado: Chatbot con RAG

# oficina-agua-rag

Feature 2 de la Fase 2 de la Oficina del Agua: un **chatbot con RAG** (Retrieval-Augmented Generation) en la página pública, que responde preguntas de los vecinos sobre tarifas, recibos, formas de pago, mora y fugas **usando solo información oficial** de la oficina.

El monolito heredado (Laravel) **no se modifica**. Este repositorio contiene:

- `rag-service`: servicio Spring Boot que busca la información relevante en una base vectorial (PostgreSQL + pgvector) y genera la respuesta con un modelo de lenguaje local (Ollama).
- `web-bff`: aplicación Next.js que sirve la página pública (`/consulta` y el widget del chat) y hace de intermediario (BFF) entre el navegador y los servicios.

> Este README tiene dos partes: la **arquitectura de toda la solución integrada** (secciones 1 a 3) y la **documentación de mi módulo** (sección 4). Las secciones 5 a 7 cubren seguridad, estructura del repositorio y cómo se elaboró la documentación.
>
> Además del módulo principal participé como líder del equipo: organicé el backlog en Jira, armé la infraestructura compartida (`oficina-agua-infra`: base principal, réplica y el compose de equipo) y apoyé la integración de los demás módulos.

## Contenido

1. [Arquitectura de la solución integrada](#1-arquitectura-de-la-solución-integrada)
2. [Componentes](#2-componentes)
3. [Cómo se ejecuta la solución completa](#3-cómo-se-ejecuta-la-solución-completa)
4. [Módulo asignado: chatbot con RAG](#4-módulo-asignado-chatbot-con-rag)
5. [Seguridad de la información](#5-seguridad-de-la-información)
6. [Estructura del repositorio](#6-estructura-del-repositorio)
7. [Cómo se elaboró esta documentación](#7-cómo-se-elaboró-esta-documentación)

---

## 1. Arquitectura de la solución integrada

La solución de la Fase 2 está formada por el **monolito heredado** y **cuatro features nuevas**, cada una en su propio repositorio de la organización `aquatech-umg`:

| Pieza | Repositorio | Qué es |
|---|---|---|
| Monolito heredado | `MCambara707/oficina-agua` | Sistema Laravel 13 (PHP 8.3) de gestión de agua. Sigue intacto. |
| Infraestructura | `oficina-agua-infra` | MariaDB con base principal y réplica, datos de prueba y el `docker-compose.equipo.yml` que levanta toda la infraestructura. |
| Feature 1: consulta pública con captcha | `oficina-agua-consulta` | `consulta-service` (estado de cuenta público). |
| **Feature 2: chatbot con RAG** | **`oficina-agua-rag` (este repo)** | `rag-service` (RAG con Ollama y pgvector) y `web-bff` (Next.js: página pública y widget del chat). |
| Feature 3: envío de pagos por colas | `oficina-agua-pagos` | `pagos-producer`, `pagos-consumer`, RabbitMQ y DynamoDB de control. |
| Feature 4: replicación asíncrona a NoSQL | `oficina-agua-replicacion` | `sync-service`, que copia clientes y pagos de la réplica a DynamoDB. |

### Diagrama

![Arquitectura de la solución integrada de la Fase 2](docs/arquitectura.png)

Versión vectorial y editable: [`docs/arquitectura.svg`](docs/arquitectura.svg) (incluye el diagrama de draw.io; se puede abrir en app.diagrams.net para editarlo). Cada caja indica el repositorio y el responsable de cada feature.

### Idea central

**El monolito escribe en la base principal; MariaDB copia cada cambio a una réplica de solo lectura; todos los servicios nuevos leen solo de esa réplica.** Así ninguna feature toca el monolito ni su base de datos, y cada una guarda su propio estado en su propia base (DynamoDB o PostgreSQL). Todo lo que está dentro del recuadro *Docker Compose* se levanta con el compose de equipo; Ollama y los sistemas externos quedan fuera.

### Qué representa cada conexión

| Origen → destino | Etiqueta en el diagrama | Qué representa |
|---|---|---|
| Personal de la oficina → Monolito | usa | Uso normal del sistema (HTTP, puerto 8000): clientes, contadores, lecturas, recibos y pagos. |
| Monolito → `db-primary` | lee y escribe | El monolito es el único que escribe en la base principal. |
| `db-primary` → `db-replica` | replicación | Replicación de MariaDB (asíncrona). La réplica solo admite lectura. |
| Ciudadano → Página `/consulta` | navegador | El vecino abre la página pública servida por el `web-bff`. |
| Página `/consulta` → `consulta-service` | /api/consulta (BFF) | El BFF reenvía el código del contador, el token del captcha y la IP real del visitante. |
| `consulta-service` → hCaptcha | verifica el token | Verificación del token del captcha (HTTPS, `siteverify`). |
| `consulta-service` → `db-replica` | solo lectura | Lectura del estado de cuenta con el usuario `lectura`. |
| Ciudadano → Widget de chat | chatea | El vecino escribe su pregunta en el widget embebido en `/consulta`. |
| Widget de chat → `web-bff` | HTTP | El widget llama a la ruta `/api/chat` del mismo BFF (**mi módulo**). |
| `web-bff` → `rag-service` | POST /api/chat | El navegador nunca habla directo con el servicio del chatbot (**mi módulo**). |
| `rag-service` → Postgres + pgvector | embeddings | Guarda los fragmentos con su vector y busca los más parecidos por distancia de coseno (**mi módulo**). |
| `rag-service` → Ollama | Feign · /api/embed, /api/chat | Embeddings con `bge-m3` y respuesta con `qwen3:8b`, en la GPU local (**mi módulo**). |
| `rag-service` → DynamoDB `chatbot-historial` | historial | Guarda cada pregunta y respuesta por `conversacionId` (**mi módulo**). |
| `rag-service` → `db-replica` | solo lectura: tarifas y contenido público | Cada 2 minutos lee tarifas, servicios, formas de pago, avisos, preguntas frecuentes y contacto (**mi módulo**). |
| Productor de pagos → `db-replica` | pagos pendientes (solo lectura) | Detecta los pagos nuevos. |
| Productor de pagos → DynamoDB `pagos-control` | marca pagos ya enviados | Control para no enviar un pago dos veces. |
| Productor de pagos → RabbitMQ | publica | Un mensaje por destino (`pagos.banco`, `pagos.erp`). |
| RabbitMQ → Consumidor de pagos | consume | Entrega con ack manual; los fallos van a colas de espera y a `pagos.dlq`. |
| Consumidor de pagos → Simulador Banco / ERP | (sin etiqueta) | `POST` a las APIs del docente con `x-api-key` e `Idempotency-Key`. |
| Consumidor de pagos → DynamoDB `pagos-control` | estado y logs | Registro de cada intento y estado final. |
| `sync-service` → `db-replica` | solo lectura | Lectura de clientes y pagos. |
| `sync-service` → DynamoDB réplica NoSQL | diff + upsert | Copia a NoSQL solo lo que cambió. |

## 2. Componentes

| Componente | Tecnología | Puerto | Función |
|---|---|---|---|
| Monolito | Laravel 13, PHP 8.3 | 8000 | Sistema heredado. Escribe en `db-primary`. |
| `db-primary` | MariaDB 11.4 | 3306 | Base principal del monolito (esquema `oficina_agua`). |
| `db-replica` | MariaDB 11.4 | 3307 | Réplica de solo lectura; usuario `lectura`. |
| `web-bff` | Next.js 16 | 3000 | Página pública `/consulta`, widget del chat y rutas `/api/consulta` y `/api/chat`. |
| `consulta-service` | Spring Boot | 8082 | Estado de cuenta público por código de contador; valida hCaptcha y limita por IP. |
| **`rag-service`** | **Spring Boot 4.1, Java 21** | **8081** | **Chatbot RAG: busca fragmentos relevantes y genera la respuesta.** |
| **`rag-db`** | **PostgreSQL 16 + pgvector** | **5433** | **Vectores del conocimiento del chatbot (tabla `fragmentos`).** |
| **`dynamodb-chat`** | **DynamoDB Local** | **8010** | **Historial del chat (tabla `chatbot-historial`).** |
| **Ollama** | **Ollama** | **11434** | **Modelos `qwen3:8b` y `bge-m3`; corre en la máquina, fuera de Docker, para usar la GPU.** |
| `pagos-producer` | Spring Boot | 8083 | Detecta pagos nuevos y los publica; API de estado y reintento. |
| `pagos-consumer` | Spring Boot | 8084 | Entrega cada pago al banco y al ERP; reintenta y registra. |
| RabbitMQ | RabbitMQ 4 | 5672 / 15672 | Colas de pagos, colas de espera y `pagos.dlq`. |
| `dynamodb-pagos` | DynamoDB Local | 8012 | Tablas `pagos-control` y `pagos-logs`. |
| `sync-service` | Spring Boot | 8085 | Copia clientes y pagos de la réplica a DynamoDB. |
| `dynamodb-local` | DynamoDB Local | 8011 | Tablas `clientes` y `pagos` de la replicación. |
| hCaptcha | Servicio externo | – | Verifica que el visitante no es un robot. |
| Banco y ERP | Simuladores del docente (API Gateway en AWS) | – | Sistemas externos que reciben los pagos. |

**Nota sobre la nube.** Como práctica, el equipo desplegó `consulta-service` en AWS (ECR + ECS Fargate + RDS + Application Load Balancer), lo probó desde internet y luego eliminó todos los recursos para no generar cobros. La solución entregada se ejecuta localmente con Docker Compose y los servicios del equipo, por eso la nube no aparece en el diagrama.

## 3. Cómo se ejecuta la solución completa

El procedimiento implementado está en `oficina-agua-infra/docker-compose.equipo.yml`, que incluye los compose de los repositorios de infraestructura, RAG, pagos y replicación.

**Requisitos:** Docker Desktop, Git, JDK 21, Node 22, PHP 8.3 con Composer 2 y Ollama (para el chatbot).

**1. Clonar los repositorios en la misma carpeta**

```bash
git clone https://github.com/aquatech-umg/oficina-agua-infra.git
git clone https://github.com/aquatech-umg/oficina-agua-rag.git
git clone https://github.com/aquatech-umg/oficina-agua-pagos.git
git clone https://github.com/aquatech-umg/oficina-agua-replicacion.git
git clone https://github.com/aquatech-umg/oficina-agua-consulta.git
git clone https://github.com/MCambara707/oficina-agua.git
```

**2. Crear los `.env` a partir de los ejemplos** (cada repo tiene su `.env.example`; los `.env` reales están en `.gitignore` y no se suben):

```bash
cp oficina-agua-infra/.env.example oficina-agua-infra/.env
cp oficina-agua-rag/.env.example oficina-agua-rag/.env
cp oficina-agua-rag/web-bff/.env.example oficina-agua-rag/web-bff/.env.local
cp oficina-agua-pagos/.env.example oficina-agua-pagos/.env
cp oficina-agua-replicacion/.env.example oficina-agua-replicacion/.env
cp oficina-agua-consulta/.env.example oficina-agua-consulta/.env
```

Cada quien define sus propias contraseñas en esos archivos y usa la **misma clave de lectura** (`DB_LECTURA_PASSWORD` de infra) en todos los servicios que leen la réplica. Los servicios Spring Boot leen el `.env` de su repo al arrancar (`spring.config.import`), así que no hace falta definir variables en cada consola.

**3. Levantar toda la infraestructura** (bases, réplica, pgvector, DynamoDB y RabbitMQ):

```bash
cd oficina-agua-infra
docker compose -f docker-compose.equipo.yml up -d
docker compose -f docker-compose.equipo.yml ps
```

Con `--profile apps` también se levanta `consulta-service` en Docker.

**4. Cargar los datos de prueba** (una sola vez; 500 clientes, 600 contadores, recibos y unos 37 mil pagos):

```bash
docker exec -i db-primary mariadb -uroot oficina_agua < datos-prueba/datos-prueba.sql
```

(En PowerShell: `Get-Content .\datos-prueba\datos-prueba.sql -Raw | docker exec -i db-primary mariadb -uroot oficina_agua`).

**5. Modelos del chatbot** (una sola vez):

```bash
ollama pull qwen3:8b
ollama pull bge-m3
```

**6. Monolito** (el esquema ya lo cargó la infraestructura):

```bash
cd oficina-agua
composer install && cp .env.example .env && php artisan key:generate
npm install && npm run build
php artisan serve        # http://localhost:8000
```

**7. Servicios Java y Next.js** (una terminal cada uno):

| Servicio | Carpeta | Comando |
|---|---|---|
| rag-service | `oficina-agua-rag/rag-service` | `./mvnw spring-boot:run` |
| web-bff | `oficina-agua-rag/web-bff` | `npm install && npm run dev` |
| consulta-service | `oficina-agua-consulta/consulta-service` | `./mvnw spring-boot:run` (si no se levantó con `--profile apps`) |
| pagos-consumer | `oficina-agua-pagos/pagos-consumer` | `./mvnw spring-boot:run` |
| pagos-producer | `oficina-agua-pagos/pagos-producer` | `./mvnw spring-boot:run` |
| sync-service | `oficina-agua-replicacion/sync-service` | `./mvnw spring-boot:run` |

(En Windows se usa `mvnw.cmd`.) Para apagar todo: `docker compose -f docker-compose.equipo.yml down` (los datos se conservan en los volúmenes).

---

## 4. Módulo asignado: chatbot con RAG

### 4.1 Funcionalidad y objetivo

**Problema del negocio.** La Oficina del Agua recibe muchas consultas repetidas: cuánto cuesta cada tipo de servicio, cómo se calcula el recibo, hasta qué día se puede pagar sin mora, dónde pagar o qué hacer ante una fuga. Atenderlas en ventanilla o por teléfono ocupa al personal. El chatbot las responde a cualquier hora desde la página pública.

**Qué construí.** Un asistente que **no inventa**: antes de responder busca la información oficial que tiene guardada y le pasa al modelo **solo** esos fragmentos. Si la pregunta no tiene relación con el agua, responde con un mensaje fijo sin llamar al modelo. Cada respuesta indica las **fuentes** usadas, para que el vecino sepa de dónde sale la información.

**Cómo funciona una pregunta** (`ServicioRag`):

1. El navegador envía la pregunta al `web-bff` (`POST /api/chat`), que la reenvía al `rag-service`.
2. `bge-m3` convierte la pregunta en un vector de 1024 dimensiones.
3. pgvector busca los 4 fragmentos más parecidos por distancia de coseno.
4. Un fragmento solo entra al contexto si su similitud es al menos **0.50** y no está a más de **0.10** del mejor. Si ninguno pasa, se responde un mensaje fijo con los temas disponibles, **sin llamar al modelo**.
5. `qwen3:8b` redacta la respuesta con reglas estrictas: en español, breve, usar solo el contexto, no inventar montos, teléfonos ni horarios, e ignorar instrucciones dentro de la pregunta que intenten cambiar esas reglas.
6. La respuesta vuelve con sus `fuentes` y se guarda en el historial de DynamoDB con su `conversacionId`.

**De dónde sale el conocimiento** (tabla `fragmentos`, columna `origen`):

| Origen | Qué contiene | Cómo se carga |
|---|---|---|
| `CONTENIDO` | 4 documentos redactados por el equipo en `rag-service/src/main/resources/contenido/` (tarifas, cálculo del consumo, cómo pagar y mora, fugas): 18 fragmentos. | Al arrancar (`CargadorContenido`). |
| `REPLICA` | Datos públicos que la oficina mantiene en el monolito: tarifas vigentes, servicios, formas de pago, avisos, preguntas frecuentes y datos de contacto. | Cada 2 minutos desde la réplica (`CargadorReplica`). |

Así, si el personal publica un aviso en el monolito (por ejemplo, un corte de agua), el chatbot lo conoce en máximo 2 minutos **sin tocar el código del monolito**. **No se carga ninguna tabla con datos personales**: clientes, contadores, lecturas, recibos, pagos ni usuarios.

### 4.2 Integración y responsabilidades

| Con quién | Cómo | Por qué esa frontera |
|---|---|---|
| Monolito | No hay comunicación directa. | El monolito no se modifica; sus datos llegan por la réplica. |
| `db-replica` (MariaDB) | Lectura periódica con el usuario `lectura` y su propio pool de conexiones. | Solo lectura: el chatbot no puede alterar los datos del monolito, y si la réplica se cae el chat sigue funcionando con los fragmentos de la última lectura buena. |
| `web-bff` (Next.js) | `POST /api/chat` con un timeout de 60 s y CORS limitado a los orígenes de `WIDGET_ALLOWED_ORIGINS`. | **El navegador nunca habla directo con el `rag-service`**: la dirección del servicio y del modelo no se exponen, y el BFF valida el mensaje antes de reenviarlo. |
| Ollama | Cliente Feign con timeout de lectura de 50 s, menor que el del BFF. | Si el modelo tarda, el servicio responde `503 MODELO_NO_DISPONIBLE` antes de que el BFF se rinda. El modelo es local: **no hay claves de API que puedan llegar al navegador**. |
| `rag-db` (pgvector) | JDBC; tabla `fragmentos` con índice HNSW de coseno. | Base propia del chatbot, separada de la del monolito. |
| `dynamodb-chat` | AWS SDK; tabla `chatbot-historial` (clave `conversacionId` + `orden`). | El historial es del chatbot; el BFF no escribe en DynamoDB. Si guardar falla, se registra un `WARN` y la respuesta llega igual. |

### 4.3 Decisiones técnicas

**1. Ollama local en lugar de OpenAI o AWS Bedrock** ([`rag-service/docs/decision-llm-embeddings.md`](rag-service/docs/decision-llm-embeddings.md)). El docente permitía las tres opciones. OpenAI pide tarjeta y saldo; Bedrock exige configurar permisos y acceso a modelos, y genera costos. Con Ollama todo corre en la laptop del equipo (GPU RTX 4070, 8 GB): `qwen3:8b` cabe en la memoria de video y maneja bien el español, y `bge-m3` es multilingüe. Además **no existe ninguna clave de API** que proteger. A cambio, la máquina con Ollama debe estar presente en la demo. Cambiar de proveedor es un cambio de configuración (`LLM_BASE_URL`, `LLM_MODEL`), no de código.

**2. Filtro de similitud antes de llamar al modelo** ([`rag-service/docs/chat-rag.md`](rag-service/docs/chat-rag.md)). Un RAG sin filtro siempre le pasa *algo* al modelo, aunque la pregunta no tenga relación ("¿Quién ganó el mundial?"), y el modelo termina inventando. Calibré el corte con preguntas reales: las del tema dan similitudes de 0.60 a 0.70 y las ajenas de 0.26 a 0.39, por eso el corte quedó en **0.50**. El margen de 0.10 respecto al mejor fragmento evita meter fragmentos de relleno. Resultado: las preguntas fuera de tema se responden al instante y sin gastar GPU, y las del tema solo usan información oficial.

**3. Leer la réplica en lugar de pedir una API al monolito** ([`rag-service/docs/replica-rag.md`](rag-service/docs/replica-rag.md)). Agregar un endpoint al monolito estaba prohibido. Leer la réplica con un usuario de solo lectura mantiene el monolito intacto y su base protegida. Para no repetir trabajo, cada fragmento lleva un hash SHA-256: solo se generan embeddings de lo nuevo o editado, y se borran los fragmentos que ya no vienen (por ejemplo, un aviso vencido).

**Detalle técnico documentado.** Al buscar con `<=>`, PostgreSQL no aplica la conversión de `real[]` a `vector` que sí aplica en el `INSERT`. La solución fue `CAST(:vector AS vector)` en la búsqueda (`RepositorioFragmentos`). Lo pregunté en la comunidad: [es.stackoverflow.com/questions/636392](https://es.stackoverflow.com/questions/636392).

### 4.4 Instrucciones para probar el módulo

**Requisitos previos:** infraestructura levantada (sección 3, pasos 1 a 5) y en `oficina-agua-rag/.env` la variable `RAG_REPLICA_PASSWORD` con la clave de lectura.

**1. Arrancar el servicio y la página:**

```bash
cd oficina-agua-rag/rag-service && ./mvnw spring-boot:run
cd oficina-agua-rag/web-bff && npm install && npm run dev
```

Resultado esperado en el log del `rag-service`:

```
Contenido cargado en pgvector: 18 fragmentos (...)
replica - Start completed.                                  <- a los 15 s
Datos de la replica en pgvector: N fragmentos (...)
```

**2. Probar desde la página:** abrir `http://localhost:3000/consulta`, pulsar el botón azul del chat (abajo a la derecha) y preguntar:

| Pregunta | Resultado esperado |
|---|---|
| ¿Cómo se calcula la mora? | Explica el recargo fijo y/o porcentual, aplicado una vez por recibo vencido. Fuente: **Recargo por mora**. |
| Tengo agua saliendo en el patio, ¿qué hago? | Pasos ante una fuga. Fuentes de *04-fugas*. |
| ¿Cuánto cuesta la media paja? | Precio de la tarifa vigente leída de la réplica. Fuente: **Tarifa vigente: ...**. |
| ¿Quién ganó el mundial de fútbol? | Mensaje fijo con los temas disponibles, sin llamar al modelo. |

**3. Probar la API directamente** (Swagger UI en `http://localhost:8081/swagger-ui.html`):

```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"mensaje":"¿Hasta qué día puedo pagar sin mora?"}'
```

Responde `200` con `respuesta`, `conversacionId` y `fuentes`. Enviando el mismo `conversacionId` en la siguiente pregunta, la conversación continúa. Con `{"mensaje":"   "}` responde `400 MENSAJE_INVALIDO`; con Ollama apagado responde `503 MODELO_NO_DISPONIBLE`.

**4. Ver que el conocimiento se actualiza desde el monolito:** publicar un aviso en la base principal (lo mismo que hace la pantalla de avisos del monolito):

```bash
docker exec db-primary mariadb -uroot oficina_agua -e "INSERT INTO avisos_publicos (titulo, contenido, fecha_inicio, fecha_fin, orden, activo, created_at, updated_at) VALUES ('Corte de agua en zona 1', 'El jueves no habrá servicio de agua en la zona 1 de 8:00 a 12:00 por mantenimiento del tanque.', CURDATE(), CURDATE() + INTERVAL 3 DAY, 1, 1, NOW(), NOW());"
```

En máximo 2 minutos el log muestra `1 nuevos` y la pregunta *"¿Va a haber corte de agua esta semana?"* responde con el aviso.

**5. Ver los vectores y el historial:**

```bash
docker exec rag-db psql -U rag -d rag -c "SELECT origen, archivo, seccion, vector_dims(embedding) FROM fragmentos ORDER BY id;"
docker run --rm -e AWS_ACCESS_KEY_ID=local -e AWS_SECRET_ACCESS_KEY=local -e AWS_DEFAULT_REGION=us-east-1 amazon/aws-cli dynamodb scan --table-name chatbot-historial --max-items 5 --endpoint-url http://host.docker.internal:8010
```

**Pruebas automáticas:** `cd rag-service && ./mvnw test` (división en fragmentos, filtro de similitud, textos de la réplica, historial y manejo de errores). Las pruebas de integración con la réplica y DynamoDB reales se activan con variables (`REPLICA_IT=true`).

### 4.5 Variables de configuración

Se definen en `oficina-agua-rag/.env` (copiado de `.env.example`) o en la consola, que tiene prioridad.

| Variable | Valor por defecto | Uso |
|---|---|---|
| `RAG_REPLICA_PASSWORD` | *(vacía)* | Clave del usuario `lectura`. **Obligatoria para leer la réplica; no se sube a Git.** |
| `RAG_REPLICA_URL` | `jdbc:mariadb://localhost:3307/oficina_agua` | Réplica de `oficina-agua-infra`. |
| `RAG_REPLICA_INTERVALO` | `PT2M` | Cada cuánto se relee la réplica. |
| `DYNAMODB_ENDPOINT` | *(vacío)* | `http://localhost:8010` en local. Vacío = AWS real con `DefaultCredentialsProvider`. |
| `DYNAMODB_TABLE_NAME` | `chatbot-historial` | Tabla del historial. |
| `LLM_BASE_URL` | `http://localhost:11434` | Dirección de Ollama. |
| `LLM_MODEL` / `EMBEDDING_MODEL` | `qwen3:8b` / `bge-m3` | Modelos de respuesta y de embeddings. |
| `RAG_SIMILITUD_MINIMA` / `RAG_MARGEN_RELATIVO` | `0.50` / `0.10` | Filtro de contexto (sección 4.3). |

Variables del `web-bff` (`web-bff/.env.local`): `RAG_SERVICE_URL`, `RAG_SERVICE_TIMEOUT_MS`, `WIDGET_ALLOWED_ORIGINS`, `CONSULTA_SERVICE_URL` y `HCAPTCHA_SITEKEY` (la *sitekey* es pública por diseño; el *secret* del captcha vive solo en `consulta-service`).

### 4.6 Documentos del módulo

| Documento | Contenido |
|---|---|
| [`rag-service/docs/decision-llm-embeddings.md`](rag-service/docs/decision-llm-embeddings.md) | Elección de Ollama, `qwen3:8b` y `bge-m3`. |
| [`rag-service/docs/cargador-pgvector.md`](rag-service/docs/cargador-pgvector.md) | División en fragmentos, hashes y tabla `fragmentos`. |
| [`rag-service/docs/chat-rag.md`](rag-service/docs/chat-rag.md) | Flujo del chat, reglas del modelo y calibración del corte. |
| [`rag-service/docs/replica-rag.md`](rag-service/docs/replica-rag.md) | Datos públicos leídos de la réplica. |
| [`rag-service/docs/feign-ollama.md`](rag-service/docs/feign-ollama.md) | Cliente Feign hacia Ollama. |
| [`dynamodb/README.md`](dynamodb/README.md) | Modelo de la tabla del historial. |
| [`docs/decision-nextjs-unica-app.md`](docs/decision-nextjs-unica-app.md) | Por qué la consulta y el chat comparten un solo `web-bff`. |
| [`web-bff/README.md`](web-bff/README.md) | Detalle de la aplicación Next.js. |

## 5. Seguridad de la información

- **Ninguna credencial está en el repositorio.** Las claves se leen de variables de entorno o del `.env` local, que está en `.gitignore`. Los `.env.example` solo tienen nombres de variables y valores de desarrollo no secretos.
- El historial de Git se revisó buscando archivos `.env`, contraseñas, llaves de AWS (`AKIA...`) y claves de API; no hay secretos publicados.
- El chatbot **no lee datos personales**: solo tablas públicas de la réplica (sección 4.1).
- Ollama corre en local y no usa clave. Si se cambiara a un proveedor en la nube, la clave iría en una variable de entorno del `rag-service` y nunca llegaría al navegador, porque este solo habla con el `web-bff`.
- Las credenciales `local/local` de DynamoDB Local son ficticias: el emulador acepta cualquier valor.

## 6. Estructura del repositorio

```
oficina-agua-rag/
├── README.md                  Este documento
├── .env.example               Variables de ejemplo del rag-service (copiar a .env)
├── docker-compose.yml         pgvector (rag-db :5433) y DynamoDB del chat (:8010)
├── docs/                      Diagrama de arquitectura (PNG y SVG de draw.io) y decisión del web-bff
├── dynamodb/                  Modelo y ejemplos de la tabla del historial
├── rag-service/               Spring Boot: RAG (chat, fragmentos, llm, replica, historial)
│   ├── docs/                  Documentación técnica del módulo
│   └── src/main/resources/
│       ├── contenido/         Documentos oficiales que conoce el chatbot
│       ├── openapi/           Contrato chatbot-api.yaml
│       └── schema.sql         Tabla fragmentos e índice HNSW
└── web-bff/                   Next.js: /consulta, widget del chat, /api/chat y /api/consulta
```

Flujo de trabajo: cada cambio en una rama `feature/AQ-XX-...` y Pull Request a `main`, enlazado a su tarea de Jira.

## 7. Cómo se elaboró esta documentación

La arquitectura general y esta documentación se redactaron con ayuda de una herramienta de IA (Claude), pidiéndole que revisara el código fuente y los archivos de configuración de los cinco repositorios de la organización. Revisé el borrador contra el código y lo probé con la solución levantada: los puertos, rutas, tablas, variables y resultados esperados corresponden a lo que está implementado.