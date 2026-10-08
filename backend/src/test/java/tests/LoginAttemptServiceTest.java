package tests;

import com.encore.encoreapi.security.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterFiveFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.loginFailed("a@test.com");
        }

        assertTrue(service.isBlocked("a@test.com"));
    }

    @Test
    void doesNotBlockBeforeLimit() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.loginFailed("a@test.com");
        }

        assertFalse(service.isBlocked("a@test.com"));
    }

    @Test
    void successfulLoginResetsCounter() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.loginFailed("a@test.com");
        }
        service.loginSucceeded("a@test.com");
        service.loginFailed("a@test.com");

        assertFalse(service.isBlocked("a@test.com"));
    }
}