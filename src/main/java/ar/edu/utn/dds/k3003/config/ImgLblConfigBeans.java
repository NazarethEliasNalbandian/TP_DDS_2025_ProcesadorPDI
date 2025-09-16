package ar.edu.utn.dds.k3003.config;

import ar.edu.utn.dds.k3003.services.tagging.ImageLabelProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// src/main/java/ar/edu/utn/dds/k3003/config/ImgLblConfigBeans.java
@Configuration
public class ImgLblConfigBeans {
    @Bean
    public ImageLabelProvider.ImgLblConfig imgLblCfg(
            @Value("${imglbl.base-url}") String baseUrl,
            @Value("${imglbl.apikey:}") String key,
            @Value("${features.imglbl.enabled:true}") boolean enabled) {
        return new ImageLabelProvider.ImgLblConfig(baseUrl, key, enabled);
    }
}
