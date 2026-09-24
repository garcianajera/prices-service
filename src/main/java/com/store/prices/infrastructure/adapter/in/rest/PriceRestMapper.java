package com.store.prices.infrastructure.adapter.in.rest;

import com.store.prices.domain.model.Price;
import org.springframework.stereotype.Component;

/** Maps the domain {@link Price} to the REST {@link PriceResponse}. */
@Component
public class PriceRestMapper {

    public PriceResponse toResponse(Price price) {
        return new PriceResponse(
                price.productId(),
                price.brandId(),
                price.priceList(),
                price.startDate(),
                price.endDate(),
                price.amount(),
                price.currency().getCurrencyCode());
    }
}
