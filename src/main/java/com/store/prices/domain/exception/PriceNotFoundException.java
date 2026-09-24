package com.store.prices.domain.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** No price applies for the brand, product and application date. */
public class PriceNotFoundException extends RuntimeException {

    // LocalDateTime.toString() drops zero seconds ("10:00"); always show them, as the API does.
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final long brandId;
    private final long productId;
    private final LocalDateTime applicationDate;

    public PriceNotFoundException(long brandId, long productId, LocalDateTime applicationDate) {
        super("No applicable price for brand %d, product %d at %s".formatted(brandId, productId,
                DATE_FORMAT.format(applicationDate)));
        this.brandId = brandId;
        this.productId = productId;
        this.applicationDate = applicationDate;
    }

    public long brandId() {
        return brandId;
    }

    public long productId() {
        return productId;
    }

    public LocalDateTime applicationDate() {
        return applicationDate;
    }
}
