package com.lsoftware.inventoco.entity.purchase.model;

/** Why an item is on the shopping list. */
public enum LineReason {
    /** At or below its minimum stock. */
    LOW_STOCK,
    /** Above the minimum but forecast to run out within the horizon. */
    RUNNING_OUT
}
