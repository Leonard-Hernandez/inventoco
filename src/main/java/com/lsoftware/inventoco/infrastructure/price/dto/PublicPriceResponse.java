package com.lsoftware.inventoco.infrastructure.price.dto;

import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.shared.Quantities;
import com.lsoftware.inventoco.usecase.price.PublicPrice;

import java.math.BigDecimal;
import java.time.Instant;

/** A price shared by someone, without anything that identifies them. */
public record PublicPriceResponse(String supplierName, String city, String country, BigDecimal amount,
                                  String currency, BigDecimal packageQuantity, String packageUnit,
                                  BigDecimal unitPrice, String unitPriceUnit, Instant observedAt,
                                  boolean storeListing) {

    public static PublicPriceResponse from(PublicPrice p, Unit unit) {
        return new PublicPriceResponse(p.supplierName(), p.city(), p.country(), p.amount(), p.currency(),
                p.packageQuantity(), p.packageUnit(), Quantities.pricePer(unit, p.unitPrice()), unit.symbol(),
                p.observedAt(), p.storeListing());
    }
}
