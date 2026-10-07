package gt.edu.umg.aquatech.ragservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import gt.edu.umg.aquatech.ragservice.api.model.ErrorResponse;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> mensajeInvalido(MethodArgumentNotValidException ex) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo("MENSAJE_INVALIDO");
        error.setMensaje("El mensaje no puede estar vacio ni superar los 1000 caracteres.");
        return ResponseEntity.badRequest().body(error);
    }
}