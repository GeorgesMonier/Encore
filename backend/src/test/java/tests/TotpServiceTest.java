package tests;

import com.encore.encoreapi.security.TotpService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    @Test
    void acceptsCurrentCodeAndAdjacentTimeStep() throws Exception {
        TotpService service = new TotpService();
        String secret = service.generateSecret();
        SystemTimeProvider timeProvider = new SystemTimeProvider();
        long currentStep = timeProvider.getTime() / 30;
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();

        assertTrue(service.verifyCode(secret, codeGenerator.generate(secret, currentStep)));
        assertTrue(service.verifyCode(secret, codeGenerator.generate(secret, currentStep - 1)));
    }

    @Test
    void rejectsCodesThatAreNotExactlySixDigits() {
        TotpService service = new TotpService();

        assertFalse(service.verifyCode(service.generateSecret(), "12345"));
        assertFalse(service.verifyCode(service.generateSecret(), "12345x"));
    }
}
