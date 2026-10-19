package com.horizon.linking;

/** What a provider can do, so callers never have to switch on {@link ProviderType}. */
public enum ProviderCapability {
    LINK,
    BALANCE_SYNC,
    TRANSACTION_SYNC,
    STATEMENT_IMPORT
}
