package com.horizon.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Local development SMS gateway: logs the message, including the code, instead of sending it. */
@Component
class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phoneE164, String message) {
        log.info("SMS to {}: {}", phoneE164, message);
    }
}
