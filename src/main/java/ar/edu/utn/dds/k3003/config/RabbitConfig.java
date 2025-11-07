package ar.edu.utn.dds.k3003.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue pdiQueue() {
        return new Queue("pdi.to.process", true);
    }

    @Bean
    public DirectExchange pdiExchange() {
        return new DirectExchange("pdi.direct");
    }

    @Bean
    public Binding pdiBinding(Queue pdiQueue, DirectExchange pdiExchange) {
        return BindingBuilder.bind(pdiQueue).to(pdiExchange).with("pdi.process");
    }
}
