package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageLabelProvider implements TagProvider {
    private final RestTemplate http = new RestTemplate();
    private final ImgLblConfig cfg;

    @Override
    public boolean supports(PdI pdi) {
        return cfg.enabled() && pdi.getImageUrl() != null && !pdi.getImageUrl().isBlank();
    }

    @Override
    public List<String> extractTags(PdI pdi) throws Exception {
        String url = cfg.baseUrl()
                + "?url=" + URLEncoder.encode(pdi.getImageUrl(), StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", cfg.apiKey());
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<Map> resp = http.exchange(url, HttpMethod.GET, req, Map.class);
        Map body = resp.getBody();
        if (body == null) return List.of();

        // El servicio suele devolver algo tipo: { "labels": ["fire","smoke",...] }
        Object labels = body.getOrDefault("labels", List.of());
        if (labels instanceof List<?> list) {
            return list.stream().map(Object::toString).map(String::toLowerCase).distinct().limit(10).toList();
        }
        return List.of();
    }

    public record ImgLblConfig(String baseUrl, String apiKey, boolean enabled) {}
}

