package gt.edu.umg.aquatech.ragservice;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import gt.edu.umg.aquatech.ragservice.api.ChatApi;
import gt.edu.umg.aquatech.ragservice.api.model.ChatRequest;
import gt.edu.umg.aquatech.ragservice.api.model.ChatResponse;
import gt.edu.umg.aquatech.ragservice.api.model.Fuente;
import gt.edu.umg.aquatech.ragservice.fragmentos.FragmentoEncontrado;
import gt.edu.umg.aquatech.ragservice.historial.HistorialConversaciones;
import gt.edu.umg.aquatech.ragservice.rag.RespuestaRag;
import gt.edu.umg.aquatech.ragservice.rag.ServicioRag;

@RestController
public class ChatController implements ChatApi {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ServicioRag rag;
    private final ObjectProvider<HistorialConversaciones> historial;

    public ChatController(ServicioRag rag, ObjectProvider<HistorialConversaciones> historial) {
        this.rag = rag;
        this.historial = historial;
    }

    @Override
    public ResponseEntity<ChatResponse> enviarMensaje(ChatRequest chatRequest) {
        String pregunta = chatRequest.getMensaje().strip();
        if (pregunta.isEmpty()) {
            throw new MensajeInvalidoException();
        }

        UUID conversacionId = chatRequest.getConversacionId() != null
                ? chatRequest.getConversacionId()
                : UUID.randomUUID();

        RespuestaRag resultado = rag.responder(pregunta);

        ChatResponse respuesta = new ChatResponse();
        respuesta.setRespuesta(resultado.texto());
        respuesta.setConversacionId(conversacionId);
        respuesta.setFuentes(resultado.fuentes().stream().map(ChatController::aFuente).toList());

        guardarHistorial(conversacionId, pregunta, respuesta);
        return ResponseEntity.ok(respuesta);
    }

    /** Si hay una implementación de historial (AQ-115), la usa; un fallo nunca bloquea la respuesta. */
    private void guardarHistorial(UUID conversacionId, String pregunta, ChatResponse respuesta) {
        historial.ifAvailable(h -> {
            try {
                h.guardar(conversacionId, pregunta, respuesta.getRespuesta(), respuesta.getFuentes());
            } catch (RuntimeException ex) {
                log.warn("No se pudo guardar el historial de la conversacion {}: {}", conversacionId, ex.getMessage());
            }
        });
    }

    private static Fuente aFuente(FragmentoEncontrado fragmento) {
        Fuente fuente = new Fuente();
        fuente.setTitulo(fragmento.seccion());
        fuente.setFragmento(fragmento.contenido());
        return fuente;
    }
}