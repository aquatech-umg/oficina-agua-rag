package gt.edu.umg.aquatech.ragservice.fragmentos;

/** Un fragmento devuelto por la búsqueda, con su similitud de coseno (1 = idéntico). */
public record FragmentoEncontrado(String archivo, String seccion, String contenido, double similitud) {
}