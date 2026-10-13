package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import gt.edu.umg.aquatech.ragservice.fragmentos.FragmentoEncontrado;
import gt.edu.umg.aquatech.ragservice.fragmentos.RepositorioFragmentos;
import gt.edu.umg.aquatech.ragservice.llm.ServicioLlm;
import gt.edu.umg.aquatech.ragservice.rag.RagProperties;
import gt.edu.umg.aquatech.ragservice.rag.RespuestaRag;
import gt.edu.umg.aquatech.ragservice.rag.ServicioRag;

class ServicioRagTests {

    private final ServicioLlm llm = mock(ServicioLlm.class);
    private final RepositorioFragmentos repositorio = mock(RepositorioFragmentos.class);
    private final ServicioRag rag = new ServicioRag(llm, repositorio, new RagProperties(4, 0.50, 0.10));

    @Test
    void sinFragmentosParecidosNoLlamaAlModelo() {
        when(llm.embedding(anyString())).thenReturn(new float[1024]);
        when(repositorio.buscarSimilares(any(), eq(4))).thenReturn(List.of(
                new FragmentoEncontrado("01-tarifas.md", "Qué es una tarifa", "texto", 0.20)));

        RespuestaRag respuesta = rag.responder("¿Quién ganó el mundial?");

        assertEquals(ServicioRag.SIN_INFORMACION, respuesta.texto());
        assertTrue(respuesta.fuentes().isEmpty());
        verify(llm, never()).responder(anyString(), anyString());
    }

    @Test
    void conFragmentosParecidosLosPasaAlModeloYLosDevuelveComoFuentes() {
        FragmentoEncontrado parecido = new FragmentoEncontrado("03-como-pagar.md", "Fecha límite de pago",
                "La fecha límite de pago es el día 10.", 0.71);
        FragmentoEncontrado lejano = new FragmentoEncontrado("04-fugas.md", "Qué hacer primero",
                "Cierre la llave de paso.", 0.25);
        when(llm.embedding(anyString())).thenReturn(new float[1024]);
        when(repositorio.buscarSimilares(any(), eq(4))).thenReturn(List.of(parecido, lejano));
        when(llm.responder(anyString(), anyString())).thenReturn("Puedes pagar hasta el día 10.");

        RespuestaRag respuesta = rag.responder("¿Hasta cuándo puedo pagar?");

        assertEquals("Puedes pagar hasta el día 10.", respuesta.texto());
        assertEquals(List.of(parecido), respuesta.fuentes(), "Solo se devuelven los que pasan el corte");

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(llm).responder(anyString(), mensaje.capture());
        assertTrue(mensaje.getValue().contains("La fecha límite de pago es el día 10."));
        assertTrue(mensaje.getValue().contains("PREGUNTA: ¿Hasta cuándo puedo pagar?"));
        assertFalse(mensaje.getValue().contains("Cierre la llave de paso."), "El fragmento lejano no entra al prompt");
    }

    @Test
    void soloEntranLosFragmentosCercanosAlMejor() {
        FragmentoEncontrado correcto = new FragmentoEncontrado("01-tarifas.md",
                "Tipos de servicio y capacidad mensual", "Cada paja equivale a 60 metros cúbicos.", 0.702);
        FragmentoEncontrado relleno = new FragmentoEncontrado("04-fugas.md",
                "Por qué conviene reparar pronto", "El agua que se pierde...", 0.438);
        when(llm.embedding(anyString())).thenReturn(new float[1024]);
        when(repositorio.buscarSimilares(any(), eq(4))).thenReturn(List.of(correcto, relleno));
        when(llm.responder(anyString(), anyString())).thenReturn("Una paja equivale a 60 metros cúbicos.");

        RespuestaRag respuesta = rag.responder("¿Cuántos metros cúbicos tiene una paja de agua?");

        assertEquals(List.of(correcto), respuesta.fuentes(), "El de 0.438 queda fuera por estar lejos del mejor (0.702)");
    }
}