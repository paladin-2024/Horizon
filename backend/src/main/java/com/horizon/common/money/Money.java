package com.horizon.common.money;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * An amount in minor units (for example cents) of one ISO 4217 currency. Entities embed it and
 * rename the columns with {@code @AttributeOverride}.
 */
@Embeddable
public record Money(
        @Column(name = "amount_minor") long amountMinor,
        @Column(name = "currency", length = 3) String currency) {

    public Money {
        currencyOf(currency);
    }

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add " + other.currency + " to " + currency);
        }
        return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(amountMinor), currency);
    }

    @JsonIgnore
    public boolean isNegative() {
        return amountMinor < 0;
    }

    /**
     * Converts a major-unit decimal (for example 12.34 USD) to minor units using the currency's
     * default fraction digits (UGX 0, CDF 2, USD 2). Rejects values with more decimals than the
     * currency allows instead of rounding them.
     */
    public static long toMinor(BigDecimal major, String currency) {
        if (major == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        int digits = currencyOf(currency).getDefaultFractionDigits();
        if (digits < 0) {
            throw new IllegalArgumentException("Currency " + currency + " has no minor unit");
        }
        if (major.stripTrailingZeros().scale() > digits) {
            throw new IllegalArgumentException(
                    "Amount " + major.toPlainString() + " has more than " + digits
                            + " decimal places for " + currency);
        }
        try {
            return major.movePointRight(digits).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount " + major.toPlainString() + " is out of range", e);
        }
    }

    private static Currency currencyOf(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code is required");
        }
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown currency code: " + code, e);
        }
    }
}
