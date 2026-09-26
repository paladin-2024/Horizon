package com.horizon.common.purge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("horizon.purge")
public record PurgeProperties(Integer batchSize, String cron) {

    public PurgeProperties {
        if (batchSize == null || batchSize <= 0) {
            batchSize = 500;
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 */10 * * * *";
        }
    }
}
