package ar.edu.utn.dds.k3003.config;

import ar.edu.utn.dds.k3003.clients.FuentesProxy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientsConfig {

    @Bean
    public FuentesProxy fuentesProxy(ObjectMapper mapper) {
        return new FuentesProxy(mapper);
    }
}
