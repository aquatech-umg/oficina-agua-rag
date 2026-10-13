package gt.edu.umg.aquatech.ragservice.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String model,
        String embeddingModel,
        int embeddingDimensions) {
}