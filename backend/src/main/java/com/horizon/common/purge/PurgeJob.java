package com.horizon.common.purge;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes expired data every ten minutes. ShedLock makes sure only one instance runs it; batches
 * keep each delete small enough not to hold long locks, and autovacuum reclaims the dead rows.
 */
@Component
public class PurgeJob {

    /** Safety stop: at most 50 000 rows per purger per run with the default batch size. */
    static final int MAX_BATCHES_PER_PURGER = 100;

    private static final Logger log = LoggerFactory.getLogger(PurgeJob.class);

    private final ObjectProvider<DataPurger> purgers;
    private final PurgeProperties properties;

    public PurgeJob(ObjectProvider<DataPurger> purgers, PurgeProperties properties) {
        this.purgers = purgers;
        this.properties = properties;
    }

    @Scheduled(cron = "${horizon.purge.cron}")
    @SchedulerLock(name = "purgeJob", lockAtMostFor = "PT9M", lockAtLeastFor = "PT0S")
    public void run() {
        int batchSize = properties.batchSize();
        for (DataPurger purger : purgers.orderedStream().toList()) {
            int deletedInTotal = 0;
            for (int batch = 0; batch < MAX_BATCHES_PER_PURGER; batch++) {
                int deleted = purger.purgeBatch(batchSize);
                deletedInTotal += deleted;
                if (deleted < batchSize) {
                    break;
                }
            }
            if (deletedInTotal > 0) {
                log.info("Purged {} rows for {}", deletedInTotal, purger.name());
            }
        }
    }
}
