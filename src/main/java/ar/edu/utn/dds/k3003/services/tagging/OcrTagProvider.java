package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrTagProvider implements TagProvider {
    private final RestTemplate http = new RestTemplate();
    private final OcrConfig cfg;

    @Override
    public boolean supports(PdI pdi) {
        return cfg.enabled() && pdi.getImageUrl() != null && !pdi.getImageUrl().isBlank();
    }

    @Override
    public List<String> extractTags(PdI pdi) throws Exception {
        String url = cfg.baseUrl()
                + "?apikey=" + URLEncoder.encode(cfg.apiKey(), StandardCharsets.UTF_8)
                + "&url=" + URLEncoder.encode(pdi.getImageUrl(), StandardCharsets.UTF_8);

        var json = http.getForObject(url, Map.class);
        String parsed = Optional.ofNullable(json)
                .map(m -> (List<Map<String, Object>>) m.get("ParsedResults"))
                .filter(list -> !list.isEmpty())
                .map(list -> (String) list.get(0).get("ParsedText"))
                .orElse("");

        pdi.setOcrText(parsed);

        // Minimal NLP: split, normalizar, filtrar stopwords/números, limitar a top-N
        Set<String> stop = Set.of("el","la","los","las","de","del","y","o","a","en","the","of","and");
        Pattern token = Pattern.compile("[\\p{L}\\p{N}]{3,}");
        Map<String,Integer> freq = new HashMap<>();

        token.matcher(parsed.toLowerCase(Locale.ROOT)).results()
                .map(r -> r.group())
                .filter(t -> !stop.contains(t))
                .forEach(t -> freq.merge(t, 1, Integer::sum));

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }

    public record OcrConfig(String baseUrl, String apiKey, boolean enabled) {}
}

