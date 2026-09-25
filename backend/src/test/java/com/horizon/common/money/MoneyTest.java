package com.horizon.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MoneyTest {

    @Test
    void ofKeepsAmountAndCurrency() {
        Money money = Money.of(12_345, "UGX");

        assertThat(money.amountMinor()).isEqualTo(12_345);
        assertThat(money.currency()).isEqualTo("UGX");
    }

    @Test
    void ofRejectsUnknownCurrencyCodes() {
        assertThatThrownBy(() -> Money.of(1, "ZZZ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(1, "ugx")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(1, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(1, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plusAddsSameCurrency() {
        assertThat(Money.of(500, "USD").plus(Money.of(250, "USD"))).isEqualTo(Money.of(750, "USD"));
        assertThat(Money.of(500, "USD").plus(Money.of(-800, "USD"))).isEqualTo(Money.of(-300, "USD"));
    }

    @Test
    void plusRejectsDifferentCurrencies() {
        assertThatThrownBy(() -> Money.of(500, "USD").plus(Money.of(500, "CDF")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CDF")
                .hasMessageContaining("USD");
    }

    @Test
    void plusFailsLoudlyOnOverflow() {
        assertThatThrownBy(() -> Money.of(Long.MAX_VALUE, "USD").plus(Money.of(1, "USD")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void negateFlipsTheSign() {
        assertThat(Money.of(500, "UGX").negate()).isEqualTo(Money.of(-500, "UGX"));
        assertThat(Money.of(-500, "UGX").negate()).isEqualTo(Money.of(500, "UGX"));
        assertThat(Money.of(0, "UGX").negate()).isEqualTo(Money.of(0, "UGX"));
    }

    @Test
    void isNegativeIsTrueOnlyBelowZero() {
        assertThat(Money.of(-1, "UGX").isNegative()).isTrue();
        assertThat(Money.of(0, "UGX").isNegative()).isFalse();
        assertThat(Money.of(1, "UGX").isNegative()).isFalse();
    }

    @Test
    void toMinorUsesTheCurrencyFractionDigits() {
        assertThat(Money.toMinor(new BigDecimal("5000"), "UGX")).isEqualTo(5_000);
        assertThat(Money.toMinor(new BigDecimal("12.34"), "CDF")).isEqualTo(1_234);
        assertThat(Money.toMinor(new BigDecimal("12.34"), "USD")).isEqualTo(1_234);
        assertThat(Money.toMinor(new BigDecimal("0.1"), "USD")).isEqualTo(10);
        assertThat(Money.toMinor(new BigDecimal("-12.34"), "USD")).isEqualTo(-1_234);
        assertThat(Money.toMinor(new BigDecimal("1E+3"), "UGX")).isEqualTo(1_000);
    }

    @Test
    void toMinorAcceptsTrailingZerosBeyondTheAllowedDecimals() {
        assertThat(Money.toMinor(new BigDecimal("5000.00"), "UGX")).isEqualTo(5_000);
        assertThat(Money.toMinor(new BigDecimal("12.3400"), "USD")).isEqualTo(1_234);
    }

    @Test
    void toMinorRejectsTooManyDecimals() {
        assertThatThrownBy(() -> Money.toMinor(new BigDecimal("5000.5"), "UGX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toMinor(new BigDecimal("12.345"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toMinor(new BigDecimal("12.345"), "CDF"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toMinorRejectsBadInput() {
        assertThatThrownBy(() -> Money.toMinor(null, "USD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toMinor(BigDecimal.ONE, "ZZZ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toMinor(BigDecimal.ONE, "XXX")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toMinor(new BigDecimal("92233720368547758.08"), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of range");
    }

    @Test
    void serializesToTheApiShape() {
        String json = JsonMapper.builder().build().writeValueAsString(Money.of(12_345, "UGX"));

        assertThat(json).isEqualTo("{\"amountMinor\":12345,\"currency\":\"UGX\"}");
    }
}
