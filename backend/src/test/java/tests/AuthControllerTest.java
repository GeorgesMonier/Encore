package tests;

import com.encore.encoreapi.security.AuthController;
import com.encore.encoreapi.security.JwtService;
import com.encore.encoreapi.user.LoginRequest;
import com.encore.encoreapi.user.LoginResponse;
import com.encore.encoreapi.user.UserRepository;
import com.encore.encoreapi.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;

    @Test
    void loginSetsHttpOnlyCookieAndDoesNotReturnTheToken() {
        when(userService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class)))
                .thenReturn(new LoginResponse("secret-access-token", "buyer@example.test", "Buyer"));
        when(jwtService.getExpiration()).thenReturn(Duration.ofHours(24));

        AuthController controller = new AuthController(userService, userRepository, jwtService, true, "Lax");
        ResponseEntity<?> response = controller.login(new LoginRequest());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertNotNull(cookie);
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api"));
        assertFalse(response.getBody().toString().contains("secret-access-token"));
        assertFalse(((Map<?, ?>) response.getBody()).containsKey("token"));
    }
}
