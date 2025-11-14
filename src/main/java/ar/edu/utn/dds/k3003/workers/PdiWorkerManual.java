package ar.edu.utn.dds.k3003.workers;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.nio.charset.StandardCharsets;

public class PdiWorkerManual {

    private static final String QUEUE_NAME = "pdi.queue";

    public static void main(String[] args) throws Exception {

        // 1️⃣ Levantar Spring para obtener la fachada real
        ApplicationContext ctx =
                new SpringApplicationBuilder(ar.edu.utn.dds.k3003.Application.class)
                        .run();

        FachadaProcesadorPDI fachada = ctx.getBean(FachadaProcesadorPDI.class);

        // Mapper para JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // 2️⃣ Conectar a RabbitMQ (CLOUDAMQP_URL)
        String rabbitUri = System.getenv("CLOUDAMQP_URL");
        if (rabbitUri == null) {
            System.err.println("❌ ERROR: Falta variable CLOUDAMQP_URL");
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitUri);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        System.out.println("🚀 Worker Manual conectado a RabbitMQ");
        System.out.println("📡 Cola: " + QUEUE_NAME);

        // 3️⃣ Consumir mensajes
        DeliverCallback callback = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("📥 Mensaje recibido: " + body);

            try {
                PdIDTO dto = mapper.readValue(body, PdIDTO.class);
                var result = fachada.procesar(dto);
                System.out.println("✅ PdI procesado → " + result.id());
            } catch (Exception e) {
                System.err.println("❌ Error procesando: " + e.getMessage());
            }
        };

        channel.basicConsume(QUEUE_NAME, true, callback, consumerTag -> {});
    }
}
