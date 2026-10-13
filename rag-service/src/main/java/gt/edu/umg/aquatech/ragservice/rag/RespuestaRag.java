package gt.edu.umg.aquatech.ragservice.rag;

import java.util.List;

import gt.edu.umg.aquatech.ragservice.fragmentos.FragmentoEncontrado;

public record RespuestaRag(String texto, List<FragmentoEncontrado> fuentes) {
}