# Contenido de referencia del chatbot (AQ-91)

Documentos estáticos que el servicio RAG vectoriza para explicar conceptos del dominio.

| Archivo | Tema |
|---|---|
| 01-tarifas.md | Tipos de servicio, capacidad, tarifa fija, exceso y vigencia |
| 02-calculo-consumo.md | Cómo se mide el consumo y cómo se calcula el recibo |
| 03-como-pagar.md | Dónde se paga, fecha límite y mora |
| 04-fugas.md | Qué hacer ante una fuga |

## Regla de este contenido

Solo se escribe aquí lo que se puede comprobar en el código del monolito o que es una recomendación práctica general. No se escriben datos inventados (teléfonos, horarios, direcciones, políticas municipales) ni datos que cambian en el sistema (montos, métodos de pago), porque el chatbot los respondería como reales o quedarían desactualizados.

## Origen de cada dato

| Dato | Fuente en el monolito |
|---|---|
| Pajas y capacidad (60 m3 por paja) | `TarifaController` |
| Tarifa fija + exceso por m3 | `LecturaController::calcularMonto` |
| Vigencia de tarifas | `TarifaController` |
| Vencimiento el día 10 y mora única | `Recibo` |

Los precios de los ejemplos son ilustrativos.

## Datos dinámicos (no van en estos archivos)

Posibles fuentes a cargar desde la réplica MySQL de solo lectura, sin tocar el monolito:

| Dato | Tabla |
|---|---|
| Montos de las tarifas vigentes | `tarifas` |
| Teléfono, WhatsApp, correo, dirección y horario | `configuraciones_landing` |
| Preguntas frecuentes | `preguntas_frecuentes` |
| Avisos públicos vigentes | `avisos_publicos` |
| Métodos de pago activos | `metodos_pago` |

Propuesta actual del equipo: no vectorizar clientes, contadores, lecturas, recibos ni pagos, evitando incorporar datos personales u operativos innecesarios al RAG. Pendiente de confirmación con el docente, igual que la lista de tablas.

## Estrategia de chunks

- Unidad base: cada sección `##` con su texto es un chunk.
- Tamaño objetivo: entre 40 y 120 palabras por chunk.
- Si una sección tiene menos de 40 palabras, se combina con otra sección relacionada del mismo documento.
- Si una sección supera aproximadamente 120 a 150 palabras, se divide por párrafos, conservando el mismo título como metadato.
- Cada chunk se entiende solo, sin depender del anterior.
- Metadatos por chunk: archivo de origen y título de la sección (se devuelven en `fuentes`).

Los cuatro documentos actuales ya cumplen el tamaño objetivo: sus secciones tienen entre 40 y 85 palabras, así que hoy cada `##` produce exactamente un chunk.

Este README no se vectoriza.