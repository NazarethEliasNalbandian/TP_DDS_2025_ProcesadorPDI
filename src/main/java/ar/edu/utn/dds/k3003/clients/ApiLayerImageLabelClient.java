package ar.edu.utn.dds.k3003.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders; // ojo: el de Spring
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ApiLayerImageLabelClient implements ImageLabelClient {

    private static final Logger log = LoggerFactory.getLogger(ApiLayerImageLabelClient.class);

    private final RestTemplate rt;
    private final ObjectMapper om;
    private final String baseUrl; // ej: https://api.apilayer.com/image_labeling/url
    private final String apiKey;

    public ApiLayerImageLabelClient(RestTemplate rt,
                                    ObjectMapper om,
                                    @Value("${imglbl.base-url}") String baseUrl,
                                    @Value("${imglbl.apikey}") String apiKey) {
        this.rt = rt;
        this.om = om;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public List<String> extractLabels(String imageUrl) {
        long t0 = System.currentTimeMillis();

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("url", imageUrl)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        log.info("[IMGLBL] GET {}", baseUrl);
        log.debug("[IMGLBL] full URL: {}", url);

        ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET, req, String.class);
        int sc = resp.getStatusCode().value();
        String body = resp.getBody();
        log.info("[IMGLBL] status={} in {}ms", sc, (System.currentTimeMillis() - t0));
        log.debug("[IMGLBL] body<= {}", truncate(body, 500));

        if (sc != 200) throw new IllegalStateException("IMGLBL HTTP " + sc);

        try {
            String jsonStr = body == null ? "" : body.trim();
            List<String> out = new ArrayList<>();

            JsonNode root = om.readTree(jsonStr.isEmpty() ? "[]" : jsonStr);
            if (root.isArray()) {
                // Variante A: [ { "label": "...", "confidence": ... }, ... ]
                for (JsonNode el : root) {
                    if (el.isObject()) out.add(el.path("label").asText(""));
                    else out.add(el.asText(""));
                }
            } else if (root.isObject()) {
                // Variante B: { "result": [...] }  // Variante C: { "labels": [...] }
                if (root.has("result") && root.get("result").isArray()) {
                    for (JsonNode el : root.get("result")) {
                        if (el.isObject()) out.add(el.path("label").asText(""));
                        else out.add(el.asText(""));
                    }
                } else if (root.has("labels") && root.get("labels").isArray()) {
                    for (JsonNode el : root.get("labels")) {
                        if (el.isObject()) out.add(el.path("label").asText(""));
                        else out.add(el.asText(""));
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

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String firstLine(Throwable e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m.split("\\R", 2)[0];
    }
}
