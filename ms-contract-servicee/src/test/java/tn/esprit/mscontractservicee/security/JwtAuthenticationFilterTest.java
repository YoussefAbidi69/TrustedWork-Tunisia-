package tn.esprit.mscontractservicee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_NoAuthorizationHeader_Delegates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_NonBearerHeader_Delegates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_AlreadyAuthenticated_DelegatesWithoutJwtCalls() throws Exception {
        Authentication existing = new UsernamePasswordAuthenticationToken("123", null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidToken_DelegatesAndDoesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid("bad");
        verify(jwtService, never()).extractCinAsLong(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_ValidToken_SetsAuthenticationWithCinAndRoles() throws Exception {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        ch.qos.logback.classic.Level prev = logger.getLevel();
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        when(request.getHeader("Authorization")).thenReturn("Bearer good");
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractCinAsLong("good")).thenReturn(123L);
        when(jwtService.extractRoles("good")).thenReturn("CLIENT, FREELANCER,ROLE_ADMIN");
        when(jwtService.extractEmail("good")).thenReturn("u@test.com");

        try {
            filter.doFilterInternal(request, response, filterChain);
        } finally {
            logger.setLevel(prev);
        }

        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractEmail("good");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("123", auth.getName());

        String authorities = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .sorted()
                .collect(Collectors.joining(","));
        assertEquals("ROLE_ADMIN,ROLE_CLIENT,ROLE_FREELANCER", authorities);
    }

    @Test
    void testDoFilterInternal_ValidToken_NullCinAndBlankRoles() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good");
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractCinAsLong("good")).thenReturn(null);
        when(jwtService.extractRoles("good")).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("", auth.getName());
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test
    void testDoFilterInternal_JwtThrows_ClearsContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good");
        when(jwtService.isTokenValid("good")).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
