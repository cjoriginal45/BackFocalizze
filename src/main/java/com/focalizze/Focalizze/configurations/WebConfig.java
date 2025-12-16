package com.focalizze.Focalizze.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapea la URL "/images/**" a la carpeta física en el disco
        // "file:" indica que es una ruta del sistema de archivos

        // Ajustamos la ruta para que apunte a la raíz de uploads, no solo avatars si tienes subcarpetas
        // Si tus imagenes de hilos están en ./uploads/, usa esto:
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/")
                .addResourceLocations("classpath:/static/images/");
    }
}
