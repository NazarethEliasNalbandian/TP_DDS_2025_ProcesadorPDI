package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.clients.OcrClient;
import ar.edu.utn.dds.k3003.model.PdI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrTagProvider implements TagProvider {

    private final OcrClient ocrClient;

    @Override
    public boolean supports(PdI pdi) {
        return pdi.getImageUrl() != null && !pdi.getImageUrl().isBlank();
    }

    @Override
    public List<String> extractTags(PdI pdi) throws Exception {
        String parsed = ocrClient.extractText(pdi.getImageUrl());
        pdi.setOcrText(parsed);

        // Tokenizado muy simple (top-10)
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
}
