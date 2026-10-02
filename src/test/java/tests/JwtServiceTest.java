package tests;

import com.encore.encoreapi.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("clave-de-prueba-de-al-menos-32-caracteres-xx");

    @Test
    void generatedTokenIsValidAndCarriesUserIdAndRole() {
        UUID id = UUID.randomUUID();

        String token = jwtService.generateToken(id, "a@test.com", "ADMIN");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(id.toString(), jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(UUID.randomUUID(), "a@test.com", "USER");

        assertFalse(jwtService.isTokenValid(token + "x"));
    }
}