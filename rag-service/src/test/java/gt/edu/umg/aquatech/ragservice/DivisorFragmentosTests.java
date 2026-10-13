package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import gt.edu.umg.aquatech.ragservice.fragmentos.CargadorContenido;
import gt.edu.umg.aquatech.ragservice.fragmentos.DivisorFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;

class DivisorFragmentosTests {

    private static final int MIN = 40;
    private static final int MAX = 120;

    private final DivisorFragmentos divisor = new DivisorFragmentos();

    private static int palabras(Fragmento f) {
        return f.contenido().strip().split("\\s+").length;
    }

    @Test
    void contenidoRealProduceUnFragmentoPorSeccion() throws IOException {
        int total = 0;
        for (String archivo : List.of("01-tarifas.md", "02-calculo-consumo.md",
                "03-como-pagar.md", "04-fugas.md")) {
            String markdown = new ClassPathResource("contenido/" + archivo)
                    .getContentAsString(StandardCharsets.UTF_8);
            List<Fragmento> fragmentos = divisor.dividir("CONTENIDO", archivo, markdown);
            for (Fragmento f : fragmentos) {
                int n = palabras(f);
                assertTrue(n >= MIN && n <= MAX,
                        f.archivo() + " / " + f.seccion() + " tiene " + n + " palabras");
            }
            total += fragmentos.size();
        }
        assertEquals(18, total, "Los 4 documentos tienen 18 secciones ##");
    }

    @Test
    void seccionCortaSeUneConLaSiguiente() {
        String markdown = "# Doc\n\n## Corta\n\nSolo unas palabras.\n\n## Larga\n\n"
                + "palabra ".repeat(45) + "\n";
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md", markdown);
        assertEquals(1, fragmentos.size());
        assertEquals("Corta / Larga", fragmentos.getFirst().seccion());
    }

    @Test
    void seccionLargaSeDividePorParrafosConservandoElTitulo() {
        String markdown = "# Doc\n\n## Larga\n\n" + "a ".repeat(100) + "\n\n" + "b ".repeat(100) + "\n";
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md", markdown);
        assertEquals(2, fragmentos.size());
        assertEquals("Larga", fragmentos.get(1).seccion());
        fragmentos.forEach(f -> assertTrue(palabras(f) >= MIN && palabras(f) <= MAX));
    }

    @Test
    void parrafoUnicoDeMasDe120PalabrasSeDividePorOraciones() {
        String parrafo = "Esta es una oracion de prueba numero uno. ".repeat(30); // 240 palabras, un solo párrafo
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md", "# Doc\n\n## Larga\n\n" + parrafo + "\n");

        assertTrue(fragmentos.size() >= 2);
        fragmentos.forEach(f -> assertTrue(palabras(f) >= MIN && palabras(f) <= MAX,
                "Un fragmento tiene " + palabras(f) + " palabras"));
        fragmentos.forEach(f -> assertEquals("Larga", f.seccion()));
        assertEquals(240, fragmentos.stream().mapToInt(DivisorFragmentosTests::palabras).sum(),
                "No se debe perder ninguna palabra al dividir");
    }

    @Test
    void oracionSinPuntosDeMasDe120PalabrasSeDividePorPalabras() {
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md",
                "# Doc\n\n## Gigante\n\n" + "palabra ".repeat(300) + "\n");
        assertEquals(List.of(120, 120, 60), fragmentos.stream().map(DivisorFragmentosTests::palabras).toList());
    }

    @Test
    void elHashNoDependeDeLosSaltosDeLineaDeWindows() {
        String markdown = "# Doc\n\n## Seccion\n\n" + "texto ".repeat(45) + "\n";
        Fragmento conLf = divisor.dividir("PRUEBA", "doc.md", markdown).getFirst();
        Fragmento conCrlf = divisor.dividir("PRUEBA", "doc.md", markdown.replace("\n", "\r\n")).getFirst();
        assertEquals(conLf.hash(), conCrlf.hash());

        Fragmento editado = divisor.dividir("PRUEBA", "doc.md", markdown.replace("texto", "otro")).getFirst();
        assertNotEquals(conLf.hash(), editado.hash());
    }

    @Test
    void elReadmeDeContenidoNoSeVectoriza() {
        assertTrue(CargadorContenido.seVectoriza("01-tarifas.md"));
        assertTrue(CargadorContenido.seVectoriza("horarios.md"), "Cualquier .md nuevo se carga, sin importar el nombre");
        assertFalse(CargadorContenido.seVectoriza("README.md"));
        assertFalse(CargadorContenido.seVectoriza("notas.txt"));
    }

    @Test
    void seccionDe121PalabrasSinPuntosNoDejaUnFragmentoDeUnaPalabra() {
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md",
                "# Doc\n\n## Limite\n\n" + "palabra ".repeat(121) + "\n");
        assertEquals(List.of(61, 60), fragmentos.stream().map(DivisorFragmentosTests::palabras).toList());
    }

    @Test
    void oracionCortaSeguidaDeUnaLargaSeRedistribuye() {
        String texto = "Una oracion corta de exactamente diez palabras para probar esto. "
                + "palabra ".repeat(114) + "fin.";
        List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md", "# Doc\n\n## Mixta\n\n" + texto + "\n");

        fragmentos.forEach(f -> assertTrue(palabras(f) >= MIN && palabras(f) <= MAX,
                "Un fragmento tiene " + palabras(f) + " palabras"));
        assertEquals(125, fragmentos.stream().mapToInt(DivisorFragmentosTests::palabras).sum());
    }

    @Test
    void ningunaLongitudDeSeccionProduceFragmentosFueraDe40a120() {
        for (int n = MIN; n <= 600; n++) {
            List<Fragmento> fragmentos = divisor.dividir("PRUEBA", "doc.md",
                    "# Doc\n\n## Seccion\n\n" + "palabra ".repeat(n) + "\n");
            for (Fragmento f : fragmentos) {
                assertTrue(palabras(f) >= MIN && palabras(f) <= MAX,
                        "Con " + n + " palabras salió un fragmento de " + palabras(f));
            }
            assertEquals(n, fragmentos.stream().mapToInt(DivisorFragmentosTests::palabras).sum(),
                    "Con " + n + " palabras se perdió contenido");
        }
    }
}