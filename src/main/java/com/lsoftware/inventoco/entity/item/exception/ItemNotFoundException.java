package com.lsoftware.inventoco.entity.item.exception;

import com.lsoftware.inventoco.entity.exception.NotFoundException;

public class ItemNotFoundException extends NotFoundException {
    public ItemNotFoundException() {
        super("ITEM_NOT_FOUND", "Item not found");
    }
}
