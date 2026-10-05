package com.horizon.account;

/** Lifecycle of a linked account. Archived accounts stay readable but stop counting towards totals later. */
enum LinkedAccountStatus {
    ACTIVE,
    ARCHIVED
}
