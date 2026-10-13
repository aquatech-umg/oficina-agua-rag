package gt.edu.umg.aquatech.ragservice.historial;

import java.util.List;
import java.util.UUID;

import gt.edu.umg.aquatech.ragservice.api.model.Fuente;

/**
 * Punto de enganche para guardar cada intercambio del chat (AQ-115, DynamoDB).
 * <p>
 * Para activarlo basta con una clase {@code @Component} que implemente esta interfaz.
 * Si no hay ninguna, el historial no se guarda y el chat funciona igual.
 * ChatController atrapa cualquier excepción de {@link #guardar}: un fallo aquí nunca bloquea la respuesta.
 */
public interface HistorialConversaciones {

    void guardar(UUID conversacionId, String pregunta, String respuesta, List<Fuente> fuentes);
}