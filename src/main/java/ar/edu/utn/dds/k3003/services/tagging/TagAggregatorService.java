package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TagAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(TagAggregatorService.class);

    private final PdIRepository repo;
    private final OcrTagProvider ocrProvider;
    private final ImageLabelProvider labelProvider;

    @Value("${features.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${features.imglbl.enabled:true}")
    private boolean imgLblEnabled;

    public TagAggregatorService(PdIRepository repo, OcrTagProvider ocrProvider, ImageLabelProvider labelProvider) {
        this.repo = repo;
        this.ocrProvider = ocrProvider;
        this.labelProvider = labelProvider;
    }

    @Async("pdiExecutor")
    public void processImageTagsAsync(Long pdiId) {
        long t0 = System.currentTimeMillis();
        MDC.put("pdiId", String.valueOf(pdiId));

        log.info("[TagAggregator] BEGIN pdiId={}", pdiId);
        try {
            PdI p = repo.findById(pdiId).orElseThrow(() ->
                    new NoSuchElementException("PdI not found: " + pdiId));
            String url = p.getImageUrl();
            log.info("[TagAggregator] imageUrl={}", url);

            boolean anySuccess = false;
            StringBuilder errorBag = new StringBuilder();

            // ---- OCR ----
            if (ocrEnabled && ocrProvider.supports(p)) {
                try {
                    var tokens = ocrProvider.extractTags(p);
                    log.info("[TagAggregator] OCR OK, lenText={}, tokens={}",
                            (p.getOcrText() == null ? 0 : p.getOcrText().length()),
                            (tokens == null ? 0 : tokens.size()));
                    anySuccess = true;
                } catch (Exception e) {
                    log.warn("[TagAggregator] OCR FAILED: {}", firstLine(e), e);
                    appendError(errorBag, "ocr: " + firstLine(e));
                }
            } else {
                log.info("[TagAggregator] OCR disabled or not supported");
            }

            // ---- Image Labeling ----
            if (imgLblEnabled && labelProvider.supports(p)) {
                try {
                    List<String> labels = labelProvider.extractTags(p);
                    log.info("[TagAggregator] Labeling OK, size={}", (labels == null ? 0 : labels.size()));
                    anySuccess = true;
                } catch (Exception e) {
                    log.warn("[TagAggregator] Labeling FAILED: {}", firstLine(e), e);
                    appendError(errorBag, "imglbl: " + firstLine(e));
                    p.setAutoTags(List.of());
                }
            } else {
                log.info("[TagAggregator] Image labeling disabled or not supported");
            }

            // ---- Estado final ----
            p.setProcessedAt(LocalDateTime.now());
            if (anySuccess) {
                p.setProcessingState(PdI.ProcessingState.PROCESSED);
                p.setLastError(isEmpty(errorBag) ? null : errorBag.toString());
            } else {
                p.setProcessingState(PdI.ProcessingState.ERROR);
                p.setLastError(isEmpty(errorBag) ? "pipeline: no step succeeded" : errorBag.toString());
            }
            repo.save(p);

            log.info("[TagAggregator] END pdiId={} state={} in {}ms",
                    pdiId, p.getProcessingState(), (System.currentTimeMillis() - t0));
        } catch (Exception fatal) {
            log.error("[TagAggregator] FATAL pdiId={}: {}", pdiId, firstLine(fatal), fatal);
            try {
                repo.findById(pdiId).ifPresent(p -> {
                    p.setProcessingState(PdI.ProcessingState.ERROR);
                    p.setLastError(firstLine(fatal));
                    p.setProcessedAt(LocalDateTime.now());
                    repo.save(p);
                });
            } catch (Exception ignore) {
                log.warn("[TagAggregator] cannot persist fatal error: {}", firstLine(ignore));
            }
        } finally {
            MDC.clear();
        }
    }

    private static void appendError(StringBuilder sb, String msg) {
        if (sb.length() > 0) sb.append(" | ");
        sb.append(msg);
    }

    private static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    private static String firstLine(Throwable e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m.split("\\R", 2)[0];
    }
}
