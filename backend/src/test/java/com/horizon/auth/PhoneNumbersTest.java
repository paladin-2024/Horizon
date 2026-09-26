package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.error.ApiException;
import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void normalizesUgandanNumbers() {
        assertThat(PhoneNumbers.normalize("+256772123456", "UG")).isEqualTo("+256772123456");
        assertThat(PhoneNumbers.normalize("0772123456", "UG")).isEqualTo("+256772123456");
        assertThat(PhoneNumbers.normalize(" +256 772 123 456 ", "UG")).isEqualTo("+256772123456");
    }

    @Test
    void normalizesCongoleseNumbers() {
        assertThat(PhoneNumbers.normalize("+243812345678", "CD")).isEqualTo("+243812345678");
        assertThat(PhoneNumbers.normalize("0812345678", "CD")).isEqualTo("+243812345678");
        assertThat(PhoneNumbers.normalize("+243991234567", "CD")).isEqualTo("+243991234567");
    }

    @Test
    void acceptsAnySupportedCountryWhenTheNumberIsInternational() {
        assertThat(PhoneNumbers.normalize("+243812345678", "UG")).isEqualTo("+243812345678");
        assertThat(PhoneNumbers.normalizeAny("+256772123456")).isEqualTo("+256772123456");
        assertThat(PhoneNumbers.normalizeAny("0812345678")).isEqualTo("+243812345678");
        assertThat(PhoneNumbers.normalizeAny("0772123456")).isEqualTo("+256772123456");
    }

    @Test
    void rejectsUnsupportedCountriesAndNonMobileAndGarbage() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("+254712345678", "UG"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Uganda");
        assertThatThrownBy(() -> PhoneNumbers.normalize("+256392123456", "UG")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("+256772", "UG")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("+25677212345678", "UG")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("not-a-number", "UG")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize(null, "UG")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalizeAny("+254712345678")).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsAnUnknownDefaultRegion() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("0772123456", "KE")).isInstanceOf(ApiException.class);
    }
}
