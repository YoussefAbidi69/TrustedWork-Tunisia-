package com.trustedwork.module06.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private static final String API_EVENTS_PATH = "/api/events/**";
    private static final String API_CHALLENGES_PATH = "/api/challenges/**";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // EVENTS
                        .requestMatchers(HttpMethod.GET, "/api/events", API_EVENTS_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events", API_EVENTS_PATH).permitAll()
                        .requestMatchers(HttpMethod.PUT, API_EVENTS_PATH).permitAll()
                        .requestMatchers(HttpMethod.DELETE, API_EVENTS_PATH).permitAll()

                        // LEADERBOARD
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()

                        // CHALLENGES
                        .requestMatchers(HttpMethod.GET, "/api/challenges", "/api/challenges/admin").permitAll()
                        .requestMatchers(HttpMethod.POST, API_CHALLENGES_PATH).permitAll()
                        .requestMatchers(HttpMethod.DELETE, API_CHALLENGES_PATH).permitAll()

                        // ANALYTICS & ML
                        .requestMatchers(HttpMethod.GET, "/api/analytics/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList("*")); 
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}