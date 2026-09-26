package com.horizon.user;

import java.util.Locale;

/** The interface languages Horizon supports. */
public enum Language {
    EN,
    FR;

    /** The lowercase code used in the API ("en", "fr"). */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    static Language fromCode(String code) {
        return Language.valueOf(code.toUpperCase(Locale.ROOT));
    }
}
