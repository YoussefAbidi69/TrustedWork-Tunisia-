package tn.esprit.msprojectservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.msprojectservice.feign.UserServiceClient;
import tn.esprit.msprojectservice.feign.dto.TokenValidationDTO;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

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

                String roles = jwtService.extractRoles(token);

                // Appel User Service pour récupérer le userId (Long)
                // Le JWT contient le cin (Integer) mais PAS le userId (Long)
                TokenValidationDTO userInfo = userServiceClient.validateToken(authHeader);

                if (userInfo != null && userInfo.isValid()) {

                    List<SimpleGrantedAuthority> authorities;
                    if (roles != null && !roles.isBlank()) {
                        authorities = Arrays.stream(roles.split(","))
                                .map(String::trim)
                                .filter(r -> !r.isEmpty())
                                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                                .map(SimpleGrantedAuthority::new)
                                .toList();
                    } else {
                        authorities = Collections.emptyList();
                    }

                    AuthenticatedUser principal = new AuthenticatedUser(
                            userInfo.getUserId(),
                            userInfo.getCin(),
                            userInfo.getEmail(),
                            userInfo.getRole()
                    );

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("✅ Authentifié : email={}, userId={}", userInfo.getEmail(), userInfo.getUserId());
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}