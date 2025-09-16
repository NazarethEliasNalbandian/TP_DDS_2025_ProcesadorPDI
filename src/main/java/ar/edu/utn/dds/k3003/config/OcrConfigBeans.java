package ar.edu.utn.dds.k3003.config;


import ar.edu.utn.dds.k3003.services.tagging.OcrTagProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfigBeans {
    @Bean
    public OcrTagProvider.OcrConfig ocrCfg(
            @Value("${ocr.base-url}") String baseUrl,
            @Value("${ocr.apikey:}") String key,
            @Value("${features.ocr.enabled:true}") boolean enabled) {
        return new OcrTagProvider.OcrConfig(baseUrl, key, enabled);
    }
}

