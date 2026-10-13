package gt.edu.umg.aquatech.ragservice.llm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.ChatRequest;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.ChatResponse;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.EmbedRequest;
import gt.edu.umg.aquatech.ragservice.llm.OllamaDtos.EmbedResponse;

@FeignClient(name = "ollama", url = "${llm.base-url}")
public interface OllamaClient {

    @PostMapping(value = "/api/embed", consumes = MediaType.APPLICATION_JSON_VALUE)
    EmbedResponse embed(@RequestBody EmbedRequest peticion);

    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse chat(@RequestBody ChatRequest peticion);
}