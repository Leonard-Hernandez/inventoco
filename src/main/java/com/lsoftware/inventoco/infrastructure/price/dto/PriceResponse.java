package com.lsoftware.inventoco.infrastructure.price.dto;

import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.shared.Quantities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** unitPrice is per unitPriceUnit (the item's unit), so packages of any size compare directly. */
public record PriceResponse(UUID id, UUID supplierId, String supplierName, BigDecimal amount, String currency,
                            BigDecimal packageQuantity, String packageUnit, BigDecimal unitPrice,
                            String unitPriceUnit, Instant observedAt, String visibility, UUID purchaseId) {

    public static PriceResponse from(PriceObservation p, String supplierName, Unit itemUnit) {
        return new PriceResponse(p.id(), p.supplierId(), supplierName, p.amount(), p.currency(),
                p.packageUnit().fromBase(p.packageQuantity()), p.packageUnit().symbol(),
                Quantities.pricePer(itemUnit, p.unitPrice()), itemUnit.symbol(), p.observedAt(), p.visibility().name(),
                p.purchaseId());
    }
}
