package com.pdfsign.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origins = appProperties.getCors().getAllowedOrigins();
        if ("*".equals(origins)) {
            registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(false);
        } else {
            registry.addMapping("/api/**")
                    .allowedOrigins(origins.split(","))
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    }
}
