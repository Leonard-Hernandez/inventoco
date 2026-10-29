package com.lsoftware.inventoco.entity.unit.model;

/** What an item is measured in. Quantities are stored in the base unit of their dimension. */
public enum Dimension {
    COUNT, MASS, VOLUME;

    public Unit baseUnit() {
        return switch (this) {
            case COUNT -> Unit.UNIT;
            case MASS -> Unit.G;
            case VOLUME -> Unit.ML;
        };
    }
}
