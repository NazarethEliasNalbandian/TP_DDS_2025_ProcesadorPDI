package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMetrics {

    private final Counter consultas;
    private final Counter solicitudesCreadas;
    private final Counter errores;
    private final Counter erroresAprobacion;
    private final Timer   tiempoProcesoPdi;

    public ApplicationMetrics(MeterRegistry registry) {
        this.consultas          = Counter.builder("app.consultas.total")
                .description("Cantidad de consultas (lecturas)")
                .register(registry);
        this.solicitudesCreadas = Counter.builder("app.solicitudes.creadas")
                .description("Cantidad de solicitudes creadas")
                .register(registry);
        this.errores            = Counter.builder("app.errores.total")
                .description("Errores totales")
                .register(registry);
        this.erroresAprobacion  = Counter.builder("app.errores.aprobacion")
                .description("Errores de negocio de aprobación")
                .register(registry);
        this.tiempoProcesoPdi   = Timer.builder("app.pdi.proceso.latency")
                .description("Latencia procesar PdI")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void incConsulta() { consultas.increment(); }
    public void incSolicitudCreada() { solicitudesCreadas.increment(); }
    public void incError() { errores.increment(); }
    public void incErrorAprobacion() { erroresAprobacion.increment(); }
    public <T> T timeProceso(java.util.concurrent.Callable<T> task) {
        try {
            return tiempoProcesoPdi.recordCallable(task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
