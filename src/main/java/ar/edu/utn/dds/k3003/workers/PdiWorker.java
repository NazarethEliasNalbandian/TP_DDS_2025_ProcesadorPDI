package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import ar.edu.utn.dds.k3003.services.tagging.TagAggregatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class PdiWorker {

    @Autowired
    private PdIRepository pdiRepository;

    @Autowired
    private TagAggregatorService tagAggregatorService;

    /**
     * Consume mensajes de la cola 'pdi.to.process'.
     * Cada mensaje contiene el ID del PdI a procesar.
     */
    @RabbitListener(queues = "pdi.to.process")
    public void handleMessage(Long pdiId) {
        log.info("[Worker] 🔄 Procesando PdI id={} ...", pdiId);

        PdI pdi = pdiRepository.findById(pdiId).orElse(null);
        if (pdi == null) {
            log.error("[Worker] ❌ PdI id={} no encontrado en base", pdiId);
            return;
        }

        try {
            // 🔸 1️⃣ Marcar como 'PROCESSING'
            pdi.setProcessingState(PdI.ProcessingState.PROCESSING);
            pdiRepository.save(pdi);

            // 🔸 2️⃣ Ejecutar todos los TagProviders registrados
            tagAggregatorService.processImageTags(pdiId);

            // 🔸 3️⃣ Marcar como 'PROCESSED'
            pdi.setProcessingState(PdI.ProcessingState.PROCESSED);
            pdi.setProcessedAt(LocalDateTime.now());
            pdiRepository.save(pdi);

            log.info("[Worker] ✅ PdI {} procesado correctamente.", pdiId);

        } catch (Exception e) {
            log.error("[Worker] ❌ Error procesando PdI {}: {}", pdiId, e.getMessage(), e);
            pdi.setProcessingState(PdI.ProcessingState.ERROR);
            pdi.setLastError(e.getMessage());
            pdiRepository.save(pdi);
        }
    }
}
