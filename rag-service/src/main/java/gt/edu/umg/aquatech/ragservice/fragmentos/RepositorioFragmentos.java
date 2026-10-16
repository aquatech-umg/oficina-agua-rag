package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla fragmentos (pgvector).
 * <p>
 * Los vectores se pasan como {@code float[]}: el driver JDBC los envía como {@code real[]}.
 * En el INSERT, pgvector convierte {@code real[]} a {@code vector} automáticamente (cast de asignación).
 * En la búsqueda, el operador {@code <=>} no aplica esa conversión, por eso se usa {@code CAST(:vector AS vector)}.
 * Referencia: https://es.stackoverflow.com/questions/636392
 */
@Repository
public class RepositorioFragmentos {

    private final JdbcClient jdbc;

    public RepositorioFragmentos(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> hashesPorOrigen(String origen) {
        return new HashSet<>(jdbc.sql("SELECT hash FROM fragmentos WHERE origen = :origen")
                .param("origen", origen)
                .query(String.class)
                .list());
    }

    public void guardar(Fragmento fragmento, float[] embedding) {
        jdbc.sql("""
                INSERT INTO fragmentos (origen, archivo, seccion, contenido, hash, embedding)
                VALUES (:origen, :archivo, :seccion, :contenido, :hash, :embedding)
                ON CONFLICT (hash) DO NOTHING
                """)
                .param("origen", fragmento.origen())
                .param("archivo", fragmento.archivo())
                .param("seccion", fragmento.seccion())
                .param("contenido", fragmento.contenido())
                .param("hash", fragmento.hash())
                .param("embedding", embedding)
                .update();
    }

    /** Borra los fragmentos de un origen cuyo hash ya no está en los documentos (secciones editadas o eliminadas). */
    public int borrarObsoletos(String origen, Set<String> hashesVigentes) {
        if (hashesVigentes.isEmpty()) {
            return jdbc.sql("DELETE FROM fragmentos WHERE origen = :origen")
                    .param("origen", origen)
                    .update();
        }
        return jdbc.sql("DELETE FROM fragmentos WHERE origen = :origen AND hash NOT IN (:hashes)")
                .param("origen", origen)
                .param("hashes", List.copyOf(hashesVigentes))
                .update();
    }

    /**
     * Los fragmentos más parecidos al vector dado, del más al menos parecido.
     * {@code <=>} es la distancia de coseno de pgvector; similitud = 1 - distancia.
     */
    public List<FragmentoEncontrado> buscarSimilares(float[] vector, int limite) {
        return jdbc.sql("""
                SELECT archivo, seccion, contenido,
                       1 - (embedding <=> CAST(:vector AS vector)) AS similitud
                FROM fragmentos
                ORDER BY embedding <=> CAST(:vector AS vector)
                LIMIT :limite
                """)
                .param("vector", vector)
                .param("limite", limite)
                .query((rs, fila) -> new FragmentoEncontrado(
                        rs.getString("archivo"),
                        rs.getString("seccion"),
                        rs.getString("contenido"),
                        rs.getDouble("similitud")))
                .list();
    }
}