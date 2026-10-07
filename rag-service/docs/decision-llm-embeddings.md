# Decisión: LLM y modelo de embeddings (AQ-89)

## Contexto

El chatbot RAG necesita dos modelos: uno de embeddings, que convierte texto en vectores para buscar por similitud, y un LLM, que redacta la respuesta a partir de los fragmentos recuperados. El docente permite AWS Bedrock, OpenAI u Ollama (local).

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| Ollama (local) | Gratis, sin claves, todo corre en la máquina | Requiere tarjeta de video; la máquina debe estar presente en la demo |
| OpenAI | Rápido y simple de conectar | Requiere tarjeta de pago y saldo |
| AWS Bedrock | Se puede pagar con créditos de AWS | Configurar permisos y acceso a modelos toma más tiempo |

## Decisión

Ollama local, con estos modelos:

| Función | Modelo | Motivo |
|---|---|---|
| LLM | `qwen3:8b` | Cabe en 8 GB de memoria de video y maneja bien el español |
| Embeddings | `bge-m3` | Multilingüe; el contenido está en español. Vectores de 1024 dimensiones |

Equipo de desarrollo: laptop con NVIDIA GeForce RTX 4070 (8 GB de video).

## Consecuencias

- La columna de vectores en pgvector se define como `vector(1024)`.
- Cambiar el LLM es un cambio de configuración. Cambiar el modelo de embeddings obliga a recrear la columna y regenerar todos los vectores.
- El proveedor y los nombres de los modelos se leen de configuración, no van fijos en el código. Si el docente pide un proveedor en la nube, se cambia a Bedrock sin tocar la lógica.
- Ollama se instala directo en Windows, fuera de Docker Compose, para usar la tarjeta de video. Es la única pieza externa, igual que en la arquitectura del curso.

## Prueba mínima

Pendiente de ejecutar.

- LLM: `ollama run qwen3:8b "Explica en dos lineas que es una tarifa de agua potable."`
    - Resultado:
- Embeddings: petición a `http://localhost:11434/api/embed` con el modelo `bge-m3`.
    - Dimensiones obtenidas:

## Por confirmar con el docente

- Si acepta Ollama local para la demo o exige un proveedor en la nube.