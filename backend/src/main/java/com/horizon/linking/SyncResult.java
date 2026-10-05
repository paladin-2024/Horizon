package com.horizon.linking;

/** The outcome of {@link BankProvider#sync}. Providers that cannot sync return {@link #notPerformed()}. */
public record SyncResult(boolean performed, int transactionsFetched) {

    public static SyncResult notPerformed() {
        return new SyncResult(false, 0);
    }

    public static SyncResult performed(int transactionsFetched) {
        return new SyncResult(true, transactionsFetched);
    }
}
