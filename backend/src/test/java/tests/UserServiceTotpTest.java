package tests;

import com.encore.encoreapi.security.JwtService;
import com.encore.encoreapi.security.LoginAttemptService;
import com.encore.encoreapi.security.TotpService;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import com.encore.encoreapi.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTotpTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TotpService totpService;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks
    private UserService userService;

    @Test
    void persistsTotpSecretAndReturnsQrImageForSetup() {
        UUID userId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpService.generateSecret()).thenReturn("secret");
        when(totpService.generateQrCodeImage("secret", user.getEmail())).thenReturn("base64image");

        var setup = userService.setupTotp(userId);

        assertEquals("secret", user.getTotpSecret());
        assertEquals("secret", setup.getSecret());
        assertEquals("base64image", setup.getQrCodeImage());
        verify(userRepository).save(user);
    }

    @Test
    void enablesTotpOnlyAfterValidatingTheSubmittedCode() {
        UUID userId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setTotpSecret("secret");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpService.verifyCode("secret", "123456")).thenReturn(true);

        userService.confirmTotp(userId, "123456");

        assertTrue(user.isTotpEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void doesNotEnableTotpForAnInvalidCode() {
        UUID userId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setTotpSecret("secret");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpService.verifyCode("secret", "000000")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.confirmTotp(userId, "000000"));

        assertFalse(user.isTotpEnabled());
        verify(userRepository, never()).save(user);
    }
}
