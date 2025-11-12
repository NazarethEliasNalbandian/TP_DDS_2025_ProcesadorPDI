package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class PdiWorkerManual extends DefaultConsumer {

    private static final String QUEUE_NAME = "pdi.to.process";
    private final ObjectMapper mapper = new ObjectMapper();

    // ⚠️ Este lo vas a instanciar manualmente si querés
    private FachadaProcesadorPDI fachadaProcesadorPdI;

    public PdiWorkerManual(Channel channel) {
        super(channel);
        this.fachadaProcesadorPdI = new FachadaProcesadorPDI() {
            @Override
            public PdIDTO procesar(PdIDTO pdi) throws IllegalStateException {
                return null;
            }

            @Override
            public PdIDTO buscarPdIPorId(String pdiId) throws NoSuchElementException {
                return null;
            }

            @Override
            public List<PdIDTO> buscarPorHecho(String hechoId) {
                return List.of();
            }

            @Override
            public void setFachadaSolicitudes(FachadaSolicitudes fachadaSolicitudes) {

            }

            @Override
            public List<PdIDTO> pdis() {
                return List.of();
            }

            @Override
            public void borrarTodo() {

            }

            @Override
            public PdI guardarPendiente(PdI pdi) {
                return null;
            }
        }; // o null si solo querés probar conexión
    }

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
            PdIDTO dto = mapper.readValue(message, PdIDTO.class);

            // ⚙️ Procesamiento real
            if (fachadaProcesadorPdI != null) {
                fachadaProcesadorPdI.procesar(dto);
            }

            getChannel().basicAck(envelope.getDeliveryTag(), false);
            log.info("✅ [Worker Manual] PdI procesado correctamente (hechoId={})", dto.hechoId());

        } catch (Exception e) {
            log.error("❌ [Worker Manual] Error procesando PdI: {}", e.getMessage(), e);
            getChannel().basicNack(envelope.getDeliveryTag(), false, true);
        }
    }

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
            } catch (InterruptedException ignored) {}
        }
    }
}
