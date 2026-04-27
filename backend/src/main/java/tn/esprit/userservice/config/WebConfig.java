package tn.esprit.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
        String absolutePath = uploadPath.toFile().getAbsolutePath().replace("\\", "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + absolutePath + "/");
    }
}