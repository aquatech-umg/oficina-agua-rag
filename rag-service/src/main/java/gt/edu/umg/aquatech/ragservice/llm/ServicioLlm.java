package gt.edu.umg.aquatech.ragservice.llm;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import feign.FeignException;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.ChatRequest;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.ChatResponse;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.EmbedRequest;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.EmbedResponse;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.Mensaje;

@Service
public class ServicioLlm {

    private static final Logger log = LoggerFactory.getLogger(ServicioLlm.class);

    private final OllamaClient ollama;
    private final LlmProperties propiedades;

    public ServicioLlm(OllamaClient ollama, LlmProperties propiedades) {
        this.ollama = ollama;
        this.propiedades = propiedades;
    }

    /** Embedding de un solo texto (por ejemplo, la pregunta del usuario). */
    public float[] embedding(String texto) {
        return embeddings(List.of(texto)).getFirst();
    }

    /** Embeddings de varios textos en una sola llamada (lo usará el cargador de fragmentos). */
    public List<float[]> embeddings(List<String> textos) {
        EmbedResponse respuesta;
        try {
            respuesta = ollama.embed(new EmbedRequest(propiedades.embeddingModel(), textos));
        } catch (FeignException ex) {
            log.warn("Ollama no respondio al generar embeddings: {}", ex.getMessage());
            throw new ModeloNoDisponibleException("No se pudieron generar los embeddings.", ex);
        }

        if (respuesta == null || respuesta.embeddings() == null
                || respuesta.embeddings().size() != textos.size()) {
            throw new ModeloNoDisponibleException("Ollama devolvio una cantidad inesperada de embeddings.");
        }

        return respuesta.embeddings().stream().map(this::aArreglo).toList();
    }

    /** Respuesta del modelo a partir de las instrucciones del sistema y el mensaje del usuario. */
    public String responder(String instrucciones, String mensajeUsuario) {
        ChatRequest peticion = new ChatRequest(
                propiedades.model(),
                List.of(new Mensaje("system", instrucciones), new Mensaje("user", mensajeUsuario)),
                false,
                false,
                Map.of("temperature", 0.2));

        ChatResponse respuesta;
        try {
            respuesta = ollama.chat(peticion);
        } catch (FeignException ex) {
            log.warn("Ollama no respondio al generar la respuesta: {}", ex.getMessage());
            throw new ModeloNoDisponibleException("No se pudo generar la respuesta.", ex);
        }

        if (respuesta == null || respuesta.message() == null
                || respuesta.message().content() == null
                || respuesta.message().content().isBlank()) {
            throw new ModeloNoDisponibleException("Ollama devolvio una respuesta vacia.");
        }

        return respuesta.message().content().strip();
    }

    private float[] aArreglo(List<Float> valores) {
        if (valores.size() != propiedades.embeddingDimensions()) {
            throw new IllegalStateException("Se esperaban " + propiedades.embeddingDimensions()
                    + " dimensiones y Ollama devolvio " + valores.size()
                    + ". Revisa EMBEDDING_MODEL.");
        }
        float[] arreglo = new float[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            arreglo[i] = valores.get(i);
        }
        return arreglo;
    }
}