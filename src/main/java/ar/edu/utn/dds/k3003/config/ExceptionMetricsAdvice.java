package ar.edu.utn.dds.k3003.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;

// Excepciones del dominio/infra de PdI:
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInexistenteException;
import ar.edu.utn.dds.k3003.exceptions.infrastructure.solicitudes.SolicitudesCommunicationException;

@ControllerAdvice
public class ExceptionMetricsAdvice {

    private static final String METRIC = "app.error";
    private final MeterRegistry registry;

    public ExceptionMetricsAdvice(MeterRegistry registry) {
        this.registry = registry;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> notFound(NoSuchElementException e) {
        registry.counter(METRIC, "type", "NoSuchElementException", "status", "404").increment();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<?> badRequest(InvalidParameterException e) {
        registry.counter(METRIC, "type", "InvalidParameterException", "status", "400").increment();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(HechoInexistenteException.class)
    public ResponseEntity<?> hechoInexistente(HechoInexistenteException e) {
        registry.counter(METRIC, "type", "HechoInexistenteException", "status", "404").increment();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(HechoInactivoException.class)
    public ResponseEntity<?> hechoInactivo(HechoInactivoException e) {
        registry.counter(METRIC, "type", "HechoInactivoException", "status", "422").increment();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }

    @ExceptionHandler(SolicitudesCommunicationException.class)
    public ResponseEntity<?> solicitudesDown(SolicitudesCommunicationException e) {
        registry.counter(METRIC, "type", "SolicitudesCommunicationException", "status", "502").increment();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> generic(Exception e) {
        registry.counter(METRIC, "type", e.getClass().getSimpleName(), "status", "500").increment();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
