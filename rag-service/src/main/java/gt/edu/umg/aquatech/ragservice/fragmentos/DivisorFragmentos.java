package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Parte un documento Markdown en fragmentos según las reglas de AQ-91:
 * una sección ## por fragmento, uniendo las de menos de 40 palabras
 * y dividiendo por párrafos las de más de 150.
 */
@Component
public class DivisorFragmentos {

    static final int MIN_PALABRAS = 40;
    static final int MAX_PALABRAS = 150;

    public List<Fragmento> dividir(String origen, String archivo, String markdown) {
        List<Fragmento> fragmentos = new ArrayList<>();
        for (Seccion seccion : combinarCortas(leerSecciones(markdown))) {
            for (String parte : dividirLarga(seccion.texto())) {
                fragmentos.add(new Fragmento(origen, archivo, seccion.titulo(), parte));
            }
        }
        return fragmentos;
    }

    private record Seccion(String titulo, String texto) {
    }

    /** Cada línea "## Título" empieza una sección; el "# Título" del documento se ignora. */
    private List<Seccion> leerSecciones(String markdown) {
        List<Seccion> secciones = new ArrayList<>();
        String titulo = null;
        StringBuilder texto = new StringBuilder();

        for (String linea : markdown.replace("\r\n", "\n").split("\n")) {
            if (linea.startsWith("## ")) {
                agregar(secciones, titulo, texto);
                titulo = linea.substring(3).strip();
                texto.setLength(0);
            } else if (titulo != null && !linea.startsWith("# ")) {
                texto.append(linea).append('\n');
            }
        }
        agregar(secciones, titulo, texto);
        return secciones;
    }

    private void agregar(List<Seccion> secciones, String titulo, StringBuilder texto) {
        if (titulo != null && !texto.toString().isBlank()) {
            secciones.add(new Seccion(titulo, texto.toString().strip()));
        }
    }

    /** Une cada sección corta con la siguiente; si la corta es la última, la une con la anterior. */
    private List<Seccion> combinarCortas(List<Seccion> secciones) {
        List<Seccion> resultado = new ArrayList<>();
        Seccion pendiente = null;

        for (Seccion seccion : secciones) {
            Seccion actual = pendiente == null ? seccion : unir(pendiente, seccion);
            pendiente = null;
            if (contarPalabras(actual.texto()) < MIN_PALABRAS) {
                pendiente = actual;
            } else {
                resultado.add(actual);
            }
        }

        if (pendiente != null) {
            if (resultado.isEmpty()) {
                resultado.add(pendiente);
            } else {
                resultado.add(unir(resultado.removeLast(), pendiente));
            }
        }
        return resultado;
    }

    private Seccion unir(Seccion primera, Seccion segunda) {
        return new Seccion(primera.titulo() + " / " + segunda.titulo(),
                primera.texto() + "\n\n" + segunda.texto());
    }

    /** Si la sección pasa de MAX_PALABRAS, la parte por párrafos sin cortar un párrafo a la mitad. */
    private List<String> dividirLarga(String texto) {
        if (contarPalabras(texto) <= MAX_PALABRAS) {
            return List.of(texto);
        }

        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String parrafo : texto.split("\\n\\s*\\n")) {
            if (!actual.isEmpty()
                    && contarPalabras(actual.toString()) + contarPalabras(parrafo) > MAX_PALABRAS) {
                partes.add(actual.toString().strip());
                actual.setLength(0);
            }
            actual.append(parrafo).append("\n\n");
        }
        if (!actual.toString().isBlank()) {
            partes.add(actual.toString().strip());
        }
        return partes;
    }

    static int contarPalabras(String texto) {
        return texto.isBlank() ? 0 : texto.strip().split("\\s+").length;
    }
}