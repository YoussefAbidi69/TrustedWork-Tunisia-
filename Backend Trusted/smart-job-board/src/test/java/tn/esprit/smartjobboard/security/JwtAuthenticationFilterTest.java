package tn.esprit.smartjobboard.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesThatIs256BitsLong!!";

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    private String buildValidToken(String email, String roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.signWith(key).compact();
    }

    @Test
    @DisplayName("should pass through when no Authorization header")
    void noAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should pass through when Authorization header is not Bearer")
    void nonBearerHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should authenticate user with valid token and set SecurityContext")
    void validBearerToken() throws Exception {
        String token = buildValidToken("dev@example.com", "FREELANCER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("dev@example.com");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_FREELANCER");
    }

    @Test
    @DisplayName("should handle multiple roles in comma-separated format")
    void multipleRoles() throws Exception {
        String token = buildValidToken("admin@example.com", "FREELANCER,ADMIN");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_FREELANCER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("should not duplicate ROLE_ prefix if already present")
    void alreadyPrefixed() throws Exception {
        String token = buildValidToken("dev@example.com", "ROLE_CLIENT");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    @DisplayName("should handle null roles claim → empty authorities")
    void nullRoles() throws Exception {
        String token = buildValidToken("dev@example.com", null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("should clear context and continue chain on invalid token")
    void invalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.jwt.token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should not overwrite existing authentication")
    void existingAuth() throws Exception {
        // Pre-set an authentication in the context
        var preAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing@user.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(preAuth);

        String token = buildValidToken("dev@example.com", "FREELANCER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        // Should NOT overwrite
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing@user.com");
    }
}
