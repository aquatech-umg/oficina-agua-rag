package gt.edu.umg.aquatech.ragservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import gt.edu.umg.aquatech.ragservice.api.model.ErrorResponse;
import gt.edu.umg.aquatech.ragservice.llm.ModeloNoDisponibleException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> mensajeInvalido(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST, "MENSAJE_INVALIDO",
                "El mensaje no puede estar vacio ni superar los 1000 caracteres.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> peticionInvalida(HttpMessageNotReadableException ex) {
        String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());

        if (detalle.contains("conversacionId")) {
            return error(HttpStatus.BAD_REQUEST, "CONVERSACION_INVALIDA",
                    "El conversacionId debe tener un formato UUID valido.");
        }

        return error(HttpStatus.BAD_REQUEST, "PETICION_INVALIDA",
                "El cuerpo de la peticion debe ser JSON valido.");
    }

    @ExceptionHandler(ModeloNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> modeloNoDisponible(ModeloNoDisponibleException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "MODELO_NO_DISPONIBLE",
                "El asistente no esta disponible en este momento. Intenta de nuevo en unos minutos.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus estado, String codigo, String mensaje) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo(codigo);
        error.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(error);
    }
}