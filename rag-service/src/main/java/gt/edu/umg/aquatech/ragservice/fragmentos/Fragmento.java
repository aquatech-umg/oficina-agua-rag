package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Un pedazo de contenido listo para vectorizar y guardar en pgvector. */
public record Fragmento(String origen, String archivo, String seccion, String contenido) {

    /** Lo que se convierte en embedding: el título ayuda a que la búsqueda encuentre mejor. */
    public String textoParaEmbedding() {
        return seccion + "\n\n" + contenido;
    }

    /** SHA-256 de todos los campos: si cambia cualquier cosa, cambia el hash. */
    public String hash() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            String base = origen + "\n" + archivo + "\n" + seccion + "\n" + contenido;
            return HexFormat.of().formatHex(sha.digest(base.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", ex);
        }
    }
}