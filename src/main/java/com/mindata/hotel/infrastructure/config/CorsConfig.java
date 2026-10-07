package com.mindata.hotel.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite llamar a la API desde un browser en otro origen (p. ej. "Try it out" de Swagger UI). */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AppProperties.Cors cors;

    public CorsConfig(AppProperties properties) {
        this.cors = properties.cors();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(cors.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type");
    }
}
