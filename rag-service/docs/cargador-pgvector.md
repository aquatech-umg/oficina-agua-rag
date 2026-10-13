# Cargador del contenido hacia pgvector (AQ-112)

Al arrancar, el rag-service carga los documentos de `src/main/resources/contenido/0*.md`
en la tabla `fragmentos` de Postgres (pgvector), con un embedding de 1024 dimensiones (bge-m3).

## Flujo

1. `DivisorFragmentos` parte cada documento por secciones `##` (reglas de AQ-91: une las de
   menos de 40 palabras y divide por párrafos las de más de 150).
2. A cada fragmento se le calcula un hash SHA-256.
3. Solo los hashes que no están en la tabla se envían a Ollama (`ServicioLlm.embeddings`, en lotes de 16).
4. Se borran los fragmentos del mismo origen cuyo hash ya no existe (secciones editadas o eliminadas).

Arrancar dos veces no duplica nada ni vuelve a llamar a Ollama.

## Tabla `fragmentos` (`schema.sql`)

| Columna | Uso |
|---|---|
| `origen` | `CONTENIDO` para los `.md`; otros orígenes (tablas de la réplica) llegarán después |
| `archivo`, `seccion` | Se devuelven en `fuentes` de la respuesta del chat |
| `contenido` | Texto que se pasa al modelo como contexto |
| `hash` | SHA-256, `UNIQUE`, evita duplicados |
| `embedding` | `vector(1024)` con índice HNSW (`vector_cosine_ops`) |

## Configuración

| Variable | Valor por defecto | Uso |
|---|---|---|
| `RAG_CARGA_AL_INICIAR` | `true` | Ponerla en `false` para arrancar sin cargar (por ejemplo, sin Ollama) |

Si Ollama no está disponible al arrancar, se registra un `WARN` y el servicio sigue funcionando.

## Verificar

    docker exec rag-db psql -U rag -d rag -c "SELECT archivo, seccion, vector_dims(embedding) FROM fragmentos ORDER BY id;"

Resultado esperado con el contenido actual: 18 filas de 1024 dimensiones.