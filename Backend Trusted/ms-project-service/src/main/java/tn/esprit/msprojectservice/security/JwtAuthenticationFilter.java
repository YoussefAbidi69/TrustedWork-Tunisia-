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

    // ✅ Fix Cognitive Complexity — doFilterInternal allégée,
    //    chaque responsabilité déléguée à une méthode privée nommée
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            authenticateRequest(request, token);
        }

        filterChain.doFilter(request, response);
    }

    // ============================================================
    // Extraction du token depuis le header Authorization
    // ============================================================
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // ============================================================
    // Authentification complète — appelée seulement si token présent
    // ✅ try/catch isolé ici, pas imbriqué dans doFilterInternal
    // ============================================================
    private void authenticateRequest(HttpServletRequest request, String token) {
        try {
            if (isAuthenticationRequired(token)) {
                String authHeader = "Bearer " + token;
                TokenValidationDTO userInfo = userServiceClient.validateToken(authHeader);
                applyAuthentication(request, token, userInfo);
            }
        } catch (Exception e) {
            log.error("Erreur JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    // ============================================================
    // Vérifie si le token est valide ET qu'aucune auth n'est déjà active
    // ============================================================
    private boolean isAuthenticationRequired(String token) {
        return jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null;
    }

    // ============================================================
    // Applique l'authentification dans le SecurityContext
    // ============================================================
    private void applyAuthentication(HttpServletRequest request, String token, TokenValidationDTO userInfo) {
        if (userInfo == null || !userInfo.isValid()) {
            return;
        }

        List<SimpleGrantedAuthority> authorities = buildAuthorities(jwtService.extractRoles(token));

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
        log.debug("Authentifié : email={}, userId={}", userInfo.getEmail(), userInfo.getUserId());
    }

    // ============================================================
    // Construction de la liste des autorités depuis les rôles JWT
    // ============================================================
    private List<SimpleGrantedAuthority> buildAuthorities(String roles) {
        if (roles == null || roles.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}