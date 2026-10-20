package gt.edu.umg.aquatech.ragservice.replica;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Aviso;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Landing;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.MetodoPago;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.PreguntaFrecuente;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Servicio;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Tarifa;

/**
 * Lee de la réplica MariaDB (solo lectura) los datos públicos que conoce el chatbot.
 *
 * Tiene su propio pool de conexiones y NO lo publica como bean DataSource: si lo hiciera,
 * Spring Boot dejaría de crear el DataSource de pgvector, que es el principal del servicio.
 */
@Component
@ConditionalOnProperty(name = "rag.replica.habilitada", havingValue = "true")
public class LectorReplica implements DisposableBean {

    private final HikariDataSource pool;
    private final JdbcClient jdbc;

    public LectorReplica(ReplicaProperties propiedades) {
        this.pool = new HikariDataSource();
        pool.setPoolName("replica");
        pool.setJdbcUrl(propiedades.url());
        pool.setUsername(propiedades.usuario());
        pool.setPassword(propiedades.password());
        pool.setReadOnly(true);
        pool.setMaximumPoolSize(2);
        pool.setMinimumIdle(0);
        pool.setConnectionTimeout(5_000);
        // No falla al arrancar si la réplica está apagada; el error sale al primer intento de lectura
        pool.setInitializationFailTimeout(-1);
        this.jdbc = JdbcClient.create(pool);
    }

    public DatosReplica leer() {
        return new DatosReplica(tarifas(), servicios(), metodosPago(), avisos(), preguntas(), landing());
    }

    /** Solo las tarifas activas y vigentes hoy. */
    List<Tarifa> tarifas() {
        return jdbc.sql("""
                SELECT id, nombre, tipo, capacidad, precio_por_m3, precio_exceso_m3,
                       mora_porcentaje, mora_monto_fijo, vigente_desde, vigente_hasta
                FROM tarifas
                WHERE activo = 1
                  AND vigente_desde <= CURDATE()
                  AND (vigente_hasta IS NULL OR vigente_hasta >= CURDATE())
                ORDER BY tipo, id
                """)
                .query((rs, fila) -> new Tarifa(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("capacidad"),
                        rs.getBigDecimal("precio_por_m3"),
                        rs.getBigDecimal("precio_exceso_m3"),
                        rs.getBigDecimal("mora_porcentaje"),
                        rs.getBigDecimal("mora_monto_fijo"),
                        fecha(rs, "vigente_desde"),
                        fecha(rs, "vigente_hasta")))
                .list();
    }

    List<Servicio> servicios() {
        return jdbc.sql("SELECT nombre, descripcion FROM servicios WHERE activo = 1 ORDER BY id")
                .query((rs, fila) -> new Servicio(rs.getString("nombre"), rs.getString("descripcion")))
                .list();
    }

    List<MetodoPago> metodosPago() {
        return jdbc.sql("SELECT nombre, descripcion FROM metodos_pago WHERE activo = 1 ORDER BY id")
                .query((rs, fila) -> new MetodoPago(rs.getString("nombre"), rs.getString("descripcion")))
                .list();
    }

    /** Solo los avisos activos cuyo rango de fechas incluye hoy (sin fecha = sin límite). */
    List<Aviso> avisos() {
        return jdbc.sql("""
                SELECT id, titulo, contenido, fecha_inicio, fecha_fin
                FROM avisos_publicos
                WHERE activo = 1
                  AND (fecha_inicio IS NULL OR fecha_inicio <= CURDATE())
                  AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
                ORDER BY orden, id
                """)
                .query((rs, fila) -> new Aviso(
                        rs.getLong("id"),
                        rs.getString("titulo"),
                        rs.getString("contenido"),
                        fecha(rs, "fecha_inicio"),
                        fecha(rs, "fecha_fin")))
                .list();
    }

    List<PreguntaFrecuente> preguntas() {
        return jdbc.sql("""
                SELECT id, pregunta, respuesta
                FROM preguntas_frecuentes
                WHERE activo = 1
                ORDER BY orden, id
                """)
                .query((rs, fila) -> new PreguntaFrecuente(
                        rs.getLong("id"), rs.getString("pregunta"), rs.getString("respuesta")))
                .list();
    }

    /** La configuración activa más reciente de la página pública, o null si no hay. */
    Landing landing() {
        return jdbc.sql("""
                SELECT titulo_principal, descripcion_principal, historia, mision, vision, diferenciadores,
                       telefono, whatsapp, correo, direccion, horario
                FROM configuraciones_landing
                WHERE activo = 1
                ORDER BY id DESC
                LIMIT 1
                """)
                .query((rs, fila) -> new Landing(
                        rs.getString("titulo_principal"),
                        rs.getString("descripcion_principal"),
                        rs.getString("historia"),
                        rs.getString("mision"),
                        rs.getString("vision"),
                        rs.getString("diferenciadores"),
                        rs.getString("telefono"),
                        rs.getString("whatsapp"),
                        rs.getString("correo"),
                        rs.getString("direccion"),
                        rs.getString("horario")))
                .optional()
                .orElse(null);
    }

    private static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        Date valor = rs.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }

    @Override
    public void destroy() {
        pool.close();
    }
}