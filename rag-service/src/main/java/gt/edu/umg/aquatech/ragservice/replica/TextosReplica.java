package gt.edu.umg.aquatech.ragservice.replica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.fragmentos.DivisorFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Aviso;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Landing;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.MetodoPago;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.PreguntaFrecuente;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Servicio;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Tarifa;

/**
 * Convierte los registros de la réplica en texto natural, que es lo que mejor entienden el modelo
 * de embeddings y el LLM. Cada registro queda en su propio fragmento (archivo = "tabla#id"),
 * y los textos largos (avisos, preguntas, historia) se parten con el mismo DivisorFragmentos de los .md.
 */
@Component
public class TextosReplica {

    public static final String ORIGEN = "REPLICA";

    private static final Locale ES = Locale.forLanguageTag("es-GT");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);

    private final DivisorFragmentos divisor;

    public TextosReplica(DivisorFragmentos divisor) {
        this.divisor = divisor;
    }

    public List<Fragmento> fragmentos(DatosReplica datos) {
        List<Fragmento> fragmentos = new ArrayList<>();
        datos.tarifas().forEach(t -> fragmentos.add(tarifa(t)));
        servicios(datos.servicios()).ifPresent(fragmentos::add);
        metodosPago(datos.metodosPago()).ifPresent(fragmentos::add);
        datos.avisos().forEach(a -> fragmentos.addAll(aviso(a)));
        datos.preguntas().forEach(p -> fragmentos.addAll(pregunta(p)));
        if (datos.landing() != null) {
            fragmentos.addAll(landing(datos.landing()));
        }
        return fragmentos;
    }

    /** La columna precio_por_m3 es el precio FIJO mensual (así lo calcula el monolito). */
    Fragmento tarifa(Tarifa t) {
        StringBuilder texto = new StringBuilder()
                .append("La tarifa vigente «").append(limpiar(t.nombre())).append("» se aplica al tipo de servicio ")
                .append(limpiar(t.tipo())).append(". ");

        if (t.capacidad() != null) {
            texto.append("Incluye una capacidad de ").append(cantidad(t.capacidad()))
                    .append(" metros cúbicos al mes y su tarifa fija mensual es de ").append(quetzales(t.precioFijo()))
                    .append(", que se cobra completa aunque se consuma menos de esa capacidad. ");
            if (t.precioExceso() != null) {
                texto.append("Cada metro cúbico que pase de los ").append(cantidad(t.capacidad()))
                        .append(" metros cúbicos se cobra como exceso a ").append(quetzales(t.precioExceso()))
                        .append(" por metro cúbico. ");
            }
        } else {
            texto.append("Su tarifa fija mensual es de ").append(quetzales(t.precioFijo())).append(". ");
        }

        texto.append(mora(t.moraPorcentaje(), t.moraMontoFijo())).append(' ');
        texto.append("Está vigente desde el ").append(fecha(t.vigenteDesde()));
        if (t.vigenteHasta() != null) {
            texto.append(" hasta el ").append(fecha(t.vigenteHasta()));
        }
        texto.append(". Estos montos son los oficiales registrados en el sistema de la Oficina Municipal de Agua.");

        return new Fragmento(ORIGEN, "tarifas#" + t.id(), "Tarifa vigente: " + limpiar(t.nombre()), texto.toString());
    }

    static String mora(BigDecimal porcentaje, BigDecimal montoFijo) {
        boolean hayPorcentaje = porcentaje != null && porcentaje.signum() > 0;
        boolean hayMonto = montoFijo != null && montoFijo.signum() > 0;
        String inicio = "Si el recibo se paga después de la fecha límite, se agrega un recargo por mora de ";
        if (hayPorcentaje && hayMonto) {
            return inicio + cantidad(porcentaje) + " % del monto del recibo más " + quetzales(montoFijo) + ".";
        }
        if (hayPorcentaje) {
            return inicio + cantidad(porcentaje) + " % del monto del recibo.";
        }
        if (hayMonto) {
            return inicio + quetzales(montoFijo) + ".";
        }
        return "Esta tarifa no tiene recargo por mora.";
    }

    /** Un solo fragmento con todos los servicios: responde mejor a "¿qué servicios ofrecen?". */
    Optional<Fragmento> servicios(List<Servicio> servicios) {
        if (servicios.isEmpty()) {
            return Optional.empty();
        }
        String lista = servicios.stream()
                .map(s -> conDescripcion(s.nombre(), s.descripcion()))
                .collect(Collectors.joining("; "));
        String texto = "Servicios que presta la Oficina Municipal de Agua: " + lista + ".";
        return Optional.of(new Fragmento(ORIGEN, "servicios", "Servicios que ofrece la oficina", texto));
    }

    /** Un solo fragmento con todas las formas de pago activas. */
    Optional<Fragmento> metodosPago(List<MetodoPago> metodos) {
        if (metodos.isEmpty()) {
            return Optional.empty();
        }
        String lista = metodos.stream()
                .map(m -> conDescripcion(m.nombre(), m.descripcion()))
                .collect(Collectors.joining("; "));
        String texto = "Formas de pago que acepta la Oficina Municipal de Agua para pagar el recibo de agua: "
                + lista + ". Son los métodos de pago activos registrados en el sistema.";
        return Optional.of(new Fragmento(ORIGEN, "metodos_pago", "Formas de pago aceptadas", texto));
    }

    List<Fragmento> aviso(Aviso a) {
        StringBuilder texto = new StringBuilder("Aviso público vigente de la Oficina Municipal de Agua: ")
                .append(limpiar(a.titulo())).append(".\n\n")
                .append(limpiar(a.contenido()));
        if (a.fechaInicio() != null && a.fechaFin() != null) {
            texto.append("\n\nEste aviso es válido del ").append(fecha(a.fechaInicio()))
                    .append(" al ").append(fecha(a.fechaFin())).append('.');
        } else if (a.fechaFin() != null) {
            texto.append("\n\nEste aviso es válido hasta el ").append(fecha(a.fechaFin())).append('.');
        }
        return dividir("avisos_publicos#" + a.id(), "Aviso: " + limpiar(a.titulo()), texto.toString());
    }

    List<Fragmento> pregunta(PreguntaFrecuente p) {
        String texto = "Pregunta frecuente: " + limpiar(p.pregunta()) + "\n\nRespuesta: " + limpiar(p.respuesta());
        return dividir("preguntas_frecuentes#" + p.id(), limpiar(p.pregunta()), texto);
    }

    List<Fragmento> landing(Landing l) {
        List<Fragmento> fragmentos = new ArrayList<>();

        List<String> contacto = new ArrayList<>();
        agregarDato(contacto, "Teléfono", l.telefono());
        agregarDato(contacto, "WhatsApp", l.whatsapp());
        agregarDato(contacto, "Correo electrónico", l.correo());
        agregarDato(contacto, "Dirección", l.direccion());
        agregarDato(contacto, "Horario de atención", l.horario());
        if (!contacto.isEmpty()) {
            String texto = "Datos de contacto oficiales de la Oficina Municipal de Agua. "
                    + String.join(". ", contacto) + ".";
            fragmentos.add(new Fragmento(ORIGEN, "configuraciones_landing#contacto",
                    "Contacto y horario de atención", texto));
        }

        // Textos institucionales: un "## título" por campo y el divisor los agrupa en 40-120 palabras
        StringBuilder markdown = new StringBuilder();
        agregarSeccion(markdown, "Sobre la Oficina Municipal de Agua",
                unir(limpiar(l.tituloPrincipal()), limpiar(l.descripcionPrincipal())));
        agregarSeccion(markdown, "Historia de la oficina", limpiar(l.historia()));
        agregarSeccion(markdown, "Misión de la oficina", limpiar(l.mision()));
        agregarSeccion(markdown, "Visión de la oficina", limpiar(l.vision()));
        agregarSeccion(markdown, "Por qué elegir a la oficina", limpiar(l.diferenciadores()));
        if (!markdown.isEmpty()) {
            fragmentos.addAll(divisor.dividir(ORIGEN, "configuraciones_landing#institucional", markdown.toString()));
        }
        return fragmentos;
    }

    private List<Fragmento> dividir(String archivo, String titulo, String texto) {
        return divisor.dividir(ORIGEN, archivo, "## " + titulo + "\n\n" + texto);
    }

    private static void agregarDato(List<String> datos, String etiqueta, String valor) {
        String limpio = limpiar(valor);
        if (!limpio.isEmpty()) {
            datos.add(etiqueta + ": " + limpio);
        }
    }

    private static void agregarSeccion(StringBuilder markdown, String titulo, String texto) {
        if (!texto.isEmpty()) {
            markdown.append("## ").append(titulo).append("\n\n").append(texto).append("\n\n");
        }
    }

    private static String unir(String primero, String segundo) {
        if (primero.isEmpty()) {
            return segundo;
        }
        return segundo.isEmpty() ? primero : primero + ". " + segundo;
    }

    private static String conDescripcion(String nombre, String descripcion) {
        String d = limpiar(descripcion);
        return d.isEmpty() ? limpiar(nombre) : limpiar(nombre) + " (" + d + ")";
    }

    /**
     * Quita etiquetas HTML (los textos vienen de formularios del monolito), espacios de más y
     * líneas que empiecen con # (para que no se confundan con títulos Markdown en el divisor).
     */
    static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinHtml = texto.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
        StringBuilder resultado = new StringBuilder();
        for (String linea : sinHtml.replace("\r\n", "\n").split("\n")) {
            String limpia = linea.replaceAll("[ \\t]+", " ").strip().replaceFirst("^#+\\s*", "");
            resultado.append(limpia).append('\n');
        }
        return resultado.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    static String quetzales(BigDecimal monto) {
        return "Q" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    static String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }
}