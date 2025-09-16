package ar.edu.utn.dds.k3003.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OcrSpaceClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(OcrSpaceClient.class);

    private final RestTemplate rt;
    private final ObjectMapper om;
    private final String baseUrl; // ej: https://api.ocr.space/parse/imageurl
    private final String apiKey;

    public OcrSpaceClient(RestTemplate rt,
                          ObjectMapper om,
                          @Value("${ocr.base-url}") String baseUrl,
                          @Value("${ocr.apikey}") String apiKey) {
        this.rt = rt;
        this.om = om;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String extractText(String imageUrl) {
        long t0 = System.currentTimeMillis();

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("url", imageUrl)
                .toUriString();

        log.info("[OCR] GET {}", baseUrl);
        log.debug("[OCR] full URL (masked): {}", url.replace(apiKey, "***"));

        ResponseEntity<String> resp = rt.getForEntity(url, String.class);
        int sc = resp.getStatusCode().value();
        String body = resp.getBody();
        log.info("[OCR] status={} in {}ms", sc, (System.currentTimeMillis() - t0));
        log.debug("[OCR] body<= {}", truncate(body, 500));

        if (sc != 200) throw new IllegalStateException("OCR HTTP " + sc);

        try {
            JsonNode root = om.readTree(body == null ? "{}" : body);
            int exit = root.path("OCRExitCode").asInt(0);
            if (exit != 1) {
                String msg = root.path("ErrorMessage").asText("unknown");
                throw new IllegalStateException("OCRExitCode=" + exit + " msg=" + msg);
            }
            JsonNode results = root.path("ParsedResults");
            if (!results.isArray() || results.isEmpty()) return "";
            return results.get(0).path("ParsedText").asText("");
        } catch (Exception e) {
            throw new IllegalStateException("OCR parse error: " + firstLine(e), e);
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
