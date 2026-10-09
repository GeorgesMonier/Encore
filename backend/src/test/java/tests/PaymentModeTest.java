package tests;

import com.encore.encoreapi.payment.PaymentMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentModeTest {

    @Test
    void enablesStripeAutomaticallyWhenASecretKeyIsConfigured() {
        assertFalse(new PaymentMode("", "sk_test_example").isDemoMode());
    }

    @Test
    void remainsInDemoModeWhenNoStripeSecretKeyIsConfigured() {
        assertTrue(new PaymentMode("", "").isDemoMode());
    }

    @Test
    void explicitDemoSettingOverridesConfiguredStripeKey() {
        assertTrue(new PaymentMode("true", "sk_test_example").isDemoMode());
    }

    @Test
    void rejectsInvalidModeSetting() {
        assertThrows(IllegalStateException.class, () -> new PaymentMode("yes", "sk_test_example").isDemoMode());
    }
}
