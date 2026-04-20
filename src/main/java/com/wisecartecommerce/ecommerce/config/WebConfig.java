package com.wisecartecommerce.ecommerce.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle product images from uploads-dev
        String uploadLocation = "file:" + uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .resourceChain(true);

        // Specifically handle products folder for cleaner URLs
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations("file:" + uploadDir + "/products/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        // Swagger handlers (keep these)
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }
}
