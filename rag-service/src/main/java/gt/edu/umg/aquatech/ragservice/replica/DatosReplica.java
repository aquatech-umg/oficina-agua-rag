package gt.edu.umg.aquatech.ragservice.replica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los datos públicos que se leen de la réplica en una pasada. Solo tablas sin datos personales:
 * nunca clientes, contadores, lecturas, recibos, pagos ni usuarios.
 *
 * @param landing la configuración activa de la página pública, o null si no hay ninguna
 */
public record DatosReplica(
        List<Tarifa> tarifas,
        List<Servicio> servicios,
        List<MetodoPago> metodosPago,
        List<Aviso> avisos,
        List<PreguntaFrecuente> preguntas,
        Landing landing) {

    /**
     * Ojo: aunque la columna se llama precio_por_m3, el monolito la usa como el precio FIJO mensual
     * de la tarifa (ver LecturaController::calcularMonto). Por eso aquí se llama precioFijo.
     */
    public record Tarifa(long id, String nombre, String tipo, BigDecimal capacidad, BigDecimal precioFijo,
                         BigDecimal precioExceso, BigDecimal moraPorcentaje, BigDecimal moraMontoFijo,
                         LocalDate vigenteDesde, LocalDate vigenteHasta) {
    }

    public record Servicio(String nombre, String descripcion) {
    }

    public record MetodoPago(String nombre, String descripcion) {
    }

    public record Aviso(long id, String titulo, String contenido, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    public record PreguntaFrecuente(long id, String pregunta, String respuesta) {
    }

    public record Landing(String tituloPrincipal, String descripcionPrincipal, String historia, String mision,
                          String vision, String diferenciadores, String telefono, String whatsapp, String correo,
                          String direccion, String horario) {
    }
}