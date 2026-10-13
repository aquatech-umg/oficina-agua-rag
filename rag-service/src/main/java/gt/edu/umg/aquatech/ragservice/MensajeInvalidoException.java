package gt.edu.umg.aquatech.ragservice;

public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException() {
        super("El mensaje no puede estar vacio.");
    }
}