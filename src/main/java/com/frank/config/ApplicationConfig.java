package com.frank.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableCaching
public class ApplicationConfig {

    @Bean
    RestTemplate soapRestTemplate(RestTemplateBuilder builder,
                                  SoapClientProperties soapClientProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(soapClientProperties.getConnectTimeoutMs());
        factory.setReadTimeout(soapClientProperties.getReadTimeoutMs());

        return builder
                .requestFactory(() -> factory)
                .setConnectTimeout(Duration.ofMillis(soapClientProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(soapClientProperties.getReadTimeoutMs()))
                .build();
    }
}
