# Datos públicos del monolito en el RAG (AQ-126)

Además de los documentos de `contenido/`, el chatbot conoce los datos públicos que la oficina
mantiene en el monolito. El rag-service los lee de la **réplica de solo lectura** (MariaDB, puerto 3307),
los convierte en texto y los guarda en pgvector con `origen = REPLICA`. El código del monolito no se toca.

## Qué tablas se cargan

| Tabla | Qué se carga | Fragmento |
|---|---|---|
| `tarifas` | Activas y vigentes hoy (`vigente_desde <= hoy` y `vigente_hasta` vacía o futura) | Uno por tarifa |
| `servicios` | Activos | Uno con todos los servicios |
| `metodos_pago` | Activos | Uno con todas las formas de pago |
| `avisos_publicos` | Activos y dentro de su rango de fechas | Uno por aviso (se parte si es largo) |
| `preguntas_frecuentes` | Activas | Una por pregunta (se parte si es larga) |
| `configuraciones_landing` | La activa más reciente | Contacto y horario + textos institucionales |

**No se carga ninguna tabla con datos personales u operativos:** clientes, contadores, lecturas,
recibos, pagos, usuarios, roles, sesiones ni auditoría.

## Detalle importante de `tarifas`

Aunque la columna se llama `precio_por_m3`, el monolito la usa como el **precio fijo mensual**
de la tarifa (ver el comentario en `LecturaController::calcularMonto`). Por eso el texto dice
*"tarifa fija mensual de Q5.00 por una capacidad de 30 metros cúbicos"* y nunca *"Q5.00 por metro cúbico"*.
En `DatosReplica.Tarifa` el campo se llama `precioFijo` para no repetir la confusión.

## Flujo

1. `CargadorReplica` corre 15 s después de arrancar y luego cada `RAG_REPLICA_INTERVALO` (2 minutos).
2. `LectorReplica` consulta las tablas con el usuario de solo lectura.
3. `TextosReplica` convierte cada registro en texto natural. Los textos largos pasan por el mismo
   `DivisorFragmentos` de los `.md` (40 a 120 palabras) y se les quita el HTML.
4. `SincronizadorFragmentos` (compartido con `CargadorContenido`) genera embeddings solo de lo nuevo
   o editado (por hash) y borra los fragmentos `REPLICA` que ya no vienen (un aviso vencido o desactivado).

Si nada cambió, no se llama a Ollama y no se escribe en el log.

## Si algo falla

- **Réplica apagada o credenciales mal:** un solo `WARN` con la causa exacta (no uno cada 2 minutos);
  el chat sigue funcionando y **no se borra nada**: quedan los fragmentos de la última lectura buena.
  Cuando la réplica vuelve, se registra "La replica volvio a estar disponible".
- **Ollama apagado:** igual, un `WARN` y se reintenta en la siguiente pasada.
- `LectorReplica` tiene su propio pool de conexiones y no lo publica como `DataSource` de Spring;
  si lo hiciera, Spring Boot dejaría de crear el `DataSource` de pgvector.

## Configuración

| Variable | Valor por defecto | Uso |
|---|---|---|
| `RAG_REPLICA_HABILITADA` | `true` | `false` para no leer la réplica |
| `RAG_REPLICA_URL` | `jdbc:mariadb://localhost:3307/oficina_agua?connectTimeout=3000` | Réplica de `oficina-agua-infra` |
| `RAG_REPLICA_USUARIO` | `lectura` | `DB_LECTURA_USER` del `.env` de infra |
| `RAG_REPLICA_PASSWORD` | *(vacía)* | `DB_LECTURA_PASSWORD` del `.env` de infra. **No se sube a Git** |
| `RAG_REPLICA_INTERVALO` | `PT2M` | Cada cuánto se relee la réplica (duración ISO-8601) |

## Cómo probarlo

1. Levantar `oficina-agua-infra` (`docker compose up -d`) y el rag-service con `RAG_REPLICA_PASSWORD`.
2. A los 15 s aparece en el log: `Datos de la replica en pgvector: 4 fragmentos (4 nuevos, ...)`
   con los datos iniciales del equipo (2 tarifas, servicios y formas de pago).
3. Preguntar en Swagger *"¿Cuánto cuesta la media paja?"*: la fuente es *Tarifa vigente: Tarifa Media Paja 2026*.
4. Publicar un aviso en la base **principal** (lo mismo que hace la pantalla de avisos del monolito):

```powershell
   docker exec db-primary mariadb -uroot oficina_agua -e "INSERT INTO avisos_publicos (titulo, contenido, fecha_inicio, fecha_fin, orden, activo, created_at, updated_at) VALUES ('Corte de agua en zona 1', 'El jueves 22 de octubre no habrá servicio de agua en la zona 1 de 8:00 a 12:00 por mantenimiento del tanque de distribución.', CURDATE(), CURDATE() + INTERVAL 3 DAY, 1, 1, NOW(), NOW());"
```

En máximo 2 minutos el log muestra `1 nuevos` y *"¿Va a haber corte de agua esta semana?"* responde con el aviso.
5. Desactivarlo (`UPDATE avisos_publicos SET activo = 0 ...`): en la siguiente pasada se borra el fragmento
   y el chat ya no lo menciona.

La prueba `LectorReplicaIntegracionTests` lee la réplica real; se activa con `REPLICA_IT=true`
y `RAG_REPLICA_PASSWORD`.