package com.horizon.auth;

/**
 * Sends one SMS. The real gateway must cover Uganda and DR Congo and is chosen, with its coverage
 * verified, before auth goes live; until then {@link LoggingSmsSender} writes the message to the log.
 */
public interface SmsSender {

    void send(String phoneE164, String message);
}
