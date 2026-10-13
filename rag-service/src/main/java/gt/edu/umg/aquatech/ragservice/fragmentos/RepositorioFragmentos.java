package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
                VALUES (:origen, :archivo, :seccion, :contenido, :hash, CAST(:embedding AS vector))
                ON CONFLICT (hash) DO NOTHING
                """)
                .param("origen", fragmento.origen())
                .param("archivo", fragmento.archivo())
                .param("seccion", fragmento.seccion())
                .param("contenido", fragmento.contenido())
                .param("hash", fragmento.hash())
                .param("embedding", aTextoVector(embedding))
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

    /** pgvector recibe los vectores como texto: [0.12,-0.03,...] */
    static String aTextoVector(float[] vector) {
        StringBuilder texto = new StringBuilder(vector.length * 10).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                texto.append(',');
            }
            texto.append(vector[i]);
        }
        return texto.append(']').toString();
    }
}