package tn.esprit.freelancerprofileservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre JWT — intercepte chaque requête et valide le token
 * émis par le user-service (Module 01)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    // URLs qui ne nécessitent pas de JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/ml/",
            "/api/reviews/profiles/",
            "/api/views/profiles/",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/ws/"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Laisser passer OPTIONS (preflight CORS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Les endpoints "propriétaire uniquement" nécessitent toujours le JWT,
        // même s'ils commencent par un chemin public.
        if (path.endsWith("/all") || path.contains("/hide") || path.contains("/restore")) {
            return false;
        }

        // Laisser passer tous les endpoints publics
        return PUBLIC_PATHS.stream().anyMatch(path::contains);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Pas de token — on continue sans authentification
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                log.debug(">>> JWT OK — user: {} role: {}", username, role);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn(">>> JWT INVALID — token rejected");
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            log.error(">>> JWT EXCEPTION — {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}