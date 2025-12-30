package com.barbearia.barbearia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Fix for Windows: Convert relative path to absolute URI
        String path = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        
        if (!path.endsWith("/")) {
            path += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path);
    }
}
