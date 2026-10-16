# Cargador del contenido hacia pgvector (AQ-112)

Al arrancar, el rag-service carga los documentos de `src/main/resources/contenido/*.md`
(excepto `README.md`) en la tabla `fragmentos` de Postgres (pgvector), con un embedding
de 1024 dimensiones (bge-m3).

## Flujo

1. `DivisorFragmentos` parte cada documento por secciones `##`, con fragmentos de 40 a 120 palabras:
   une las secciones de menos de 40 palabras y divide las de más de 120 por párrafos, luego por
   oraciones y, si hace falta, por palabras. Ningún fragmento pasa de 120 palabras.
2. A cada fragmento se le calcula un hash SHA-256.
3. Solo los hashes que no están en la tabla se envían a Ollama (`ServicioLlm.embeddings`, en lotes de 16).
4. Se borran los fragmentos del mismo origen cuyo hash ya no existe (secciones editadas o eliminadas).

Arrancar dos veces no duplica nada ni vuelve a llamar a Ollama.

## Qué documentos se cargan

- Cualquier archivo `.md` dentro de `contenido/`, sin importar su nombre.
- `README.md` no se carga: documenta las reglas internas del equipo y el chatbot no debe responderlas.
  La regla está en `CargadorContenido.seVectoriza`.

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


## Cómo se pasan los vectores (AQ-125)

`RepositorioFragmentos` pasa los embeddings como `float[]`, sin convertirlos a texto:

- El driver JDBC de PostgreSQL envía el `float[]` como `real[]`.
- **INSERT:** pgvector define un cast de asignación `real[]` → `vector`, así que `VALUES (..., :embedding)` funciona sin `CAST`.
- **Búsqueda:** el operador `<=>` no aplica casts de asignación; por eso se usa `embedding <=> CAST(:vector AS vector)`, también pasando el `float[]`.

Sin el `CAST` en la búsqueda, PostgreSQL responde `operator does not exist: vector <=> real[]`.
Referencia: https://es.stackoverflow.com/questions/636392