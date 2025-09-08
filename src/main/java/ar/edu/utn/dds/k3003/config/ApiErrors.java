package ar.edu.utn.dds.k3003.config;

import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        ApiError error = new ApiError(
                "BAD_REQUEST",                 // código lógico
                e.getMessage(),                // mensaje de la excepción
                "/api/pdis",                   // podés inyectar dinámicamente el path si querés
                Instant.now()                  // timestamp
        );
        System.out.println("ProcesadorPdI 400: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HechoInactivoException.class)
    public ResponseEntity<ApiError> unprocessable(RuntimeException e) {
        ApiError error = new ApiError(
                "UNPROCESSABLE_ENTITY",
                e.getMessage(),
                "/api/pdis",
                Instant.now()
        );
        System.out.println("ProcesadorPdI 422: " + e.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    // Podés agregar otros handlers si los necesitás
}
