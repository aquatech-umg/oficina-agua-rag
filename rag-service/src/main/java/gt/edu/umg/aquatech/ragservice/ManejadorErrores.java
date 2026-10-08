package gt.edu.umg.aquatech.ragservice;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import gt.edu.umg.aquatech.ragservice.api.model.ErrorResponse;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> mensajeInvalido(MethodArgumentNotValidException ex) {
        return error("MENSAJE_INVALIDO",
                "El mensaje no puede estar vacio ni superar los 1000 caracteres.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> peticionInvalida(HttpMessageNotReadableException ex) {
        String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());

        if (detalle.contains("conversacionId")) {
            return error("CONVERSACION_INVALIDA",
                    "El conversacionId debe tener un formato UUID valido.");
        }

        return error("PETICION_INVALIDA",
                "El cuerpo de la peticion debe ser JSON valido.");
    }

    private ResponseEntity<ErrorResponse> error(String codigo, String mensaje) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo(codigo);
        error.setMensaje(mensaje);
        return ResponseEntity.badRequest().body(error);
    }
}