package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdiWorker {

    private final FachadaProcesadorPDI fachadaProcesadorPdI;

    public PdiWorker(FachadaProcesadorPDI fachadaProcesadorPdI) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
    }

    /**
     * 🎯 Escucha la cola y delega directamente al método procesar() de la fachada.
     * El mensaje debe contener los datos del PdI serializados (JSON).
     */
    @RabbitListener(queues = "${queue.name}")
    public void handleMessage(String body) {
        try {
            log.info("📥 [Worker] Recibido mensaje: {}", body);

            // 🔸 Deserializar el mensaje al DTO
            // (Suponiendo que el mensaje que mandás al Rabbit es un JSON con los campos del PdI)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            PdIDTO entrada = mapper.readValue(body, PdIDTO.class);

            // 🔸 Llamar al méto.do procesar() original
            var resultado = fachadaProcesadorPdI.procesar(entrada);

            log.info("✅ [Worker] PdI procesado correctamente: id={}, estado={}",
                    resultado.id(), resultado.processingState());

        } catch (Exception e) {
            log.error("❌ [Worker] Error procesando mensaje '{}': {}", body, e.getMessage(), e);
        }
    }
}
