package com.school.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig {

    /** Angular (localhost:4200) veya baska porttan gelen isteklere izin */
    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = allowedOrigins == null || allowedOrigins.isBlank()
                        ? new String[]{"http://localhost:4200"}
                        : allowedOrigins.split("\\s*,\\s*");

                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}