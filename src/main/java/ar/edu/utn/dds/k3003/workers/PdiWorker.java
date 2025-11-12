package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdiWorker {

    private final FachadaProcesadorPDI fachadaProcesadorPdI;
    private final ObjectMapper mapper;

    public PdiWorker(FachadaProcesadorPDI fachadaProcesadorPdI) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule()); // ✅ soporte para LocalDateTime
    }

    /**
     * 🎯 Escucha la cola y delega directamente al método procesar() de la fachada.
     * Cada mensaje debe ser un JSON con los datos del PdI.
     */
    @RabbitListener(queues = "${queue.name}")
    public void handleMessage(String body) {
        log.info("📥 [Worker] Mensaje recibido desde la cola: {}", body);

        try {
            // 🔸 Deserializar JSON a PdIDTO
            PdIDTO entrada = mapper.readValue(body, PdIDTO.class);

            // 🔸 Llamar al método procesar() original de la fachada
            var resultado = fachadaProcesadorPdI.procesar(entrada);

            log.info("✅ [Worker] PdI procesado correctamente → id={}, estado={}",
                    resultado.id(), resultado.processingState());

        } catch (Exception e) {
            log.error("❌ [Worker] Error procesando mensaje: {}", e.getMessage(), e);
        }
    }
}
