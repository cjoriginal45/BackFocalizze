package com.focalizze.Focalizze.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Configures static resource handling for uploaded images.
 * <p>
 * Configuración Web MVC.
 * Configura el manejo de recursos estáticos para imágenes subidas.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Maps the "/images/**" URL path to the physical file system location.
     * <p>
     * Mapea la ruta URL "/images/**" a la ubicación física del sistema de archivos.
     *
     * @param registry The ResourceHandlerRegistry. / El ResourceHandlerRegistry.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Maps URL path to file system path / Mapea ruta URL a ruta de sistema de archivos
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/")
                .addResourceLocations("classpath:/static/images/");
    }
}
