package com.horizon.common.purge;

/**
 * One kind of expired data. Implementations delete at most {@code batchSize} rows per call and
 * return how many they deleted; the job keeps calling until a batch comes back short.
 */
public interface DataPurger {

    String name();

    int purgeBatch(int batchSize);
}
