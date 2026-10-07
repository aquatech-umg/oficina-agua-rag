# Contenido de referencia del chatbot (AQ-91)

Documentos que el servicio RAG vectoriza para responder preguntas del dominio.

| Archivo | Tema |
|---|---|
| 01-tarifas.md | Tipos de servicio, capacidad, precios y vigencia |
| 02-calculo-consumo.md | Cómo se calcula el consumo y el monto del recibo |
| 03-como-pagar.md | Dónde, cuándo y cómo pagar; mora |
| 04-fugas.md | Qué hacer ante una fuga |

## Origen de los datos

- Tomado de la lógica del monolito: pajas y capacidad (60 m3 por paja), precio normal y por exceso, vigencia, fórmula del monto, vencimiento el día 10 y mora aplicada una sola vez.
- Inventado para el proyecto: ubicación y horario de la ventanilla, formas de pago, teléfonos (5555-0100 y 5555-0199), reglas sobre fugas y ajuste de recibos.
- Los precios de los ejemplos son ilustrativos. Los montos reales de cada tarifa vigente se cargarán desde la réplica de MySQL.

## Estrategia de chunks

- Un chunk por sección: cada encabezado `##` con su párrafo.
- Cada sección se entiende sola, sin depender de la anterior.
- Tamaño aproximado: entre 60 y 130 palabras por chunk.
- Metadatos por chunk: archivo de origen y título de la sección (se devuelven en `fuentes`).

Este README no se vectoriza.
