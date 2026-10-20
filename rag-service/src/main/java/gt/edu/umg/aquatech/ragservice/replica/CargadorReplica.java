package gt.edu.umg.aquatech.ragservice.replica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.fragmentos.SincronizadorFragmentos;
import gt.edu.umg.aquatech.ragservice.llm.ModeloNoDisponibleException;

/**
 * Cada cierto tiempo (rag.replica.intervalo, 2 minutos por defecto) lee la réplica y deja en pgvector
 * los datos públicos actuales con origen REPLICA. Gracias al hash, si nada cambió no se generan
 * embeddings; si alguien publica un aviso en el monolito, el chatbot lo conoce en la siguiente pasada.
 *
 * Si la réplica u Ollama no están disponibles, se registra un solo WARN (no uno cada 2 minutos)
 * y no se borra nada: los fragmentos de la última lectura buena se quedan.
 */
@Component
@ConditionalOnProperty(name = "rag.replica.habilitada", havingValue = "true")
public class CargadorReplica {

    private static final Logger log = LoggerFactory.getLogger(CargadorReplica.class);

    private final LectorReplica lector;
    private final TextosReplica textos;
    private final SincronizadorFragmentos sincronizador;

    private boolean primeraVez = true;
    private boolean fallando = false;

    public CargadorReplica(LectorReplica lector, TextosReplica textos, SincronizadorFragmentos sincronizador) {
        this.lector = lector;
        this.textos = textos;
        this.sincronizador = sincronizador;
    }

    @Scheduled(initialDelayString = "${rag.replica.retraso-inicial:PT15S}",
            fixedDelayString = "${rag.replica.intervalo:PT2M}")
    public void sincronizarProgramado() {
        try {
            SincronizadorFragmentos.Resultado resultado = cargar();
            if (fallando) {
                log.info("La replica volvio a estar disponible.");
            }
            if (primeraVez || fallando || resultado.huboCambios()) {
                log.info("Datos de la replica en pgvector: {} fragmentos ({} nuevos, {} sin cambios, {} borrados)",
                        resultado.total(), resultado.nuevos(), resultado.sinCambios(), resultado.borrados());
            }
            primeraVez = false;
            fallando = false;
        } catch (DataAccessException ex) {
            // getMostSpecificCause: el error real del driver (contraseña, host, puerto...)
            avisarFallo("la replica no esta disponible", ex.getMostSpecificCause().getMessage());
        } catch (ModeloNoDisponibleException ex) {
            avisarFallo("Ollama no esta disponible", ex.getMessage());
        }
    }

    public SincronizadorFragmentos.Resultado cargar() {
        DatosReplica datos = lector.leer();
        return sincronizador.sincronizar(TextosReplica.ORIGEN, textos.fragmentos(datos));
    }

    private void avisarFallo(String causa, String detalle) {
        if (!fallando) {
            log.warn("No se pudieron cargar los datos de la replica porque {}: {}. "
                    + "Se reintentara automaticamente.", causa, detalle);
        }
        fallando = true;
    }
}