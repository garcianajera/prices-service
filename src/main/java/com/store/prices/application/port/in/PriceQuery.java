package com.store.prices.application.port.in;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A request for the price that applies to a product of a brand at a date.
 *
 * @param applicationDate date at which the price must apply
 * @param productId       product to price
 * @param brandId         brand the product belongs to
 */
public record PriceQuery(LocalDateTime applicationDate, long productId, long brandId) {

    public PriceQuery {
        Objects.requireNonNull(applicationDate, "applicationDate must not be null");
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (brandId <= 0) {
            throw new IllegalArgumentException("brandId must be positive");
        }
    }
}
