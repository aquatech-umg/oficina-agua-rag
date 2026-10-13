package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import gt.edu.umg.aquatech.ragservice.fragmentos.DivisorFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;

class DivisorFragmentosTests {

    private final DivisorFragmentos divisor = new DivisorFragmentos();

    @Test
    void contenidoRealProduceUnFragmentoPorSeccion() throws IOException {
        int total = 0;
        for (String archivo : List.of("01-tarifas.md", "02-calculo-consumo.md",
                "03-como-pagar.md", "04-fugas.md")) {
            String markdown = new ClassPathResource("contenido/" + archivo)
                    .getContentAsString(StandardCharsets.UTF_8);
            List<Fragmento> fragmentos = divisor.dividir("CONTENIDO", archivo, markdown);
            for (Fragmento f : fragmentos) {
                int palabras = f.contenido().strip().split("\\s+").length;
                assertTrue(palabras >= 40 && palabras <= 150,
                        f.archivo() + " / " + f.seccion() + " tiene " + palabras + " palabras");
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
}