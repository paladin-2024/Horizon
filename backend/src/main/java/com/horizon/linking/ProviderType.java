package com.horizon.linking;

/** How an account's data reaches Horizon. New integrations add a constant and a {@link BankProvider} bean. */
public enum ProviderType {
    MANUAL,
    CSV
}
