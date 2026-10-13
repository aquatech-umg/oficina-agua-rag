package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.llm.ModeloNoDisponibleException;
import gt.edu.umg.aquatech.ragservice.llm.ServicioLlm;

/**
 * Al arrancar el servicio, carga los documentos de contenido/ (excepto README.md) en pgvector.
 * Solo genera embeddings de los fragmentos nuevos o editados, y borra los que ya no existen.
 * Se desactiva con rag.carga.al-iniciar=false (variable RAG_CARGA_AL_INICIAR).
 */
@Component
@ConditionalOnProperty(name = "rag.carga.al-iniciar", havingValue = "true", matchIfMissing = true)
public class CargadorContenido implements ApplicationRunner {

    static final String ORIGEN = "CONTENIDO";
    private static final String PATRON = "classpath:contenido/*.md";
    private static final int TAMANO_LOTE = 16;

    private static final Logger log = LoggerFactory.getLogger(CargadorContenido.class);

    private final DivisorFragmentos divisor;
    private final RepositorioFragmentos repositorio;
    private final ServicioLlm llm;

    public CargadorContenido(DivisorFragmentos divisor, RepositorioFragmentos repositorio, ServicioLlm llm) {
        this.divisor = divisor;
        this.repositorio = repositorio;
        this.llm = llm;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            cargar();
        } catch (ModeloNoDisponibleException ex) {
            log.warn("No se cargo el contenido porque Ollama no esta disponible. "
                    + "Se intentara de nuevo la proxima vez que arranque el servicio.");
        }
    }

    public void cargar() {
        List<Fragmento> fragmentos = leerFragmentos();
        Set<String> existentes = repositorio.hashesPorOrigen(ORIGEN);
        List<Fragmento> nuevos = fragmentos.stream()
                .filter(f -> !existentes.contains(f.hash()))
                .toList();

        for (int inicio = 0; inicio < nuevos.size(); inicio += TAMANO_LOTE) {
            List<Fragmento> lote = nuevos.subList(inicio, Math.min(inicio + TAMANO_LOTE, nuevos.size()));
            List<float[]> vectores = llm.embeddings(lote.stream().map(Fragmento::textoParaEmbedding).toList());
            for (int i = 0; i < lote.size(); i++) {
                repositorio.guardar(lote.get(i), vectores.get(i));
            }
        }

        Set<String> vigentes = fragmentos.stream().map(Fragmento::hash).collect(Collectors.toSet());
        int borrados = repositorio.borrarObsoletos(ORIGEN, vigentes);

        log.info("Contenido cargado en pgvector: {} fragmentos ({} nuevos, {} sin cambios, {} borrados)",
                fragmentos.size(), nuevos.size(), fragmentos.size() - nuevos.size(), borrados);
    }

    /** El README de contenido/ documenta las reglas del equipo y no se vectoriza. */
    public static boolean seVectoriza(String nombreArchivo) {
        return nombreArchivo != null
                && nombreArchivo.endsWith(".md")
                && !nombreArchivo.equalsIgnoreCase("README.md");
    }

    private List<Fragmento> leerFragmentos() {
        try {
            Resource[] documentos = new PathMatchingResourcePatternResolver().getResources(PATRON);
            Arrays.sort(documentos, Comparator.comparing(Resource::getFilename));

            List<Fragmento> fragmentos = new ArrayList<>();
            for (Resource documento : documentos) {
                if (!seVectoriza(documento.getFilename())) {
                    continue;
                }
                String markdown = documento.getContentAsString(StandardCharsets.UTF_8);
                fragmentos.addAll(divisor.dividir(ORIGEN, documento.getFilename(), markdown));
            }
            return fragmentos;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudieron leer los documentos de contenido/", ex);
        }
    }
}