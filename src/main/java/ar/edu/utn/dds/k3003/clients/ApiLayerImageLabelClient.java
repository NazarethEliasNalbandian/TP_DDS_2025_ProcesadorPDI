package ar.edu.utn.dds.k3003.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiLayerImageLabelClient implements ImageLabelClient {

    private static final Logger log = LoggerFactory.getLogger(ApiLayerImageLabelClient.class);

    private final RestTemplate rt;
    private final ObjectMapper om;
    private final String baseUrl; // ej: https://api.apilayer.com/image_labeling/url
    private final String apiKey;

    // Configurables por properties
    private final int maxAttempts;
    private final long baseBackoffMs;

    public ApiLayerImageLabelClient(RestTemplate rt,
                                    ObjectMapper om,
                                    @Value("${imglbl.base-url}") String baseUrl,
                                    @Value("${imglbl.apikey}") String apiKey,
                                    @Value("${imglbl.retry.max-attempts:3}") int maxAttempts,
                                    @Value("${imglbl.retry.base-backoff-ms:600}") long baseBackoffMs,
                                    // timeouts opcionales (si el RestTemplate no viene configurado)
                                    @Value("${imglbl.timeout.connect-ms:3000}") int connectTimeoutMs,
                                    @Value("${imglbl.timeout.read-ms:10000}") int readTimeoutMs) {
        this.rt = ensureTimeouts(rt, connectTimeoutMs, readTimeoutMs);
        this.om = om;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoffMs = Math.max(100, baseBackoffMs);
    }

    @Override
    public List<String> extractLabels(String imageUrl) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("url", imageUrl)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        long backoff = baseBackoffMs;

        for (int attempt = 1; ; attempt++) {
            long t0 = System.currentTimeMillis();
            try {
                log.info("[IMGLBL] GET {}", baseUrl);
                log.debug("[IMGLBL] full URL: {}", url);

                ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET, req, String.class);
                int sc = resp.getStatusCode().value();
                String body = resp.getBody();
                log.info("[IMGLBL] status={} in {}ms (attempt={})", sc, (System.currentTimeMillis() - t0), attempt);
                log.debug("[IMGLBL] body<= {}", truncate(body, 500));

                if (sc != 200) throw new IllegalStateException("IMGLBL HTTP " + sc);

                return parseLabels(body);

            } catch (ResourceAccessException e) {
                // timeouts / I/O
                log.warn("[IMGLBL] attempt={} timeout/I-O: {}", attempt, firstLine(e));
                if (attempt >= maxAttempts) throw e;
                sleep(backoff);
                backoff = backoff * 2; // backoff exponencial
            } catch (HttpStatusCodeException e) {
                // 4xx/5xx: reintentamos sólo si es 5xx; 4xx no suele recuperarse
                int status = e.getStatusCode().value();
                log.warn("[IMGLBL] attempt={} HTTP {}: {}", attempt, status, firstLine(e));
                if (status >= 500 && attempt < maxAttempts) {
                    sleep(backoff);
                    backoff = backoff * 2;
                } else {
                    throw e;
                }
            } catch (RuntimeException e) {
                // otros errores (parseo, etc.). Reintento 1 vez más por si fue intermitente.
                log.warn("[IMGLBL] attempt={} error: {}", attempt, firstLine(e));
                if (attempt >= maxAttempts) throw e;
                sleep(backoff);
                backoff = backoff * 2;
            }
        }
    }

    // ---- Helpers ----

    private List<String> parseLabels(String body) {
        try {
            String jsonStr = (body == null) ? "" : body.trim();
            List<String> out = new ArrayList<>();
            JsonNode root = om.readTree(jsonStr.isEmpty() ? "[]" : jsonStr);

            if (root.isArray()) {
                // Variante A: [ { "label": "...", "confidence": ... }, ... ]
                for (JsonNode el : root) {
                    out.add(el.isObject() ? el.path("label").asText("") : el.asText(""));
                }
            } else if (root.isObject()) {
                // Variante B: { "result": [...] }  // Variante C: { "labels": [...] }
                if (root.has("result") && root.get("result").isArray()) {
                    for (JsonNode el : root.get("result")) {
                        out.add(el.isObject() ? el.path("label").asText("") : el.asText(""));
                    }
                } else if (root.has("labels") && root.get("labels").isArray()) {
                    for (JsonNode el : root.get("labels")) {
                        out.add(el.isObject() ? el.path("label").asText("") : el.asText(""));
                    }
                }
            }

            return out.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(String::toLowerCase)
                    .distinct()
                    .limit(10)
                    .toList();

        } catch (Exception e) {
            throw new IllegalStateException("IMGLBL parse error: " + firstLine(e), e);
        }
    }

    private static RestTemplate ensureTimeouts(RestTemplate rt, int connectMs, int readMs) {
        // Si el RT ya tiene timeouts vía otra factory, respetalos.
        if (!(rt.getRequestFactory() instanceof SimpleClientHttpRequestFactory f)) {
            // Intento crear una factory simple con timeouts si no es de este tipo
            SimpleClientHttpRequestFactory nf = new SimpleClientHttpRequestFactory();
            nf.setConnectTimeout(connectMs);
            nf.setReadTimeout(readMs);
            rt.setRequestFactory(nf);
            return rt;
        }
        // Si es SimpleClientHttpRequestFactory, seteamos (idempotente)
        f.setConnectTimeout(connectMs);
        f.setReadTimeout(readMs);
        return rt;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String firstLine(Throwable e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m.split("\\R", 2)[0];
    }
}
