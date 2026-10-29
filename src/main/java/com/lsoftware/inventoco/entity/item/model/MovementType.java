package com.lsoftware.inventoco.entity.item.model;

/** Why the stock of an item changed. */
public enum MovementType {
    /** Bought: adds stock. */
    PURCHASE,
    /** Used: removes stock and feeds the depletion forecast. */
    CONSUMPTION,
    /** Thrown away (expired, broken): removes stock and also counts as outflow for the forecast. */
    WASTE,
    /** Counted: the quantity given is the new stock, the movement stores the difference. */
    ADJUSTMENT;

    public boolean outflow() {
        return this == CONSUMPTION || this == WASTE;
    }
}
