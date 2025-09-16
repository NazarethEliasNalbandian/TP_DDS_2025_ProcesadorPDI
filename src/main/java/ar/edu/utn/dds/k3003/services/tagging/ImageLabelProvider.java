package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.clients.ImageLabelClient;
import ar.edu.utn.dds.k3003.model.PdI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageLabelProvider implements TagProvider {

    private final ImageLabelClient client;

    @Override
    public boolean supports(PdI pdi) {
        return pdi.getImageUrl() != null && !pdi.getImageUrl().isBlank();
    }

    @Override
    public List<String> extractTags(PdI pdi) throws Exception {
        List<String> labels = client.extractLabels(pdi.getImageUrl());
        pdi.setAutoTags(labels);
        return labels;
    }
}
