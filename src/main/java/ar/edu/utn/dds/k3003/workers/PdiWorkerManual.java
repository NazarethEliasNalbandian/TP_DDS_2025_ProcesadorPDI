package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdiWorkerManual {

    private final FachadaProcesadorPDI fachadaProcesadorPdI;
    private final ObjectMapper mapper;

    public PdiWorkerManual(FachadaProcesadorPDI fachadaProcesadorPdI) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @RabbitListener(queues = "${queue.name}")
    public void handleMessage(String body) {
        try {
            PdIDTO dto = mapper.readValue(body, PdIDTO.class);
            var result = fachadaProcesadorPdI.procesar(dto);
            log.info("✅ [Worker] Procesado PdI {} → estado={}", result.id(), result.processingState());
        } catch (Exception e) {
            log.error("❌ [Worker] Error procesando mensaje: {}", e.getMessage(), e);
        }
    }
}
