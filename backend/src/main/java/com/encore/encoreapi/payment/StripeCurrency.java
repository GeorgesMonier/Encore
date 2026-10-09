package com.encore.encoreapi.payment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;
import java.util.Locale;

public final class StripeCurrency {

    private static final Set<String> TWO_DECIMAL_ZERO_UNIT_STRIPE_CURRENCIES = Set.of("ISK", "UGX");

    private StripeCurrency() {}

    public static long toMinorUnits(BigDecimal amount, String currencyCode) {
        int fractionDigits;
        try {
            String normalizedCurrency = currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
            fractionDigits = TWO_DECIMAL_ZERO_UNIT_STRIPE_CURRENCIES.contains(normalizedCurrency)
                    ? 2 : Currency.getInstance(normalizedCurrency).getDefaultFractionDigits();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La moneda indicada no es un código ISO-4217 válido");
        }
        if (fractionDigits < 0) {
            throw new IllegalArgumentException("La moneda no admite importes fraccionarios");
        }
        try {
            return amount.movePointRight(fractionDigits).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El importe tiene más decimales de los permitidos por la moneda");
        }
    }
}
