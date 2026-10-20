package gt.edu.umg.aquatech.ragservice.replica;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Conexión de solo lectura a la réplica MariaDB del monolito (sección rag.replica del yaml). */
@ConfigurationProperties(prefix = "rag.replica")
public record ReplicaProperties(String url, String usuario, String password) {
}