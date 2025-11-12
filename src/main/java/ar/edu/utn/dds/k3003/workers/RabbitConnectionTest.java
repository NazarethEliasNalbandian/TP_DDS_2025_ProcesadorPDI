package ar.edu.utn.dds.k3003.workers;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitConnectionTest {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri("amqps://pilwyxdw:G8EYYMZ_f_GU1pTzQZO9pWXl7_IPYFX0@beaver.rmq.cloudamqp.com/pilwyxdw");
            factory.useSslProtocol("TLSv1.2");

            Connection conn = factory.newConnection();
            System.out.println("✅ Conectado a CloudAMQP correctamente!");

            conn.close();
        } catch (Exception e) {
            System.err.println("❌ Error conectando a CloudAMQP:");
            e.printStackTrace();
        }
    }
}
