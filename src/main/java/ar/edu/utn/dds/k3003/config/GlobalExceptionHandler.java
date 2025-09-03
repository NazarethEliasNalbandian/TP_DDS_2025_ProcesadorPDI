package ar.edu.utn.dds.k3003.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

// Excepciones de tu dominio/infra:
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInexistenteException;
import ar.edu.utn.dds.k3003.exceptions.infrastructure.solicitudes.SolicitudesCommunicationException;

// Micrometer (para exportar a Datadog)
import io.micrometer.core.instrument.MeterRegistry;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String METRIC_NAME = "app.error";
    private final MeterRegistry registry;

    public GlobalExceptionHandler(MeterRegistry registry) {
        this.registry = registry;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException e) {
        return buildAndCount(HttpStatus.NOT_FOUND, "NoSuchElementException", "Not Found", e.getMessage());
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<Map<String, String>> handleInvalidParameterException(InvalidParameterException e) {
        return buildAndCount(HttpStatus.BAD_REQUEST, "InvalidParameterException", "Bad Request", e.getMessage());
    }

    // === Nuevos handlers con códigos ajustados y métricas ===

    @ExceptionHandler(HechoInactivoException.class)
    public ResponseEntity<Map<String, String>> handleHechoInactivo(HechoInactivoException e) {
        return buildAndCount(HttpStatus.UNPROCESSABLE_ENTITY, "HechoInactivoException",
                "Hecho Inactivo", e.getMessage());
    }

    @ExceptionHandler(HechoInexistenteException.class)
    public ResponseEntity<Map<String, String>> handleHechoInexistente(HechoInexistenteException e) {
        return buildAndCount(HttpStatus.NOT_FOUND, "HechoInexistenteException",
                "Hecho Inexistente", e.getMessage());
    }

    @ExceptionHandler(SolicitudesCommunicationException.class)
    public ResponseEntity<Map<String, String>> handleSolicitudesCommunication(SolicitudesCommunicationException e) {
        return buildAndCount(HttpStatus.BAD_GATEWAY, "SolicitudesCommunicationException",
                "Solicitudes Communication Error", e.getMessage());
    }

    // =======================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        return buildAndCount(HttpStatus.INTERNAL_SERVER_ERROR, e.getClass().getSimpleName(),
                "Internal Server Error", "An unexpected error occurred");
    }

    // Helper: arma respuesta y cuenta métricas {type,status}
    private ResponseEntity<Map<String, String>> buildAndCount(HttpStatus status, String type, String error, String message) {
        // métrica para Datadog
        registry.counter(METRIC_NAME, "type", type, "status", String.valueOf(status.value())).increment();

        Map<String, String> response = new HashMap<>();
        response.put("status", String.valueOf(status.value()));
        response.put("error", error);
        response.put("message", message != null ? message : "");
        return new ResponseEntity<>(response, status);
    }
}
