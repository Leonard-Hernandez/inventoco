package com.lsoftware.inventoco.entity.item.exception;

import com.lsoftware.inventoco.entity.exception.ConflictException;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String stock) {
        super("INSUFFICIENT_STOCK", "Only " + stock + " in stock. Record the missing purchase or adjust the count first");
    }
}
