package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Parte un documento Markdown en fragmentos de 40 a 120 palabras (criterio de AQ-112):
 * una sección ## por fragmento, uniendo las de menos de 40 palabras y dividiendo las de más de 120
 * por párrafos, luego por oraciones y, si hace falta, por palabras. Al final, cualquier fragmento
 * que haya quedado con menos de 40 palabras se completa con su vecino.
 */
@Component
public class DivisorFragmentos {

    static final int MIN_PALABRAS = 40;
    static final int MAX_PALABRAS = 120;

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

    /** Si la sección pasa de MAX_PALABRAS, la divide por párrafos y agrupa los que quepan juntos. */
    private List<String> dividirLarga(String texto) {
        if (contarPalabras(texto) <= MAX_PALABRAS) {
            return List.of(texto);
        }
        List<String> piezas = new ArrayList<>();
        for (String parrafo : texto.split("\\n\\s*\\n")) {
            piezas.addAll(partirParrafo(parrafo.strip()));
        }
        return completarCortos(agrupar(piezas, "\n\n"));
    }

    /**
     * Junta cada fragmento de menos de MIN_PALABRAS con su vecino. Si juntos caben en MAX_PALABRAS
     * quedan como uno solo; si no, se reparten a la mitad (como suman menos de MIN + MAX, cada mitad
     * queda entre 61 y 80 palabras). Se repite hasta que no quede ninguno corto.
     */
    private List<String> completarCortos(List<String> grupos) {
        List<String> resultado = new ArrayList<>(grupos);
        boolean cambio = true;
        while (cambio && resultado.size() > 1) {
            cambio = false;
            for (int i = 0; i < resultado.size(); i++) {
                if (contarPalabras(resultado.get(i)) >= MIN_PALABRAS) {
                    continue;
                }
                int vecino = i < resultado.size() - 1 ? i + 1 : i - 1;
                int primero = Math.min(i, vecino);
                String unido = resultado.get(primero) + "\n\n" + resultado.get(primero + 1);
                resultado.remove(primero + 1);
                resultado.remove(primero);
                if (contarPalabras(unido) <= MAX_PALABRAS) {
                    resultado.add(primero, unido);
                } else {
                    resultado.addAll(primero, partirEnDos(unido));
                }
                cambio = true;
                break;
            }
        }
        return resultado;
    }

    private List<String> partirEnDos(String texto) {
        String[] palabras = texto.strip().split("\\s+");
        int mitad = (palabras.length + 1) / 2;
        return List.of(
                String.join(" ", Arrays.copyOfRange(palabras, 0, mitad)),
                String.join(" ", Arrays.copyOfRange(palabras, mitad, palabras.length)));
    }

    /** Un párrafo que sigue pasado se parte por oraciones; una oración que sigue pasada, por palabras. */
    private List<String> partirParrafo(String parrafo) {
        if (contarPalabras(parrafo) <= MAX_PALABRAS) {
            return List.of(parrafo);
        }
        List<String> oraciones = new ArrayList<>();
        for (String oracion : parrafo.split("(?<=[.!?])\\s+")) {
            if (contarPalabras(oracion) <= MAX_PALABRAS) {
                oraciones.add(oracion);
            } else {
                oraciones.addAll(partirPorPalabras(oracion));
            }
        }
        return agrupar(oraciones, " ");
    }

    private List<String> partirPorPalabras(String texto) {
        String[] palabras = texto.strip().split("\\s+");
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < palabras.length; i += MAX_PALABRAS) {
            partes.add(String.join(" ", Arrays.copyOfRange(palabras, i, Math.min(i + MAX_PALABRAS, palabras.length))));
        }
        return partes;
    }

    /** Junta piezas consecutivas mientras el total no pase de MAX_PALABRAS. */
    private List<String> agrupar(List<String> piezas, String separador) {
        List<String> grupos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int palabrasActual = 0;
        for (String pieza : piezas) {
            int palabras = contarPalabras(pieza);
            if (palabrasActual > 0 && palabrasActual + palabras > MAX_PALABRAS) {
                grupos.add(actual.toString());
                actual.setLength(0);
                palabrasActual = 0;
            }
            if (palabrasActual > 0) {
                actual.append(separador);
            }
            actual.append(pieza);
            palabrasActual += palabras;
        }
        if (palabrasActual > 0) {
            grupos.add(actual.toString());
        }
        return grupos;
    }

    static int contarPalabras(String texto) {
        return texto.isBlank() ? 0 : texto.strip().split("\\s+").length;
    }
}