package gt.edu.umg.aquatech.ragservice.llm;

public class ModeloNoDisponibleException extends RuntimeException {

    public ModeloNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ModeloNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}