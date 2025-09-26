// ar/edu/utn/dds/k3003/config/HttpAndAsyncConfig.java
package ar.edu.utn.dds.k3003.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpAndAsyncConfig {

    @Bean
    public RestTemplate restTemplate() {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(10000);
        return new RestTemplate(f);
    }
}
