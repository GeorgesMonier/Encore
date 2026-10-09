package tests;

import com.encore.encoreapi.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigCookiePolicyTest {

    @Test
    void refusesCrossSiteCookiesWithoutSecureTransport() {
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityConfig(null, null, null, "https://frontend.example.test", false, "None"));
    }
}
