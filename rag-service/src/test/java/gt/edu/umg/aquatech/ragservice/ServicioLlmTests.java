package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import gt.edu.umg.aquatech.ragservice.llm.ServicioLlm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Prueba contra Ollama real. Se activa con la variable de entorno OLLAMA_IT=true. */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "rag.carga.al-iniciar=false",
        "rag.replica.habilitada=false"
})
@EnabledIfEnvironmentVariable(named = "OLLAMA_IT", matches = "true")
class ServicioLlmTests {

    @Autowired
    private ServicioLlm llm;

    @Test
    void generaEmbeddingDe1024Dimensiones() {
        float[] vector = llm.embedding("¿Cuánto cuesta una paja de agua?");
        assertEquals(1024, vector.length);
    }

    @Test
    void respondeSinRazonamientoVisible() {
        String respuesta = llm.responder("Responde en español, en una sola línea.", "Saluda al usuario.");
        assertFalse(respuesta.isBlank());
        assertFalse(respuesta.contains("<think>"), "El modo thinking debería estar apagado");
    }
}