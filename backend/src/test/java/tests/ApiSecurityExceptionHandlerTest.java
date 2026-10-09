package tests;

import com.encore.encoreapi.security.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class ApiSecurityExceptionHandlerTest {

    private final ApiSecurityExceptionHandler handler =
            new ApiSecurityExceptionHandler(new ObjectMapper());

    @Test
    void reportsMissingAuthenticationAs401WithoutEchoingCookieData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/setup-intent");
        request.setCookies(new jakarta.servlet.http.Cookie("encore_session", "sensitive-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new InsufficientAuthenticationException("not authenticated"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Inicia sesión"));
        assertFalse(response.getContentAsString().contains("sensitive-token"));
    }

    @Test
    void preserves403ForDeniedRequestsAndCsrfFailures() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/totp/setup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new MissingCsrfTokenException("missing"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Falta el token CSRF"));
    }

    @Test
    void identifiesInvalidCsrfSeparatelyFromAuthorizationDenials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/setup-intent");
        MockHttpServletResponse csrfResponse = new MockHttpServletResponse();
        MockHttpServletResponse accessResponse = new MockHttpServletResponse();

        handler.handle(request, csrfResponse,
                new InvalidCsrfTokenException(new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "expected"), "actual"));
        handler.handle(request, accessResponse, new AccessDeniedException("denied"));

        assertEquals(403, csrfResponse.getStatus());
        assertTrue(csrfResponse.getContentAsString().contains("token CSRF no es válido"));
        assertEquals(403, accessResponse.getStatus());
        assertTrue(accessResponse.getContentAsString().contains("No tienes permiso"));
    }
}
