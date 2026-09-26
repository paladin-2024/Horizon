package com.horizon.auth;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the SMS gateway in tests. The database is never mocked; this stands in for the one
 * collaborator that does not exist yet (the spec chooses a real gateway before go-live).
 */
@TestConfiguration
public class SmsTestConfig {

    @Bean
    @Primary
    RecordingSmsSender recordingSmsSender() {
        return new RecordingSmsSender();
    }

    public static class RecordingSmsSender implements SmsSender {

        private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

        private final List<Sms> sent = new CopyOnWriteArrayList<>();

        public record Sms(String phone, String message) {
        }

        @Override
        public void send(String phoneE164, String message) {
            sent.add(new Sms(phoneE164, message));
        }

        public List<Sms> sent() {
            return List.copyOf(sent);
        }

        public int countFor(String phone) {
            return (int) sent.stream().filter(sms -> sms.phone().equals(phone)).count();
        }

        public String lastCodeFor(String phone) {
            return lastMessageFor(phone)
                    .map(message -> {
                        Matcher matcher = CODE.matcher(message);
                        if (!matcher.find()) {
                            throw new IllegalStateException("No 6-digit code in: " + message);
                        }
                        return matcher.group(1);
                    })
                    .orElseThrow(() -> new IllegalStateException("No SMS was sent to " + phone));
        }

        public Optional<String> lastMessageFor(String phone) {
            return sent.stream()
                    .filter(sms -> sms.phone().equals(phone))
                    .reduce((first, second) -> second)
                    .map(Sms::message);
        }

        public void clear() {
            sent.clear();
        }
    }
}
