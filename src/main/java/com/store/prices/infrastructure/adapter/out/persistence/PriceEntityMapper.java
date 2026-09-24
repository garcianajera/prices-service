package com.store.prices.infrastructure.adapter.out.persistence;

import com.store.prices.domain.model.Price;
import java.util.Currency;
import org.springframework.stereotype.Component;

/** Maps {@link PriceEntity} rows to the domain {@link Price}. */
@Component
public class PriceEntityMapper {

    public Price toDomain(PriceEntity entity) {
        return new Price(
                entity.getBrandId(),
                entity.getProductId(),
                entity.getPriceList(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPriority(),
                entity.getPrice(),
                Currency.getInstance(entity.getCurrency()));
    }
}
