package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
import ar.edu.utn.dds.k3003.repository.PdIRepository;

import ar.edu.utn.dds.k3003.services.tagging.TagAggregatorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Fachada del módulo ProcesadorPdI (Entrega 4)
 * - Soporta una sola imageUrl por PdI.
 * - Si hay imageUrl, dispara procesamiento de imagen (OCR + labels) de forma asíncrona.
 * - Si no hay imageUrl, usa el clasificador simple por contenido (backward compatible).
 * - Evita reprocesar duplicados.
 */
@Slf4j
@Service
public class Fachada implements FachadaProcesadorPDI {

    private FachadaSolicitudes fachadaSolicitudes;

    @Getter
    private final PdIRepository pdiRepository;

    // Servicio opcional para extraer tags desde imageUrl (lo inyectás si implementás el pipeline).
    private final @Nullable TagAggregatorService tagService;

    private final AtomicLong generadorID = new AtomicLong(1);

    /** Constructor por defecto para tests/local (repo en memoria) */
    protected Fachada() {
        this.pdiRepository = new InMemoryPdIRepo();
        this.tagService = null;
    }

    /** Constructor solo con repo (sigue funcionando sin el pipeline de imagen) */
    @Autowired
    public Fachada(PdIRepository pdiRepository) {
        this.pdiRepository = pdiRepository;
        this.tagService = null;
    }

    /** Constructor completo (repo + pipeline de imagen) */
    @Autowired
    public Fachada(PdIRepository pdiRepository,
                   @Nullable TagAggregatorService tagAggregatorService) {
        this.pdiRepository = pdiRepository;
        this.tagService = tagAggregatorService;
    }

    @Override
    public void setFachadaSolicitudes(FachadaSolicitudes fachadaSolicitudes) {
        this.fachadaSolicitudes = fachadaSolicitudes;
    }

    @Override
    public PdIDTO procesar(PdIDTO pdiDTORecibido) {
        Objects.requireNonNull(pdiDTORecibido, "PdIDTO requerido");
        final String hechoId = pdiDTORecibido.hechoId();
        if (hechoId == null || hechoId.isBlank()) {
            throw new IllegalArgumentException("hechoId requerido en PdIDTO");
        }

        log.info("[ProcesadorPdI] procesar() recibido hechoId={}, dto={}", hechoId, pdiDTORecibido);

        // Validación mínima de imageUrl (si viene)
        if (!isValidImageUrl(pdiDTORecibido.imageUrl())) {
            throw new IllegalArgumentException("imageUrl inválida (debe ser http/https y con path de imagen)");
        }

        PdI nuevoPdI = recibirPdIDTO(pdiDTORecibido);
        log.debug("Mapeado a entidad: {}", nuevoPdI);

        // Buscar duplicado: ahora incluye imageUrl en la comparación
        Optional<PdI> yaProcesado =
                pdiRepository.findByHechoId(nuevoPdI.getHechoId()).stream()
                        .filter(p ->
                                Objects.equals(p.getDescripcion(), nuevoPdI.getDescripcion()) &&
                                        Objects.equals(p.getLugar(), nuevoPdI.getLugar()) &&
                                        Objects.equals(p.getMomento(), nuevoPdI.getMomento()) &&
                                        Objects.equals(p.getContenido(), nuevoPdI.getContenido()) &&
                                        Objects.equals(p.getImageUrl(), nuevoPdI.getImageUrl()))
                        .findFirst();

        if (yaProcesado.isPresent()) {
            log.info("PdI duplicado detectado para hechoId={} → se reutiliza el existente id={}",
                    hechoId, yaProcesado.get().getId());
            return convertirADTO(yaProcesado.get());
        }

        // Rama 1: hay imageUrl → guardamos y disparamos pipeline async (OCR + labels)
        if (nuevoPdI.getImageUrl() != null && !nuevoPdI.getImageUrl().isBlank()) {
            // Etiquetas iniciales “pendiente” (opcional), hasta que llegue el procesamiento
            if (nuevoPdI.getEtiquetas() == null || nuevoPdI.getEtiquetas().isEmpty()) {
                nuevoPdI.setEtiquetas(List.of("pendiente"));
            }
            pdiRepository.save(nuevoPdI);
            log.info("Guardado PdI id={} hechoId={} con imageUrl. Disparando procesamiento async...",
                    nuevoPdI.getId(), nuevoPdI.getHechoId());

            if (tagService != null) {
                // ⚠️ requiere @EnableAsync en tu @SpringBootApplication
                tagService.processImageTagsAsync(nuevoPdI.getId());
            } else {
                log.warn("TagAggregatorService no disponible: no se procesará imageUrl de forma automática.");
            }

            return convertirADTO(nuevoPdI);
        }

        // Rama 2: no hay imageUrl → clasificador simple por contenido (legacy)
        nuevoPdI.setEtiquetas(etiquetar(nuevoPdI.getContenido()));
        pdiRepository.save(nuevoPdI);
        log.info("Guardado PdI id={} hechoId={} (sin imageUrl) con etiquetas={}",
                nuevoPdI.getId(), nuevoPdI.getHechoId(), nuevoPdI.getEtiquetas());

        return convertirADTO(nuevoPdI);
    }

