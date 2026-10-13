package gt.edu.umg.aquatech.ragservice.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cómo se eligen los fragmentos de contexto.
 *
 * @param maxFragmentos   cuántos candidatos se buscan en pgvector
 * @param similitudMinima similitud mínima absoluta (0 a 1); debajo de esto la pregunta se considera fuera de tema
 * @param margenRelativo  solo entran los fragmentos que estén a lo sumo este margen por debajo del mejor
 */
@ConfigurationProperties(prefix = "rag.busqueda")
public record RagProperties(int maxFragmentos, double similitudMinima, double margenRelativo) {
}