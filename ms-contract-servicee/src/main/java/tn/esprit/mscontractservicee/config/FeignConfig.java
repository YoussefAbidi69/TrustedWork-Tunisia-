package tn.esprit.mscontractservicee.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignConfig {

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                requestTemplate.header("X-Internal-Api-Key", internalApiKey);
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");
                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}
