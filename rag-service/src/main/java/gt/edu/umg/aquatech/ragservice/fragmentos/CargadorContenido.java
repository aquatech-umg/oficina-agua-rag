package gt.edu.umg.aquatech.ragservice.fragmentos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.llm.ModeloNoDisponibleException;

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

    private static final Logger log = LoggerFactory.getLogger(CargadorContenido.class);

    private final DivisorFragmentos divisor;
    private final SincronizadorFragmentos sincronizador;

    public CargadorContenido(DivisorFragmentos divisor, SincronizadorFragmentos sincronizador) {
        this.divisor = divisor;
        this.sincronizador = sincronizador;
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
        SincronizadorFragmentos.Resultado resultado = sincronizador.sincronizar(ORIGEN, leerFragmentos());
        log.info("Contenido cargado en pgvector: {} fragmentos ({} nuevos, {} sin cambios, {} borrados)",
                resultado.total(), resultado.nuevos(), resultado.sinCambios(), resultado.borrados());
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