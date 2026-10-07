package gt.edu.umg.aquatech.ragservice;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import gt.edu.umg.aquatech.ragservice.api.ChatApi;
import gt.edu.umg.aquatech.ragservice.api.model.ChatRequest;
import gt.edu.umg.aquatech.ragservice.api.model.ChatResponse;
import gt.edu.umg.aquatech.ragservice.api.model.Fuente;

@RestController
public class ChatController implements ChatApi {

    @Override
    public ResponseEntity<ChatResponse> enviarMensaje(ChatRequest chatRequest) {
        UUID conversacionId = chatRequest.getConversacionId() != null
                ? chatRequest.getConversacionId()
                : UUID.randomUUID();

        Fuente fuente = new Fuente();
        fuente.setTitulo("Respuesta simulada");
        fuente.setFragmento("El RAG aun no esta conectado.");

        ChatResponse respuesta = new ChatResponse();
        respuesta.setRespuesta("Recibi tu pregunta: " + chatRequest.getMensaje());
        respuesta.setConversacionId(conversacionId);
        respuesta.setFuentes(List.of(fuente));

        return ResponseEntity.ok(respuesta);
    }
}