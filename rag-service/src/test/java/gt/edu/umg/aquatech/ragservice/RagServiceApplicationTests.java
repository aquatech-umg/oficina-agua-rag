package gt.edu.umg.aquatech.ragservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifica que el contexto arranque sin necesitar Postgres ni Ollama. */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "rag.carga.al-iniciar=false",
        "rag.replica.habilitada=false"
})
class RagServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}