package ar.edu.utn.dds.k3003.workers;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class PdiWorkerManual extends DefaultConsumer {

    private static final String QUEUE_NAME = "pdi.to.process";

    public PdiWorkerManual(Channel channel) {
        super(channel);
    }

    private void init() throws IOException {
        Channel channel = getChannel();

        // 🔹 Declaramos la cola desde la cual consumir mensajes
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        // 🔹 Empezamos a consumir mensajes
        channel.basicConsume(QUEUE_NAME, false, this);
        log.info("[Worker] 🟢 Esperando mensajes en la cola '{}'", QUEUE_NAME);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {

        String message = new String(body, StandardCharsets.UTF_8);
        log.info("📥 [Worker] Mensaje recibido: {}", message);

        // Confirmamos que se procesó correctamente
        getChannel().basicAck(envelope.getDeliveryTag(), false);
    }

    public static void main(String[] args) throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        log.info("🚀 Iniciando PdiWorkerManual (modo test conexión)...");

        // 🧩 URI de CloudAMQP (usá la tuya)
        String uri = "amqps://pilwyxdw:G8EYYMZ_f_GU1pTzQZO9pWXl7_IPYFX0@beaver.rmq.cloudamqp.com/pilwyxdw";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(uri);
        factory.useSslProtocol();
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        PdiWorkerManual worker = new PdiWorkerManual(channel);
        worker.init();

        log.info("✅ Worker conectado a CloudAMQP y escuchando mensajes...");
    }
}
