package tn.esprit.freelancerprofileservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String REVIEWS_API = "/api/reviews/**";

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // WebSocket
                        .requestMatchers("/ws/**").permitAll()

                        // ML Service — accès public
                        .requestMatchers("/api/ml/**").permitAll()

                        // Profils — lecture publique
                        .requestMatchers(HttpMethod.GET, "/api/profiles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/{profileId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/ranking/**").permitAll()

                        // Reviews — endpoints réservés au propriétaire (JWT obligatoire)
                        .requestMatchers(HttpMethod.GET, "/api/reviews/profiles/*/all").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/hide").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/*/restore").authenticated()

                        // Reviews — lecture et écriture publiques
                        .requestMatchers(HttpMethod.GET, REVIEWS_API).permitAll()
                        .requestMatchers(HttpMethod.POST, REVIEWS_API).permitAll()
                        .requestMatchers(HttpMethod.PUT, REVIEWS_API).permitAll()

                        // Vues profil
                        .requestMatchers(HttpMethod.POST, "/api/views/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/views/profiles/**").permitAll()

                        // Notifications — authentifié
                        .requestMatchers("/api/notifications/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:4201"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}