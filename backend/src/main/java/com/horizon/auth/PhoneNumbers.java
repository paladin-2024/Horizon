package com.horizon.auth;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.horizon.common.error.ApiException;
import java.util.List;
import java.util.Optional;

/** E.164 normalization limited to the countries Horizon serves: Uganda (+256) and DR Congo (+243). */
public final class PhoneNumbers {

    public static final List<String> SUPPORTED_REGIONS = List.of("UG", "CD");

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();
    private static final String MESSAGE =
            "Enter a mobile number from Uganda (+256) or DR Congo (+243).";

    private PhoneNumbers() {
    }

    /** Normalizes {@code raw}, reading local formats such as 0772123456 with {@code country} as the default region. */
    public static String normalize(String raw, String country) {
        if (raw == null || raw.isBlank() || country == null || !SUPPORTED_REGIONS.contains(country)) {
            throw invalid();
        }
        return tryNormalize(raw, country).orElseThrow(PhoneNumbers::invalid);
    }

    /** Normalizes {@code raw} without a country hint, trying each supported region in turn. */
    public static String normalizeAny(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        for (String region : SUPPORTED_REGIONS) {
            Optional<String> normalized = tryNormalize(raw, region);
            if (normalized.isPresent()) {
                return normalized.get();
            }
        }
        throw invalid();
    }

    private static Optional<String> tryNormalize(String raw, String defaultRegion) {
        try {
            PhoneNumber number = UTIL.parse(raw.trim(), defaultRegion);
            if (!UTIL.isValidNumber(number)) {
                return Optional.empty();
            }
            String region = UTIL.getRegionCodeForNumber(number);
            if (region == null || !SUPPORTED_REGIONS.contains(region)) {
                return Optional.empty();
            }
            PhoneNumberType type = UTIL.getNumberType(number);
            if (type != PhoneNumberType.MOBILE && type != PhoneNumberType.FIXED_LINE_OR_MOBILE) {
                return Optional.empty();
            }
            return Optional.of(UTIL.format(number, PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    private static ApiException invalid() {
        return ApiException.badRequest("invalid_phone", MESSAGE);
    }
}
