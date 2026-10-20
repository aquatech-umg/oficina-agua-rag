package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.llm.ServicioLlm;

/**
 * Deja en pgvector exactamente los fragmentos recibidos para un origen (CONTENIDO, REPLICA...):
 * genera embeddings solo de los nuevos o editados (por hash) y borra los que ya no vienen.
 * Lo usan el cargador de contenido/ (AQ-112) y el de la réplica (AQ-126).
 */
@Component
public class SincronizadorFragmentos {

    static final int TAMANO_LOTE = 16;

    private final RepositorioFragmentos repositorio;
    private final ServicioLlm llm;

    public SincronizadorFragmentos(RepositorioFragmentos repositorio, ServicioLlm llm) {
        this.repositorio = repositorio;
        this.llm = llm;
    }

    public Resultado sincronizar(String origen, List<Fragmento> fragmentos) {
        // Si dos fragmentos son idénticos, basta con uno (el hash es único en la tabla)
        Map<String, Fragmento> porHash = new LinkedHashMap<>();
        fragmentos.forEach(f -> porHash.putIfAbsent(f.hash(), f));

        Set<String> existentes = repositorio.hashesPorOrigen(origen);
        List<Fragmento> nuevos = porHash.values().stream()
                .filter(f -> !existentes.contains(f.hash()))
                .toList();

        for (int inicio = 0; inicio < nuevos.size(); inicio += TAMANO_LOTE) {
            List<Fragmento> lote = nuevos.subList(inicio, Math.min(inicio + TAMANO_LOTE, nuevos.size()));
            List<float[]> vectores = llm.embeddings(lote.stream().map(Fragmento::textoParaEmbedding).toList());
            for (int i = 0; i < lote.size(); i++) {
                repositorio.guardar(lote.get(i), vectores.get(i));
            }
        }

        int borrados = repositorio.borrarObsoletos(origen, porHash.keySet());
        return new Resultado(porHash.size(), nuevos.size(), borrados);
    }

    public record Resultado(int total, int nuevos, int borrados) {

        public int sinCambios() {
            return total - nuevos;
        }

        public boolean huboCambios() {
            return nuevos > 0 || borrados > 0;
        }
    }
}