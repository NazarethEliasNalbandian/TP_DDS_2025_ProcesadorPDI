package ar.edu.utn.dds.k3003.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.datadog.DatadogConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public DatadogConfig datadogConfig(
            @Value("${management.datadog.metrics.export.apiKey}") String apiKey,
            @Value("${management.datadog.metrics.export.uri:https://api.datadoghq.com}") String uri,
            @Value("${management.datadog.metrics.export.step:30s}") String step) {

        return new DatadogConfig() {

            @Override
            public String apiKey() {
                return apiKey;
            }

            @Override
            public String uri() {
                return uri;
            }

            @Override
            public java.time.Duration step() {
                // Micrometer requiere formato ISO-8601 -> PT30S, PT1M, etc.
                if (step.endsWith("s")) {
                    return java.time.Duration.ofSeconds(Long.parseLong(step.replace("s", "")));
                } else if (step.endsWith("m")) {
                    return java.time.Duration.ofMinutes(Long.parseLong(step.replace("m", "")));
                }
                return java.time.Duration.ofSeconds(30); // fallback seguro
            }

            @Override
            public String get(String key) {
                return null; // dejamos defaults
            }
        };
    }

    @Bean
    public MeterRegistry meterRegistry(DatadogConfig config) {
        DatadogMeterRegistry registry = DatadogMeterRegistry.builder(config).build();

        // Métricas de infraestructura
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmHeapPressureMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new FileDescriptorMetrics().bindTo(registry);

        return registry;
    }
}
