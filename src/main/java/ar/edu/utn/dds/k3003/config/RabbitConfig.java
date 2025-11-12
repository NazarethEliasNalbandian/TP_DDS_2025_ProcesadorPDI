package ar.edu.utn.dds.k3003.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("pdi.direct");
    }

    @Bean
    public Queue pdiQueue() {
        return new Queue("pdi.to.process", true);
    }

    @Bean
    public Binding binding(Queue pdiQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(pdiQueue).to(directExchange).with("pdi.process");
    }
}
