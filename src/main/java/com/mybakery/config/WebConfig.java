package com.mybakery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

/**
 * Web MVC configuration.
 * Registers view controllers and static resource handlers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/admin", "/admin/products");
        registry.addRedirectViewController("/admin/", "/admin/products");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Paths.get("uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/product-images/**")
                .addResourceLocations(uploadLocation);
    }
}
