package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsAspect {
    private final MeterRegistry registry;
    public MetricsAspect(MeterRegistry registry){ this.registry = registry; }

    @SneakyThrows
    @Around("execution(* ar.edu.utn.dds.k3003..ProcesadorPdI.*(..))")
    public Object timeProcesador(ProceedingJoinPoint pjp) throws Throwable {
        return registry.timer("app.pdi.proceso.latency", "method", pjp.getSignature().getName())
                .recordCallable(pjp::proceed);
    }
}
