package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import gt.edu.umg.aquatech.ragservice.api.model.ChatRequest;
import gt.edu.umg.aquatech.ragservice.historial.HistorialConversaciones;
import gt.edu.umg.aquatech.ragservice.rag.RespuestaRag;
import gt.edu.umg.aquatech.ragservice.rag.ServicioRag;

class ChatControllerHistorialTests {

    @SuppressWarnings("unchecked")
    @Test
    void falloDelHistorialNoBloqueaLaRespuestaDelChat() {

        ServicioRag rag = mock(ServicioRag.class);

        ObjectProvider<HistorialConversaciones> provider =
                mock(ObjectProvider.class);

        HistorialConversaciones historialQueFalla =
                mock(HistorialConversaciones.class);

        when(rag.responder(anyString()))
                .thenReturn(
                        new RespuestaRag(
                                "Respuesta correcta del chatbot.",
                                List.of()));

        doAnswer(invocation -> {
            Consumer<HistorialConversaciones> consumidor =
                    invocation.getArgument(0);

            consumidor.accept(historialQueFalla);
            return null;
        }).when(provider).ifAvailable(any());

        org.mockito.Mockito.doThrow(
                        new RuntimeException("DynamoDB no disponible"))
                .when(historialQueFalla)
                .guardar(
                        any(UUID.class),
                        anyString(),
                        anyString(),
                        any());

        ChatController controller =
                new ChatController(rag, provider);

        ChatRequest request = new ChatRequest();
        request.setMensaje("¿Cómo pago mi recibo?");

        var response = controller.enviarMensaje(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "Respuesta correcta del chatbot.",
                response.getBody().getRespuesta());
    }
}