package tn.esprit.mscontractservicee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                Long cin = jwtService.extractCinAsLong(token);
                String roles = jwtService.extractRoles(token);

                List<SimpleGrantedAuthority> authorities;
                if (roles != null && !roles.isBlank()) {
                    authorities = Arrays.stream(roles.split(","))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                } else {
                    authorities = Collections.emptyList();
                }

                // Requirement: use CIN as the authenticated principal (instead of DB id).
                String principal = cin != null ? cin.toString() : "";

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authToken);

                if (log.isDebugEnabled()) {
                    String email = jwtService.extractEmail(token);
                    log.debug("Authenticated user email={} cin={} authorities={}", email, cin, authorities);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

