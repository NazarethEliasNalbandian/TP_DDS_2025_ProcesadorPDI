package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class HttpMetricsFilter implements Filter {

    private final MeterRegistry registry;

    public HttpMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        try {
            chain.doFilter(req, res);
        } finally {
            String method = r.getMethod();
            String path   = r.getRequestURI().replaceAll("/\\d+", "/{id}");
            registry.counter("http.requests.count", "method", method, "path", path).increment();
        }
    }
}
