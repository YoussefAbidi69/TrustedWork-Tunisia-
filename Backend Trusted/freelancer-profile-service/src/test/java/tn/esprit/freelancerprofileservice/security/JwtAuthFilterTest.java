package tn.esprit.freelancerprofileservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── shouldNotFilter ────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] method={0} uri={1} → shouldNotFilter={2}")
    @CsvSource({
        "OPTIONS, /api/skills/1,          true",
        "GET,     /api/ml/predict,         true",
        "GET,     /swagger-ui/index.html,  true",
        "GET,     /api/profiles/1,         false"
    })
    void shouldNotFilter_variousPaths(String method, String uri, boolean expected) {
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri.trim());

        boolean result = jwtAuthFilter.shouldNotFilter(request);

        assertThat(result).isEqualTo(expected);
    }

    // ─── doFilterInternal ───────────────────────────────────────────────────

    @Test
    void doFilterInternal_shouldPassThrough_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenAuthHeaderNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtUtil.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("user@test.com");
        when(jwtUtil.extractRole("valid.token.here")).thenReturn("FREELANCER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@test.com");
    }

    @Test
    void doFilterInternal_shouldClearContext_whenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(jwtUtil.isTokenValid("bad.token")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldClearContext_whenJwtUtilThrows() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.token");
        when(jwtUtil.isTokenValid("broken.token"))
                .thenThrow(new RuntimeException("parse error"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
