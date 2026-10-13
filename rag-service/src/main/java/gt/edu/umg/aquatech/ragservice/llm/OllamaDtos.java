package gt.edu.umg.aquatech.ragservice.llm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class OllamaDtos {

    private OllamaDtos() {
    }

    public record EmbedRequest(String model, List<String> input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(String model, List<List<Float>> embeddings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensaje(String role, String content) {
    }

    /** think=false apaga el razonamiento de qwen3; stream=false devuelve la respuesta completa de una vez. */
    public record ChatRequest(
            String model,
            List<Mensaje> messages,
            boolean stream,
            boolean think,
            Map<String, Object> options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(String model, Mensaje message, boolean done) {
    }
}