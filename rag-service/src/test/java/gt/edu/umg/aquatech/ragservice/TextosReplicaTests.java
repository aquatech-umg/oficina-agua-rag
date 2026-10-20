package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import gt.edu.umg.aquatech.ragservice.fragmentos.DivisorFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Aviso;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Landing;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.MetodoPago;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.PreguntaFrecuente;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Servicio;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica.Tarifa;
import gt.edu.umg.aquatech.ragservice.replica.TextosReplica;

class TextosReplicaTests {

    private final TextosReplica textos = new TextosReplica(new DivisorFragmentos());

    /** Igual a la tarifa "Media paja" de los datos iniciales del equipo. */
    private static Tarifa mediaPaja() {
        return new Tarifa(1, "Tarifa Media Paja 2026", "Media paja", new BigDecimal("30.000"), new BigDecimal("5.00"),
                new BigDecimal("8.00"), new BigDecimal("10.00"), null, LocalDate.of(2026, 1, 1), null);
    }

    private static DatosReplica soloTarifas(Tarifa... tarifas) {
        return new DatosReplica(List.of(tarifas), List.of(), List.of(), List.of(), List.of(), null);
    }

    @Test
    void laTarifaSeDescribeComoPrecioFijoMensualYNoPorMetroCubico() {
        Fragmento f = textos.fragmentos(soloTarifas(mediaPaja())).getFirst();

        assertEquals("REPLICA", f.origen());
        assertEquals("tarifas#1", f.archivo());
        assertEquals("Tarifa vigente: Tarifa Media Paja 2026", f.seccion());
        assertTrue(f.contenido().contains("capacidad de 30 metros cúbicos"));
        assertTrue(f.contenido().contains("tarifa fija mensual es de Q5.00"));
        assertTrue(f.contenido().contains("exceso a Q8.00 por metro cúbico"));
        assertTrue(f.contenido().contains("10 % del monto del recibo"));
        assertTrue(f.contenido().contains("desde el 1 de enero de 2026"));
        assertFalse(f.contenido().contains("Q5.00 por metro cúbico"));
    }

    @Test
    void laMoraCubreLosCuatroCasos() {
        Tarifa base = mediaPaja();
        String ambos = textos.fragmentos(soloTarifas(new Tarifa(2, "T", "Una paja", base.capacidad(),
                base.precioFijo(), base.precioExceso(), new BigDecimal("5"), new BigDecimal("2.5"),
                base.vigenteDesde(), null))).getFirst().contenido();
        String soloMonto = textos.fragmentos(soloTarifas(new Tarifa(3, "T", "Una paja", base.capacidad(),
                base.precioFijo(), base.precioExceso(), null, new BigDecimal("2.5"),
                base.vigenteDesde(), null))).getFirst().contenido();
        String sinMora = textos.fragmentos(soloTarifas(new Tarifa(4, "T", "Una paja", base.capacidad(),
                base.precioFijo(), base.precioExceso(), null, BigDecimal.ZERO,
                base.vigenteDesde(), null))).getFirst().contenido();

        assertTrue(ambos.contains("5 % del monto del recibo más Q2.50"));
        assertTrue(soloMonto.contains("recargo por mora de Q2.50."));
        assertTrue(sinMora.contains("no tiene recargo por mora"));
    }

    @Test
    void serviciosYFormasDePagoQuedanEnUnSoloFragmentoCadaUno() {
        DatosReplica datos = new DatosReplica(List.of(),
                List.of(new Servicio("Agua potable", "Suministro de agua potable domiciliar"),
                        new Servicio("Drenaje sanitario", null)),
                List.of(new MetodoPago("Efectivo", "Pago en efectivo en la oficina"),
                        new MetodoPago("Transferencia bancaria", "")),
                List.of(), List.of(), null);

        List<Fragmento> fragmentos = textos.fragmentos(datos);

        assertEquals(2, fragmentos.size());
        assertTrue(fragmentos.get(0).contenido()
                .contains("Agua potable (Suministro de agua potable domiciliar); Drenaje sanitario."));
        assertTrue(fragmentos.get(1).contenido()
                .contains("Efectivo (Pago en efectivo en la oficina); Transferencia bancaria."));
    }

    @Test
    void elAvisoIncluyeSuVigenciaYQuitaElHtml() {
        Aviso aviso = new Aviso(7, "Corte de agua en zona 1",
                "<p>El jueves no habrá servicio<br>de 8:00 a 12:00.</p>",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));

        Fragmento f = textos.fragmentos(new DatosReplica(List.of(), List.of(), List.of(),
                List.of(aviso), List.of(), null)).getFirst();

        assertEquals("avisos_publicos#7", f.archivo());
        assertEquals("Aviso: Corte de agua en zona 1", f.seccion());
        assertFalse(f.contenido().contains("<"));
        assertTrue(f.contenido().contains("El jueves no habrá servicio"));
        assertTrue(f.contenido().contains("del 20 de octubre de 2026 al 23 de octubre de 2026"));
    }

    @Test
    void unaPreguntaLargaSeParteComoLosDocumentos() {
        String respuesta = "Revise la llave de paso y el contador antes de llamar. ".repeat(30);
        List<Fragmento> fragmentos = textos.fragmentos(new DatosReplica(List.of(), List.of(), List.of(),
                List.of(), List.of(new PreguntaFrecuente(3, "¿Qué hago si no tengo agua?", respuesta)), null));

        assertTrue(fragmentos.size() > 1);
        fragmentos.forEach(f -> {
            assertEquals("preguntas_frecuentes#3", f.archivo());
            assertTrue(f.contenido().strip().split("\\s+").length <= 120);
        });
    }

    @Test
    void laLandingSoloIncluyeLosDatosQueExisten() {
        Landing landing = new Landing(null, null, null, "Brindar agua potable de calidad a todo el municipio.",
                null, null, "7777-0000", "", null, "Palacio municipal, Jalpatagua", "Lunes a viernes de 8:00 a 16:00");

        List<Fragmento> fragmentos = textos.fragmentos(new DatosReplica(List.of(), List.of(), List.of(),
                List.of(), List.of(), landing));

        Fragmento contacto = fragmentos.getFirst();
        assertEquals("Contacto y horario de atención", contacto.seccion());
        assertTrue(contacto.contenido().contains("Teléfono: 7777-0000"));
        assertTrue(contacto.contenido().contains("Horario de atención: Lunes a viernes de 8:00 a 16:00"));
        assertFalse(contacto.contenido().contains("WhatsApp"));
        assertFalse(contacto.contenido().contains("Correo"));
        assertTrue(fragmentos.stream().anyMatch(f -> f.contenido().contains("Brindar agua potable de calidad")));
    }

    @Test
    void sinDatosNoHayFragmentos() {
        assertTrue(textos.fragmentos(new DatosReplica(List.of(), List.of(), List.of(), List.of(), List.of(), null))
                .isEmpty());
    }
}