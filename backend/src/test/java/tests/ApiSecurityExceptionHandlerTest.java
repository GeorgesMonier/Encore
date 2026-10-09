package tests;

import com.encore.encoreapi.security.ApiSecurityExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

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
        assertTrue(response.getContentAsString().contains("CSRF"));
    }
}
