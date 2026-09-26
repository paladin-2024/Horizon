package com.horizon.auth;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Unique, genuinely valid mobile numbers so tests never collide on the unique phone constraint.
 * Verified with libphonenumber 9.0.39: every +25677XXXXXXX and +24381XXXXXXX is a valid MOBILE
 * number for UG and CD respectively.
 */
final class TestPhones {

    private TestPhones() {
    }

    static String nextUgandan() {
        return "+25677" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    static String nextCongolese() {
        return "+24381" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }
}
