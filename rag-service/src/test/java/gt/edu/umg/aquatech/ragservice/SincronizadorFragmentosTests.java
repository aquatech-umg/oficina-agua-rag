package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;
import gt.edu.umg.aquatech.ragservice.fragmentos.RepositorioFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.SincronizadorFragmentos;
import gt.edu.umg.aquatech.ragservice.llm.ServicioLlm;

class SincronizadorFragmentosTests {

    private final RepositorioFragmentos repositorio = mock(RepositorioFragmentos.class);
    private final ServicioLlm llm = mock(ServicioLlm.class);
    private final SincronizadorFragmentos sincronizador = new SincronizadorFragmentos(repositorio, llm);

    private final Fragmento existente = new Fragmento("REPLICA", "tarifas#1", "Tarifa", "texto que ya estaba");
    private final Fragmento nuevo = new Fragmento("REPLICA", "avisos_publicos#9", "Aviso", "aviso recien publicado");

    @Test
    void soloGeneraEmbeddingsDeLoNuevoYBorraLoQueYaNoViene() {
        when(repositorio.hashesPorOrigen("REPLICA")).thenReturn(Set.of(existente.hash()));
        when(llm.embeddings(anyList())).thenReturn(List.of(new float[1024]));
        when(repositorio.borrarObsoletos(eq("REPLICA"), any())).thenReturn(2);

        SincronizadorFragmentos.Resultado resultado = sincronizador.sincronizar("REPLICA", List.of(existente, nuevo));

        verify(llm).embeddings(List.of(nuevo.textoParaEmbedding()));
        verify(repositorio).guardar(eq(nuevo), any());
        verify(repositorio, never()).guardar(eq(existente), any());
        verify(repositorio).borrarObsoletos("REPLICA", Set.of(existente.hash(), nuevo.hash()));
        assertEquals(new SincronizadorFragmentos.Resultado(2, 1, 2), resultado);
    }

    @Test
    void sinCambiosNoLlamaAOllama() {
        when(repositorio.hashesPorOrigen("REPLICA")).thenReturn(Set.of(existente.hash()));

        SincronizadorFragmentos.Resultado resultado = sincronizador.sincronizar("REPLICA", List.of(existente));

        verify(llm, never()).embeddings(anyList());
        assertEquals(1, resultado.sinCambios());
        assertEquals(false, resultado.huboCambios());
    }

    @Test
    void losFragmentosRepetidosSeGuardanUnaSolaVez() {
        when(repositorio.hashesPorOrigen("REPLICA")).thenReturn(Set.of());
        when(llm.embeddings(anyList())).thenReturn(List.of(new float[1024]));

        SincronizadorFragmentos.Resultado resultado = sincronizador.sincronizar("REPLICA", List.of(nuevo, nuevo));

        verify(repositorio, times(1)).guardar(eq(nuevo), any());
        assertEquals(1, resultado.total());
    }
}