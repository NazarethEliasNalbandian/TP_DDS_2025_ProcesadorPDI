package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TagAggregatorService {

    private final List<TagProvider> providers;
    private final PdIRepository pdiRepository; // <-- ajustá el nombre si tu repo difiere

    public TagAggregatorService(List<TagProvider> providers, PdIRepository pdiRepository) {
        List<TagProvider> copy = new ArrayList<>(providers != null ? providers : List.of());
        AnnotationAwareOrderComparator.sort(copy);        // respeta @Order/Ordered
        this.providers = Collections.unmodifiableList(copy);
        this.pdiRepository = Objects.requireNonNull(pdiRepository, "PdIRepository no puede ser null");
    }

    /**
     * Busca el PdI, ejecuta todos los TagProvider soportados y persiste el PdI con las nuevas tags.
     * - Evita duplicados (conserva orden de llegada).
     * - Loguea errores por provider pero no corta la ejecución.
     * - Devuelve el PdI actualizado.
     */
    @Transactional
    public PdI processImageTags(Long pdiId) {
        Objects.requireNonNull(pdiId, "pdiId no puede ser null");

        PdI pdi = pdiRepository.findById(pdiId)
                .orElseThrow(() -> new NoSuchElementException("No existe PdI con id=" + pdiId));

        // conjunto ordenado: evita duplicados y preserva orden
        Set<String> aggregated = new LinkedHashSet<>();

        List<String> tagsList = new ArrayList<>(aggregated);

        for (TagProvider provider : providers) {
            String pname = safeName(provider);

            if (!safeSupports(provider, pdi)) {
                log.debug("Provider {} no soporta el PdI id={}, se omite.", pname, pdiId);
                continue;
            }

            try {
                List<String> tags = Optional.ofNullable(provider.extractTags(pdi)).orElse(List.of())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                if (!tags.isEmpty()) {
                    log.debug("Provider {} devolvió {} tag(s) para PdI {}.", pname, tags.size(), pdiId);
                    aggregated.addAll(tags);
                } else {
                    log.debug("Provider {} no devolvió tags para PdI {}.", pname, pdiId);
                }
            } catch (Exception ex) {
                log.warn("Fallo en provider {} para PdI {}: {} - {}",
                        pname, pdiId, ex.getClass().getSimpleName(), ex.getMessage());
                // seguimos con los demás providers
            }
        }

        // ==== Actualización del PdI ====
        // Opción A (si existe un método acumulativo):
        try {
            pdi.setAutoTags(tagsList); // <-- usa tu método real (addTags/addAll/etc.)
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            // Opción B (si solo hay setter):
            // pdi.setTags(new ArrayList<>(aggregated));
            throw e; // Dejá la opción que corresponda a tu modelo y eliminá la otra.
        }

        // persistimos y devolvemos el PdI actualizado
        PdI actualizado = pdiRepository.save(pdi);
        log.debug("PdI {} actualizado con {} tag(s).", pdiId, aggregated.size());
        return actualizado;
    }

    /* ===================== Helpers ===================== */

    private boolean safeSupports(TagProvider p, PdI pdi) {
        try {
            return p.supports(pdi);
        } catch (Exception e) {
            log.debug("supports() lanzó excepción en {}: {}", safeName(p), e.toString());
            return false;
        }
    }

    private String safeName(TagProvider p) {
        try {
            String n = p.name();
            return (n != null && !n.isBlank()) ? n : p.getClass().getSimpleName();
        } catch (Exception e) {
            return p.getClass().getSimpleName();
        }
    }
}
