package tests;

import com.encore.encoreapi.payment.StripeCurrency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeCurrencyTest {

    @Test
    void usesCurrencyFractionDigitsForStripeAmounts() {
        assertEquals(1234L, StripeCurrency.toMinorUnits(new BigDecimal("12.34"), "EUR"));
        assertEquals(1234L, StripeCurrency.toMinorUnits(new BigDecimal("12.34"), "USD"));
        assertEquals(123L, StripeCurrency.toMinorUnits(new BigDecimal("123"), "JPY"));
        assertEquals(123400L, StripeCurrency.toMinorUnits(new BigDecimal("1234"), "ISK"));
    }

    @Test
    void rejectsInvalidOrFractionalZeroDecimalAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> StripeCurrency.toMinorUnits(new BigDecimal("10.25"), "JPY"));
        assertThrows(IllegalArgumentException.class,
                () -> StripeCurrency.toMinorUnits(new BigDecimal("10.00"), "not-a-currency"));
    }
}
