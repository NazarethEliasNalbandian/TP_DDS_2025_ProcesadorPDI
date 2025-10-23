package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import ar.edu.utn.dds.k3003.services.tagging.TagAggregatorService;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // Servicio opcional para extraer tags desde imageUrl
    private final @Nullable TagAggregatorService tagService;

    private final AtomicLong generadorID = new AtomicLong(1);

    /** Constructor por defecto para tests/local (repo en memoria). */
    protected Fachada() {
        this.pdiRepository = new InMemoryPdIRepo();
        this.tagService = null;
    }

    /** ÚNICO constructor autowireable (repo + servicio opcional). */
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

    @Transactional
    @Override
    public PdIDTO procesar(PdIDTO entrada) {

        PdI existente = pdiRepository
                .findByHechoIdAndImageUrl(entrada.hechoId(), entrada.imageUrl());

        System.out.println("Existente : " + existente);

        if (existente != null) {
            return convertirADTO(existente); // evitar reprocesar
        }


        // map DTO -> entity inicial
        PdI p = new PdI();
        p.setHechoId(entrada.hechoId());
        p.setDescripcion(entrada.descripcion());
        p.setLugar(entrada.lugar());
        p.setMomento(entrada.momento());
        p.setContenido(entrada.contenido());
        p.setImageUrl(entrada.imageUrl());
        p.setProcessingState(
                (entrada.imageUrl() != null && !entrada.imageUrl().isBlank())
                        ? PdI.ProcessingState.PROCESSING // o PENDING si querés, pero lo vamos a completar acá mismo
                        : PdI.ProcessingState.PROCESSED
        );

        p = pdiRepository.save(p);

        // si hay imagen, ejecutar pipeline sincrónico
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            p = tagService.processImageTags(p.getId()); // <--- ahora bloqueante
        } else {
            // sin imagen, dejalo PROCESSED y sin autoTags/ocrText
            p.setProcessedAt(LocalDateTime.now());
            p = pdiRepository.save(p);
        }

        return convertirADTO(p);
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
        List<PdI> pdis = pdiRepository.findByHechoId(hechoId);
        log.info("PDIs encontradas para hechoId={}: {}", hechoId, pdis.size());
        pdis.forEach(pdi -> log.info("PDI: {}", pdi));
        return pdis.stream().map(this::convertirADTO).toList();
    }

    @Override
    public List<PdIDTO> pdis() {
        return pdiRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Override
    public void borrarTodo() {
        pdiRepository.deleteAll();
        generadorID.set(1);
    }

    // ---------- Helpers ----------

    private PdIDTO convertirADTO(PdI p) {
        return new PdIDTO(
                p.getId() == null ? null : String.valueOf(p.getId()),
                p.getHechoId(),
                p.getDescripcion(),
                p.getLugar(),
                p.getMomento(),
                p.getContenido(),
                p.getImageUrl(),
                p.getAutoTags(),
                p.getOcrText(),
                p.getProcessingState(),
                p.getProcessedAt(),
                p.getLastError()
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
        p.setImageUrl(d.imageUrl());
        return p;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) return true;
        try {
            URI u = URI.create(url);
            String scheme = Optional.ofNullable(u.getScheme()).orElse("").toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            String path = Optional.ofNullable(u.getPath()).orElse("").toLowerCase(Locale.ROOT);
            return path.isEmpty() || path.endsWith(".jpg") || path.endsWith(".jpeg")
                    || path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".webp");
        } catch (Exception e) {
            return false;
        }
    }
}
