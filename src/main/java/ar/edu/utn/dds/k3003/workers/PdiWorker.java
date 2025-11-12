package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import ar.edu.utn.dds.k3003.services.tagging.TagAggregatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
public class PdiWorker {

    private final PdIRepository pdiRepository;
    private final TagAggregatorService tagAggregatorService;

    public PdiWorker(PdIRepository pdiRepository, TagAggregatorService tagAggregatorService) {
        this.pdiRepository = pdiRepository;
        this.tagAggregatorService = tagAggregatorService;
    }

    /**
     * 🎯 Cada worker escucha la misma cola y RabbitMQ reparte los mensajes.
     * Un PdI se procesa exactamente una vez.
     */
    @RabbitListener(queues = "${queue.name}")
    public void handleMessage(String body) {
        try {
            Long pdiId = Long.parseLong(body);
            log.info("📥 [Worker] Recibido PdI id={}", pdiId);

            PdI pdi = pdiRepository.findById(pdiId).orElse(null);
            if (pdi == null) {
                log.warn("⚠️ PdI id={} no encontrado en la base", pdiId);
                return;
            }

            // 🔸 Procesamiento
            pdi.setProcessingState(PdI.ProcessingState.PROCESSING);
            pdiRepository.save(pdi);

            tagAggregatorService.processImageTags(pdiId);

            pdi.setProcessingState(PdI.ProcessingState.PROCESSED);
            pdi.setProcessedAt(LocalDateTime.now());
            pdiRepository.save(pdi);

            log.info("✅ PdI id={} procesado correctamente.", pdiId);

        } catch (Exception e) {
            log.error("❌ Error procesando mensaje '{}': {}", body, e.getMessage(), e);
        }
    }
}
