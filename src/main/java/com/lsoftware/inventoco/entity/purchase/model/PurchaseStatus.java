package com.lsoftware.inventoco.entity.purchase.model;

public enum PurchaseStatus {
    /** The open shopping list (one per user). */
    DRAFT,
    /** Bought: its lines became stock movements and prices. */
    COMPLETED
}
