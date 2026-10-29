package com.lsoftware.inventoco.infrastructure.supplier.dto;

import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase.SupplierCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** country defaults to the user's. */
public record SupplierRequest(@NotBlank @Size(max = 160) String name, String country, @Size(max = 120) String city,
                              @Size(max = 500) String url) {

    public SupplierCommand command() {
        return new SupplierCommand(name, country, city, url);
    }
}
