package com.lsoftware.inventoco.infrastructure.config;

import com.lsoftware.inventoco.infrastructure.config.db.schema.ItemEntity;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockColumnTest {

    /**
     * Saving an edited item must not write back the stock it read: a movement committed in between
     * would be lost. Only the atomic addStock statement changes the column.
     */
    @Test
    void theStockIsNeverPartOfAnEntityUpdate() throws NoSuchFieldException {
        Column column = ItemEntity.class.getDeclaredField("currentStock").getAnnotation(Column.class);

        assertThat(column.updatable()).isFalse();
        assertThat(column.insertable()).isTrue();
    }
}
