package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import gt.edu.umg.aquatech.ragservice.fragmentos.DivisorFragmentos;
import gt.edu.umg.aquatech.ragservice.fragmentos.Fragmento;
import gt.edu.umg.aquatech.ragservice.replica.DatosReplica;
import gt.edu.umg.aquatech.ragservice.replica.LectorReplica;
import gt.edu.umg.aquatech.ragservice.replica.ReplicaProperties;
import gt.edu.umg.aquatech.ragservice.replica.TextosReplica;

/**
 * Lee la réplica real (oficina-agua-infra levantado, con los datos iniciales del equipo).
 * Se activa con REPLICA_IT=true; la contraseña se toma de RAG_REPLICA_PASSWORD.
 */
@EnabledIfEnvironmentVariable(named = "REPLICA_IT", matches = "true")
class LectorReplicaIntegracionTests {

    private final LectorReplica lector = new LectorReplica(new ReplicaProperties(
            System.getenv().getOrDefault("RAG_REPLICA_URL", "jdbc:mariadb://localhost:3307/oficina_agua"),
            System.getenv().getOrDefault("RAG_REPLICA_USUARIO", "lectura"),
            System.getenv().getOrDefault("RAG_REPLICA_PASSWORD", "")));

    @AfterEach
    void cerrar() {
        lector.destroy();
    }

    @Test
    void leeLasTablasPublicasYLasConvierteEnFragmentos() {
        DatosReplica datos = lector.leer();

        assertFalse(datos.tarifas().isEmpty(), "Los datos iniciales traen tarifas vigentes");
        assertFalse(datos.metodosPago().isEmpty(), "Los datos iniciales traen métodos de pago");

        List<Fragmento> fragmentos = new TextosReplica(new DivisorFragmentos()).fragmentos(datos);
        fragmentos.forEach(f -> System.out.println("[" + f.archivo() + "] " + f.seccion() + "\n" + f.contenido() + "\n"));
        assertTrue(fragmentos.stream().anyMatch(f -> f.contenido().contains("tarifa fija mensual")));
    }
}