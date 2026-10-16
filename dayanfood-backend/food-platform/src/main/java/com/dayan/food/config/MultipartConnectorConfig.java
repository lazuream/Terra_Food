package com.dayan.food.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConnectorConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> multipartConnectorLimits() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(6 * 1024 * 1024);
            connector.setProperty("maxPartCount", "4");
            connector.setProperty("maxPartHeaderSize", "2048");
            connector.setProperty("maxSwallowSize", Integer.toString(6 * 1024 * 1024));
        });
    }
}
