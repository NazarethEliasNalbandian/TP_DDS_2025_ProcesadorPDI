package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class PdiWorkerManual extends DefaultConsumer {

    private static final String QUEUE_NAME = "pdi.to.process";
    private final ObjectMapper mapper = new ObjectMapper();

    // 👇 Se inyecta la fachada para procesar los PdIs
    @Autowired
    private FachadaProcesadorPDI fachadaProcesadorPdI;

    public PdiWorkerManual(Channel channel) {
        super(channel);
    }

    /**
     * Inicializa el worker: declara la cola y empieza a consumir mensajes.
     */
    private void init() throws IOException {
        Channel channel = getChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.basicConsume(QUEUE_NAME, false, this);
        log.info("🟢 [Worker Manual] Escuchando mensajes en la cola '{}'", QUEUE_NAME);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {

        String message = new String(body, StandardCharsets.UTF_8);
        log.info("📥 [Worker Manual] Mensaje recibido: {}", message);

        try {
            // 🔹 Deserializar el JSON recibido
            PdIDTO dto = mapper.readValue(message, PdIDTO.class);

            // 🔹 Procesar usando la fachada real
            fachadaProcesadorPdI.procesar(dto);

            log.info("✅ [Worker Manual] PdI procesado correctamente (hechoId={})", dto.hechoId());

            // 🔸 Confirmar que el mensaje fue procesado
            getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("❌ [Worker Manual] Error procesando PdI: {}", e.getMessage(), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, true); // requeue=true para reintentar
        }
    }

    /**
     * 🚀 Punto de entrada — ejecutá esta clase como aplicación independiente.
     */
    public static void main(String[] args)
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        log.info("🚀 Iniciando PdiWorkerManual (modo conexión directa a CloudAMQP)...");

        String uri = "amqps://pilwyxdw:G8EYYMZ_f_GU1pTzQZO9pWXl7_IPYFX0@beaver.rmq.cloudamqp.com/pilwyxdw";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(uri);
        factory.useSslProtocol("TLSv1.2");
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        PdiWorkerManual worker = new PdiWorkerManual(channel);
        worker.init();

        log.info("✅ [Worker Manual] Conectado a CloudAMQP y esperando mensajes...");

        synchronized (PdiWorkerManual.class) {
            try {
                PdiWorkerManual.class.wait();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
