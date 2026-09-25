package com.horizon.common.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Time-ordered UUID version 7 (RFC 9562): 48-bit Unix millisecond timestamp, version, a 12-bit
 * counter that keeps ids generated in the same millisecond strictly increasing, variant, and
 * 62 random bits.
 */
public final class Uuid7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Object LOCK = new Object();
    private static long lastMillis = -1;
    private static int sequence;

    private Uuid7() {
    }

    public static UUID next() {
        long millis;
        int seq;
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (now > lastMillis) {
                lastMillis = now;
                sequence = RANDOM.nextInt(1 << 11);
            } else {
                sequence++;
                if (sequence > 0xFFF) {
                    lastMillis++;
                    sequence = RANDOM.nextInt(1 << 11);
                }
            }
            millis = lastMillis;
            seq = sequence;
        }
        long msb = (millis << 16) | (0x7L << 12) | seq;
        long lsb = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }
}
