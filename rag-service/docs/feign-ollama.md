# Feign hacia Ollama (AQ-111)

## Versión de Spring Cloud

| Spring Boot | Spring Cloud | OpenFeign |
|---|---|---|
| 4.1.1 | **2025.1.3** (Oakwood) | 5.0.3 |

- Spring Cloud 2025.1.x está basado en Boot 4.0.x y es compatible con Boot 4.1.x.
- Con Boot 4.1 hay que usar **2025.1.2 o más nueva**; la 2025.1.1 no funciona bien.
- Fuente: https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/

En el `pom.xml`: propiedad `spring-cloud.version`, BOM `spring-cloud-dependencies` en
`dependencyManagement` y el starter `spring-cloud-starter-openfeign`.

## Cómo está armado

- `llm/OllamaClient`: interfaz `@FeignClient` con `POST /api/embed` y `POST /api/chat`.
- `llm/ServicioLlm`: lo que usa el resto del servicio (`embedding`, `embeddings`, `responder`).
  Apaga el modo thinking de qwen3 (`think: false`).
- Si Ollama no responde, `ServicioLlm` lanza `ModeloNoDisponibleException` y la API devuelve
  `503 MODELO_NO_DISPONIBLE`.

## Configuración

| Variable | Valor local |
|---|---|
| `LLM_BASE_URL` | `http://localhost:11434` |
| `LLM_MODEL` | `qwen3:8b` |
| `EMBEDDING_MODEL` | `bge-m3` (1024 dimensiones) |

Timeouts del cliente `ollama`: conexión 5 s, lectura 50 s (menor que los 60 s del BFF).

## Prueba contra Ollama real

PowerShell, desde `rag-service`:

    $env:OLLAMA_IT="true"; .\mvnw.cmd test -Dtest=ServicioLlmTests

Con Ollama encendido pasan las 2 pruebas. Con Ollama apagado fallan con
`ModeloNoDisponibleException` (comportamiento esperado).