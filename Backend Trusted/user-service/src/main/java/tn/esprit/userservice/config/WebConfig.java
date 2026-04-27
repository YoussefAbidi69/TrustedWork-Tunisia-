package tn.esprit.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Static /uploads/** serving has been removed — profile photos are now
 * stored on Cloudinary and served via their CDN URL.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}