package gt.edu.umg.aquatech.ragservice;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludController {

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        String postgres = jdbc.queryForObject("SELECT version()", String.class);
        String pgvector = jdbc.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'vector'",
                String.class);

        return Map.of(
                "servicio", "rag-service",
                "postgres", postgres,
                "pgvector", pgvector);
    }
}