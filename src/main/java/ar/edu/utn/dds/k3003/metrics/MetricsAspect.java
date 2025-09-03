package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry registry;

    public MetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    // Ajustá el pointcut a tu paquete/clase real
    @Around("execution(* ar.edu.utn.dds.k3003..ProcesadorPdI.*(..))")
    public Object timeProcesador(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.nanoTime() - start;
            registry.timer(
                    "app.pdi.proceso.latency",
                    "method", pjp.getSignature().getName()
            ).record(duration, TimeUnit.NANOSECONDS);
        }
    }
}