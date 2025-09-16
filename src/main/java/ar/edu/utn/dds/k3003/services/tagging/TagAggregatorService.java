package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TagAggregatorService {
    private final List<TagProvider> providers;
    private final PdIRepository repo;

    @Async // 🔁 si querés hacerlo async desde ya
    @Transactional
    public void processImageTagsAsync(Long pdiId) {
        PdI p = repo.findById(pdiId).orElseThrow();
        processImageTags(p);
    }

    @Transactional
    public void processImageTags(PdI p) {
        if (p.getImageUrl() == null || p.getImageUrl().isBlank()) return;
        if (p.getProcessingState() == PdI.ProcessingState.PROCESSED && p.getAutoTags() != null && !p.getAutoTags().isEmpty())
            return; // no reprocesar

        p.setProcessingState(PdI.ProcessingState.PROCESSING);
        try {
            Set<String> merged = new LinkedHashSet<>();
            for (TagProvider tp : providers) {
                if (tp.supports(p)) {
                    merged.addAll(tp.extractTags(p));
                }
            }
            // normalización final
            List<String> tags = merged.stream()
                    .map(String::toLowerCase)
                    .map(String::trim)
                    .filter(s -> s.length() >= 3)
                    .limit(20)
                    .toList();

            p.setAutoTags(tags);
            p.setProcessingState(PdI.ProcessingState.PROCESSED);
            p.setProcessedAt(LocalDateTime.now());
            p.setLastError(null);
        } catch (Exception e) {
            p.setProcessingState(PdI.ProcessingState.ERROR);
            p.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        repo.save(p);
    }
}
