# Decisión: LLM y modelo de embeddings (AQ-89)

## Contexto

El chatbot RAG necesita dos modelos: uno de embeddings, que convierte texto en vectores para buscar por similitud, y un LLM, que redacta la respuesta a partir de los fragmentos recuperados. El docente permite AWS Bedrock, OpenAI u Ollama (local).

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| Ollama (local) | Gratis, sin claves, todo corre en la máquina | Sin tarjeta de video responde lento; la máquina debe estar presente en la demo |
| OpenAI | Rápido y simple de conectar | Requiere tarjeta de pago y saldo |
| AWS Bedrock | Se puede pagar con créditos de AWS | Configurar permisos y acceso a modelos toma más tiempo |

## Decisión

Ollama local, con estos modelos:

| Función | Modelo | Motivo |
|---|---|---|
| LLM | `qwen3:8b` | Cabe en 8 GB de memoria de video y maneja bien el español |
| Embeddings | `bge-m3` | Multilingüe; el contenido está en español. Vectores de 1024 dimensiones |

Ollama puede ejecutarse en CPU, pero para la demo se utilizará GPU NVIDIA para obtener mejores tiempos de respuesta.

Versión probada localmente: Ollama 0.40.0, en una laptop con NVIDIA GeForce RTX 4070 (8 GB de video).

## Consecuencias

- `bge-m3` produce vectores de 1024 dimensiones, por lo que la columna inicial en pgvector se define como `vector(1024)`.
- Cambiar el LLM es un cambio de configuración.
- Cambiar el modelo de embeddings obliga a regenerar todos los vectores. Si el nuevo modelo utiliza una dimensión diferente, también debe ajustarse la columna `vector(...)` y el índice correspondiente en pgvector.
- Ollama se instala directo en Windows, fuera de Docker Compose, para usar la tarjeta de video. Es la única pieza externa, igual que en la arquitectura del curso.
- El razonamiento del modelo ("thinking") se desactiva desde el servicio: en un RAG el contexto ya trae la información y apagarlo reduce el tiempo de respuesta.

## Configuración

Nada de esto va fijo en el código. El servicio lo lee de variables de entorno, con valores por defecto para desarrollo local:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `LLM_PROVIDER` | `ollama` | Proveedor del modelo |
| `LLM_BASE_URL` | `http://localhost:11434` | Dirección del proveedor |
| `LLM_MODEL` | `qwen3:8b` | Modelo que redacta la respuesta |
| `EMBEDDING_MODEL` | `bge-m3` | Modelo de embeddings |
| `LLM_API_KEY` | (vacío) | Solo si se cambia a un proveedor en la nube |

Ollama no usa clave. Si se cambia a un proveedor en la nube, la clave se pasa por variable de entorno: nunca se sube a Git ni llega al navegador.

## Prueba mínima

Ejecutada el 7 de octubre de 2026.

- LLM: `ollama run qwen3:8b "Explica en dos lineas que es una tarifa de agua potable."`
  - Resultado: respondió en español, en dos líneas: "Una tarifa de agua potable es el precio que se cobra por el consumo de agua, establecido por las autoridades o empresas encargadas de su distribución. Esta tarifa cubre los costos de tratamiento, distribución, mantenimiento de infraestructuras y, en algunos casos, impuestos o tarifas adicionales."
- Embeddings: petición a `http://localhost:11434/api/embed` con el modelo `bge-m3` y el texto "tarifa vigente del agua".
  - Dimensiones obtenidas: 1024.
- Uso de recursos (`ollama ps`):
  - `bge-m3`: 664 MB, 100% GPU.
  - `qwen3:8b`: 5.6 GB, 100% GPU.
  - Los dos modelos caben juntos en la memoria de video.

## Pendiente

- Confirmar si existe alguna instrucción posterior del docente que exija un proveedor en la nube para la demo final.