package com.lsoftware.inventoco.infrastructure.item.dto;

import com.lsoftware.inventoco.usecase.item.ManageItemUseCase.ItemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** minStock and initialStock are in unit; initialStock only applies on creation. */
public record ItemRequest(@NotBlank @Size(max = 160) String name, @Size(max = 80) String category,
                          @NotBlank String unit, BigDecimal minStock, BigDecimal initialStock,
                          @Size(max = 32) String barcode, UUID catalogProductId) {

    public ItemCommand command() {
        return new ItemCommand(name, category, unit, minStock, initialStock, barcode, catalogProductId);
    }
}
