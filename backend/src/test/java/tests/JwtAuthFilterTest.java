package tests;

import com.encore.encoreapi.security.JwtAuthFilter;
import com.encore.encoreapi.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private static final String JWT_SECRET = "clave-de-prueba-de-al-menos-32-caracteres-xx";
    private final JwtService jwtService = new JwtService(JWT_SECRET);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsingTheHttpOnlySessionCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@example.test", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/totp/setup");
        request.setCookies(new jakarta.servlet.http.Cookie("encore_session", token));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId.toString(), authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void doesNotTreatAuthorizationBearerAsAnAuthenticationMechanism() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.test", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/setup-intent");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