    @Override
    public PdIDTO buscarPdIPorId(String idString) {
        Long id = Long.parseLong(idString);
        PdI pdi = pdiRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el PdI con id: " + id));
        return convertirADTO(pdi);
    }

    @Override
    public List<PdIDTO> buscarPorHecho(String hechoId) {
        List<PdI> lista = pdiRepository.findByHechoId(hechoId);
        log.info("buscarPorHecho hechoId={} → {} resultados", hechoId, lista.size());
        return lista.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    @Override
    public List<PdIDTO> pdis() {
        return this.pdiRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Override
    public void borrarTodo() {
        pdiRepository.deleteAll();
        generadorID.set(1); // opcional: reiniciar IDs en memoria
        log.warn("Se borraron todos los PdIs");
    }

    // =================== Helpers ===================

    private PdIDTO convertirADTO(PdI p) {
        return new PdIDTO(
                p.getId() == null ? null : String.valueOf(p.getId()),
                p.getHechoId(),
                p.getDescripcion(),
                p.getLugar(),
                p.getMomento(),
                p.getContenido(),
                p.getEtiquetas(),      // (deprecated) fallback
                p.getImageUrl(),
                p.getAutoTags(),       // 👈 nuevo
                p.getOcrText(),        // 👈 nuevo
                p.getProcessingState(),// 👈 nuevo
                p.getProcessedAt(),    // 👈 nuevo
                p.getLastError()       // 👈 nuevo
        );
    }


    public List<String> etiquetar(String contenido) {
        List<String> etiquetas = new ArrayList<>();
        if (contenido != null) {
            String lc = contenido.toLowerCase();
            if (lc.contains("fuego")) etiquetas.add("incendio");
            if (lc.contains("agua")) etiquetas.add("inundación");
        }
        if (etiquetas.isEmpty()) etiquetas.add("sin clasificar");
        return etiquetas;
    }

    private PdI recibirPdIDTO(PdIDTO d) {
        PdI p = new PdI();
        p.setHechoId(d.hechoId());
        p.setDescripcion(d.descripcion());
        p.setLugar(d.lugar());
        p.setMomento(d.momento());
        p.setContenido(d.contenido());
        p.setEtiquetas(d.etiquetas());
        p.setImageUrl(d.imageUrl());
        return p;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) return true; // es opcional
        try {
            URI u = URI.create(url);
            String scheme = Optional.ofNullable(u.getScheme()).orElse("").toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            String path = Optional.ofNullable(u.getPath()).orElse("").toLowerCase(Locale.ROOT);
            // Permitimos extensiones comunes; si el CDN no usa extensión, igual la aceptamos.
            return path.isEmpty() || path.endsWith(".jpg") || path.endsWith(".jpeg")
                    || path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".webp");
        } catch (Exception e) {
            return false;
        }
    }
}
