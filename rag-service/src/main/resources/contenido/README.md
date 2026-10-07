# Contenido de referencia del chatbot (AQ-91)

Documentos estáticos que el servicio RAG vectoriza para explicar conceptos del dominio.

| Archivo | Tema |
|---|---|
| 01-tarifas.md | Tipos de servicio, capacidad, tarifa fija, exceso y vigencia |
| 02-calculo-consumo.md | Cómo se mide el consumo y cómo se calcula el recibo |
| 03-como-pagar.md | Forma de pago, fecha límite y mora |
| 04-fugas.md | Qué hacer ante una fuga |

## Regla de este contenido

Solo se escribe aquí lo que se puede comprobar en el código del monolito o que es una recomendación práctica general. No se escriben datos inventados (teléfonos, horarios, direcciones, políticas municipales), porque el chatbot los respondería como reales.

## Origen de cada dato

| Dato | Fuente en el monolito |
|---|---|
| Pajas y capacidad (60 m3 por paja) | `TarifaController` |
| Tarifa fija + exceso por m3 | `LecturaController::calcularMonto` |
| Vigencia de tarifas | `TarifaController` |
| Vencimiento el día 10 y mora única | `Recibo` |
| Forma de pago: efectivo | Tabla `metodos_pago` (único método habilitado) |

Los precios de los ejemplos son ilustrativos.

## Datos dinámicos (no van en estos archivos)

Se cargarán desde la réplica MySQL de solo lectura, sin tocar el monolito:

| Dato | Tabla |
|---|---|
| Montos de las tarifas vigentes | `tarifas` |
| Teléfono, WhatsApp, correo, dirección y horario | `configuraciones_landing` |
| Preguntas frecuentes | `preguntas_frecuentes` |
| Avisos públicos vigentes | `avisos_publicos` |
| Métodos de pago activos | `metodos_pago` |

No se vectorizan datos de clientes, contadores, lecturas, recibos ni pagos.

Por confirmar con el docente: que estas tablas sean las que espera ver vectorizadas.

## Estrategia de chunks

- Un chunk por sección: cada encabezado `##` con su texto.
- Cada sección se entiende sola, sin depender de la anterior.
- Metadatos por chunk: archivo de origen y título de la sección (se devuelven en `fuentes`).

Este README no se vectoriza.